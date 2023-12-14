/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2019
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.citygml.importer.database.content;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.building.BuildingFurniture;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;

public class DBBuildingFurniture extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private GeometryConverter geometryConverter;
	private DBImplicitGeometry implicitGeometryImporter;
	private AttributeValueJoiner valueJoiner;	
	private int batchCounter;
	
	private boolean affineTransformation;
	private boolean hasObjectClassIdColumn;

	public DBBuildingFurniture(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		implicitGeometryImporter = importer.getImporter(DBImplicitGeometry.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}
	
	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}

	@Override
	protected String getTableName() {
		return TableEnum.BUILDING_FURNITURE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "buildingfurniture/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".building_furniture (id, class, class_codespace, function, function_codespace, usage, usage_codespace, room_id, " +
				"lod4_brep_id, lod4_other_geom, " +
				"lod4_implicit_rep_id, lod4_implicit_ref_point, lod4_implicit_transformation" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + "> " +  // add BASE by SYL
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
				"{ ? " + SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_CLASS + param +
				SchemaManagerAdapter.ONTO_CLASS_CODESPACE + param +
				SchemaManagerAdapter.ONTO_FUNCTION + param +
				SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE + param +
				SchemaManagerAdapter.ONTO_USAGE + param +
				SchemaManagerAdapter.ONTO_USAGE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_ROOM_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_TRANSFORMATION + param +
				(hasObjectClassIdColumn ? SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param : "") +
				".}" +
				"}";

		return stmt;
	}

	protected long doImport(BuildingFurniture buildingFurniture) throws CityGMLImportException, SQLException {
		return doImport(buildingFurniture, 0);
	}

	protected long doImport(BuildingFurniture buildingFurniture, long roomId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(buildingFurniture);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long buildingFurnitureId = cityObjectImporter.doImport(buildingFurniture, featureType);

		URL objectURL = null;
		int index = 0;
		// import building furniture information
		if (importer.isBlazegraph()) {
			try {
				String uuid = buildingFurniture.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(iriGraphObject + uuid + "/");
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
			}
			preparedStatement.setURL(++index, objectURL);
			// primary id
			preparedStatement.setURL(++index, objectURL);
			buildingFurniture.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			// primary id
			preparedStatement.setLong(++index, buildingFurnitureId);
		}

		// bldg:class
		if (buildingFurniture.isSetClazz() && buildingFurniture.getClazz().isSetValue()) {
			preparedStatement.setString(++index, buildingFurniture.getClazz().getValue());
			preparedStatement.setString(++index, buildingFurniture.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// bldg:function
		if (buildingFurniture.isSetFunction()) {
			valueJoiner.join(buildingFurniture.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// bldg:usage
		if (buildingFurniture.isSetUsage()) {
			valueJoiner.join(buildingFurniture.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// parent room id
		if (roomId != 0)
			preparedStatement.setLong(++index, roomId);
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(8, Types.NULL);

		// bldg:lod4Geometry
		long geometryId = 0;
		GeometryObject geometryObject = null;

		if (buildingFurniture.isSetLod4Geometry()) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = buildingFurniture.getLod4Geometry();

			if (geometryProperty.isSetGeometry()) {
				AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
				if (importer.isSurfaceGeometry(abstractGeometry))
					geometryId = surfaceGeometryImporter.doImport(abstractGeometry, buildingFurnitureId);
				else if (importer.isPointOrLineGeometry(abstractGeometry))
					geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
				else
					importer.logOrThrowUnsupportedGeometryMessage(buildingFurniture, abstractGeometry);

				geometryProperty.unsetGeometry();
			} else {
				String href = geometryProperty.getHref();
				if (href != null && href.length() != 0) {
					importer.propagateXlink(new DBXlinkSurfaceGeometry(
							TableEnum.BUILDING_FURNITURE.getName(),
							buildingFurnitureId,
							href,
							"lod4_brep_id"));
				}
			}
		}

		if (geometryId != 0)
			preparedStatement.setLong(++index, geometryId);
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(9, Types.NULL);

		if (geometryObject != null)
			preparedStatement.setObject(++index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(10, importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
					importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());

		// bldg:lod4ImplicitRepresentation
		GeometryObject pointGeom = null;
		String matrixString = null;
		long implicitId = 0;

		if (buildingFurniture.isSetLod4ImplicitRepresentation()) {
			ImplicitRepresentationProperty implicit = buildingFurniture.getLod4ImplicitRepresentation();

			if (implicit.isSetObject()) {
				ImplicitGeometry geometry = implicit.getObject();

				// reference Point
				if (geometry.isSetReferencePoint())
					pointGeom = geometryConverter.getPoint(geometry.getReferencePoint());

				// transformation matrix
				if (geometry.isSetTransformationMatrix()) {
					Matrix matrix = geometry.getTransformationMatrix().getMatrix();
					if (affineTransformation)
						matrix = importer.getAffineTransformer().transformImplicitGeometryTransformationMatrix(matrix);

					matrixString = valueJoiner.join(" ", matrix.toRowPackedList());
				}

				// reference to IMPLICIT_GEOMETRY
				implicitId = implicitGeometryImporter.doImport(geometry);
			}
		}

		if (implicitId != 0)
			preparedStatement.setLong(++index, implicitId);
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(11, Types.NULL);

		if (pointGeom != null)
			preparedStatement.setObject(++index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(12, importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
					importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());

		if (matrixString != null)
			preparedStatement.setString(++index, matrixString);
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(13, Types.VARCHAR);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(++index, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BUILDING_FURNITURE);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(buildingFurniture, buildingFurnitureId, featureType);

		return buildingFurnitureId;
	}

}
