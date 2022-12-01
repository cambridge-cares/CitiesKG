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
import org.citygml4j.model.citygml.cityfurniture.CityFurniture;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;

public class DBCityFurniture extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBImplicitGeometry implicitGeometryImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;
	private boolean affineTransformation;

	public DBCityFurniture(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
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
		return TableEnum.CITY_FURNITURE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "cityfurniture/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".city_furniture (id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"lod1_terrain_intersection, lod2_terrain_intersection, lod3_terrain_intersection, lod4_terrain_intersection, " +
				"lod1_brep_id, lod2_brep_id, lod3_brep_id, lod4_brep_id, " +
				"lod1_other_geom, lod2_other_geom, lod3_other_geom, lod4_other_geom, " +
				"lod1_implicit_rep_id, lod2_implicit_rep_id, lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod1_implicit_ref_point, lod2_implicit_ref_point, lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod1_implicit_transformation, lod2_implicit_transformation, lod3_implicit_transformation, lod4_implicit_transformation" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + "> " +
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
				"{ ? " + SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_CLASS + param +
				SchemaManagerAdapter.ONTO_CLASS_CODESPACE + param +
				SchemaManagerAdapter.ONTO_FUNCTION + param +
				SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE + param +
				SchemaManagerAdapter.ONTO_USAGE + param +
				SchemaManagerAdapter.ONTO_USAGE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_LOD1_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD2_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD3_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD4_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD1_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD1_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD2_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD3_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD4_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD1_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD1_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD1_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_TRANSFORMATION + param +
				(hasObjectClassIdColumn ? SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param : "") +
				".}" +
				"}";
		return stmt;
	}

	protected long doImport(CityFurniture cityFurniture) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(cityFurniture);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long cityFurnitureId = cityObjectImporter.doImport(cityFurniture, featureType);
		int index = 0;
		URL objectURL = null;

		// import city furniture information
		if (importer.isBlazegraph()) {
			try {
				String uuid = cityFurniture.getId();
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
			cityFurniture.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			preparedStatement.setLong(++index, cityFurnitureId);
		}

		// frn:class
		if (cityFurniture.isSetClazz() && cityFurniture.getClazz().isSetValue()) {
			String code =  cityFurniture.getClazz().getValue();
			String codespace = cityFurniture.getClazz().getCodeSpace(); //valueJoiner.result(1);

			if (importer.isBlazegraph() && code == null){
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, code);
			}

			if (importer.isBlazegraph() &&  codespace == null) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// frn:function
		if (cityFurniture.isSetFunction()) {
			valueJoiner.join(cityFurniture.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			String codespace = valueJoiner.result(1);
			if (importer.isBlazegraph() &&  codespace == null) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// frn:usage
		if (cityFurniture.isSetUsage()) {
			valueJoiner.join(cityFurniture.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			String codespace = valueJoiner.result(1);
			if (importer.isBlazegraph() &&  codespace == null) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}



		// bldg:lodXTerrainIntersectionCurve
		index = importGeometryObjectProperties(new MultiCurveProperty[]{
				cityFurniture.getLod1TerrainIntersection(),
				cityFurniture.getLod2TerrainIntersection(),
				cityFurniture.getLod3TerrainIntersection(),
				cityFurniture.getLod4TerrainIntersection()
		}, geometryConverter::getMultiCurve, index);

		int BrepId_index = 0;
		int Geom_index = 0;

		// frn:lodXGeometry
		for (int i = 0; i < 4; i++) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = null;
			long geometryId = 0;
			GeometryObject geometryObject = null;

			switch (i) {
			case 0:
				geometryProperty = cityFurniture.getLod1Geometry();
				break;
			case 1:
				geometryProperty = cityFurniture.getLod2Geometry();
				break;
			case 2:
				geometryProperty = cityFurniture.getLod3Geometry();
				break;
			case 3:
				geometryProperty = cityFurniture.getLod4Geometry();
				break;
			}

			if (geometryProperty != null) {
				if (geometryProperty.isSetGeometry()) {
					AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
					if (importer.isSurfaceGeometry(abstractGeometry))
						geometryId = surfaceGeometryImporter.doImport(abstractGeometry, cityFurnitureId);
					else if (importer.isPointOrLineGeometry(abstractGeometry))
						geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
					else
						importer.logOrThrowUnsupportedGeometryMessage(cityFurniture, abstractGeometry);

					geometryProperty.unsetGeometry();
				} else {
					String href = geometryProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.CITY_FURNITURE.getName(),
								cityFurnitureId, 
								href, 
								"lod" + i + "_brep_id"));
					}
				}
			}

			BrepId_index = ++index;

			if (geometryId != 0)
				preparedStatement.setLong(BrepId_index, geometryId);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, BrepId_index);
			else
				preparedStatement.setNull(BrepId_index, Types.NULL);

			Geom_index = BrepId_index + 4;

			if (geometryObject != null)
				preparedStatement.setObject(Geom_index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, Geom_index);
			else
				preparedStatement.setNull(Geom_index, nullGeometryType, nullGeometryTypeName);
		}

		index = Geom_index;

		int implicitId_index = 0;
		int pointGeom_index = 0;
		int matrixString_index = 0;

		// frn:lodXImplicitRepresentation
		for (int i = 0; i < 4; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = cityFurniture.getLod1ImplicitRepresentation();
				break;
			case 1:
				implicit = cityFurniture.getLod2ImplicitRepresentation();
				break;
			case 2:
				implicit = cityFurniture.getLod3ImplicitRepresentation();
				break;
			case 3:
				implicit = cityFurniture.getLod4ImplicitRepresentation();
				break;
			}

			if (implicit != null) {
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

			implicitId_index = ++index;
			pointGeom_index = implicitId_index + 4;
			matrixString_index = pointGeom_index + 4;

			if (implicitId != 0)
				preparedStatement.setLong(implicitId_index, implicitId);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, implicitId_index);
			else
				preparedStatement.setNull(implicitId_index, Types.NULL);

			if (pointGeom != null)
				preparedStatement.setObject(pointGeom_index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, pointGeom_index);
			else
				preparedStatement.setNull(pointGeom_index, nullGeometryType, nullGeometryTypeName);

			if (matrixString != null)
				preparedStatement.setString(matrixString_index, matrixString);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, matrixString_index);
			else
				preparedStatement.setNull(matrixString_index, Types.VARCHAR);
		}

		index = matrixString_index;

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(++index, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.CITY_FURNITURE);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(cityFurniture, cityFurnitureId, featureType);

		return cityFurnitureId;
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			preparedStatement.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		preparedStatement.close();
	}

}
