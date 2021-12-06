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

import org.apache.jena.graph.NodeFactory;
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
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.citygml.generics.GenericCityObject;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class DBGenericCityObject extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBImplicitGeometry implicitGeometryImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;
	private boolean affineTransformation;

	public DBGenericCityObject(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
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
		return TableEnum.GENERIC_CITYOBJECT.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "genericcityobject/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".generic_cityobject (id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"lod0_terrain_intersection, lod1_terrain_intersection, lod2_terrain_intersection, lod3_terrain_intersection, lod4_terrain_intersection, " +
				"lod0_brep_id, lod1_brep_id, lod2_brep_id, lod3_brep_id, lod4_brep_id, " +
				"lod0_other_geom, lod1_other_geom, lod2_other_geom, lod3_other_geom, lod4_other_geom, " +
				"lod0_implicit_rep_id, lod1_implicit_rep_id, lod2_implicit_rep_id, lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod0_implicit_ref_point, lod1_implicit_ref_point, lod2_implicit_ref_point, lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod0_implicit_transformation, lod1_implicit_transformation, lod2_implicit_transformation, lod3_implicit_transformation, lod4_implicit_transformation" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_CLASS + param +
				SchemaManagerAdapter.ONTO_CLASS_CODESPACE + param +
				SchemaManagerAdapter.ONTO_FUNCTION + param +
				SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE + param +
				SchemaManagerAdapter.ONTO_USAGE + param +
				SchemaManagerAdapter.ONTO_USAGE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_LOD0_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD1_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD2_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD3_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD4_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD0_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD1_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_BREP_ID + param +
				SchemaManagerAdapter.ONTO_LOD0_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD1_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD2_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD3_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD4_OTHER_GEOM + param +
				SchemaManagerAdapter.ONTO_LOD0_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD1_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REP_ID + param +
				SchemaManagerAdapter.ONTO_LOD0_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD1_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REF_POINT + param +
				SchemaManagerAdapter.ONTO_LOD0_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD1_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD2_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_TRANSFORMATION + param +
				SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_TRANSFORMATION + param +
				(hasObjectClassIdColumn ? SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param : "") +
				".}" +
				"}";

		return stmt;
	}

	protected long doImport(GenericCityObject genericCityObject) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(genericCityObject);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// "http://www.theworldavatar.com:83/citieskg/"

		// import city object information
		long genericCityObjectId = cityObjectImporter.doImport(genericCityObject, featureType);

		int index = 0;
		URL objectURL = null;
		URL rootURL = null;
		URL parentURL = null;

		// import generic city object information
		if (importer.isBlazegraph()) {
			try {
				String uuid = genericCityObject.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(IRI_GRAPH_OBJECT + uuid + "/");
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
			}
			preparedStatement.setURL(++index, objectURL);   // index = 1
			// primary id
			preparedStatement.setURL(++index, objectURL);     //  index = 2
			genericCityObject.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			// primary id, SQL index = 1
			preparedStatement.setLong(++index, genericCityObjectId);
		}

		// gen:class
		if (genericCityObject.isSetClazz() && genericCityObject.getClazz().isSetValue()) {
			preparedStatement.setString(++index, genericCityObject.getClazz().getValue());
			preparedStatement.setString(++index, genericCityObject.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// gen:function
		if (genericCityObject.isSetFunction()) {
			valueJoiner.join(genericCityObject.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// gen:usage
		if (genericCityObject.isSetUsage()) {
			valueJoiner.join(genericCityObject.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));   // index = 6
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:lodXTerrainIntersectionCurve
		index = importGeometryObjectProperties(new MultiCurveProperty[]{
				genericCityObject.getLod0TerrainIntersection(),
				genericCityObject.getLod1TerrainIntersection(),
				genericCityObject.getLod2TerrainIntersection(),
				genericCityObject.getLod3TerrainIntersection(),
				genericCityObject.getLod4TerrainIntersection()
		}, geometryConverter::getMultiCurve, index);

		int BrepId_index = 0;
		int Geom_index = 0;

		// gen:lodXGeometry
		for (int i = 0; i < 5; i++) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = null;
			long geometryId = 0;
			GeometryObject geometryObject = null;

			switch (i) {
			case 0:
				geometryProperty = genericCityObject.getLod0Geometry();
				break;
			case 1:
				geometryProperty = genericCityObject.getLod1Geometry();
				break;
			case 2:
				geometryProperty = genericCityObject.getLod2Geometry();
				break;
			case 3:
				geometryProperty = genericCityObject.getLod3Geometry();
				break;
			case 4:
				geometryProperty = genericCityObject.getLod4Geometry();
				break;
			}

			if (geometryProperty != null) {
				if (geometryProperty.isSetGeometry()) {
					AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
					if (importer.isSurfaceGeometry(abstractGeometry))
						geometryId = surfaceGeometryImporter.doImport(abstractGeometry, genericCityObjectId);
					else if (importer.isPointOrLineGeometry(abstractGeometry))
						geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
					else 
						importer.logOrThrowUnsupportedGeometryMessage(genericCityObject, abstractGeometry);

					geometryProperty.unsetGeometry();
				} else {
					String href = geometryProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.GENERIC_CITYOBJECT.getName(),
								genericCityObjectId, 
								href, 
								"lod" + i + "_brep_id"));
					}
				}
			}

			BrepId_index = ++index;

			if (geometryId != 0)
				preparedStatement.setLong(BrepId_index, geometryId);  // 13 + i
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, BrepId_index);
			else
				preparedStatement.setNull(BrepId_index, Types.NULL);  // 13 + i

			Geom_index = BrepId_index + 5;

			if (geometryObject != null)
				preparedStatement.setObject(Geom_index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, Geom_index);
			else
				preparedStatement.setNull(Geom_index, nullGeometryType, nullGeometryTypeName);
		}

		index = Geom_index;  // @TODO: Check if index = 18 + 4

		int implicitId_index = 0;
		int pointGeom_index = 0;
		int matrixString_index = 0;

		// gen:lodXImplicitRepresentation
		for (int i = 0; i < 5; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = genericCityObject.getLod0ImplicitRepresentation();
				break;
			case 1:
				implicit = genericCityObject.getLod1ImplicitRepresentation();
				break;
			case 2:
				implicit = genericCityObject.getLod2ImplicitRepresentation();
				break;
			case 3:
				implicit = genericCityObject.getLod3ImplicitRepresentation();
				break;
			case 4:
				implicit = genericCityObject.getLod4ImplicitRepresentation();
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
			pointGeom_index = implicitId_index + 5;
			matrixString_index = pointGeom_index + 5;

			if (implicitId != 0)	// 23 + i
				preparedStatement.setLong(implicitId_index, implicitId);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, implicitId_index);
			else
				preparedStatement.setNull(implicitId_index, Types.NULL);

			if (pointGeom != null)	// 28 + i
				preparedStatement.setObject(pointGeom_index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, pointGeom_index);
			else
				preparedStatement.setNull(pointGeom_index, nullGeometryType, nullGeometryTypeName);

			if (matrixString != null)	// 33 + i
				preparedStatement.setString(matrixString_index, matrixString);
			else if (importer.isBlazegraph())
				setBlankNode(preparedStatement, matrixString_index);
			else
				preparedStatement.setNull(matrixString_index, Types.VARCHAR);
		}

		index = matrixString_index;

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(++index, featureType.getObjectClassId());	// 38

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.GENERIC_CITYOBJECT);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(genericCityObject, genericCityObjectId, featureType);

		return genericCityObjectId;
	}

}
