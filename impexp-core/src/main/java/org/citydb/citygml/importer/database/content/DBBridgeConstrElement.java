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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;



import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.bridge.AbstractBoundarySurface;
import org.citygml4j.model.citygml.bridge.AbstractBridge;
import org.citygml4j.model.citygml.bridge.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.bridge.BridgeConstructionElement;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;

public class DBBridgeConstrElement extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBBridgeThematicSurface thematicSurfaceImporter;
	private GeometryConverter geometryConverter;
	private DBImplicitGeometry implicitGeometryImporter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;
	private boolean affineTransformation;

	public DBBridgeConstrElement(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		implicitGeometryImporter = importer.getImporter(DBImplicitGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBBridgeThematicSurface.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}

	@Override
	protected String getTableName() {
		return TableEnum.BRIDGE_CONSTR_ELEMENT.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "bridgeconstrelement/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".bridge_constr_element (id, class, class_codespace, function, function_codespace, usage, usage_codespace, bridge_id, " +
				"lod1_terrain_intersection, lod2_terrain_intersection, lod3_terrain_intersection, lod4_terrain_intersection, " +
				"lod1_brep_id, lod2_brep_id, lod3_brep_id, lod4_brep_id, lod1_other_geom, lod2_other_geom, lod3_other_geom, lod4_other_geom, " +
				"lod1_implicit_rep_id, lod2_implicit_rep_id, lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod1_implicit_ref_point, lod2_implicit_ref_point, lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod1_implicit_transformation, lod2_implicit_transformation, lod3_implicit_transformation, lod4_implicit_transformation" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	protected long doImport(BridgeConstructionElement bridgeConstruction) throws CityGMLImportException, SQLException {
		return doImport(bridgeConstruction, null, 0);
	}

	public long doImport(BridgeConstructionElement bridgeConstruction, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(bridgeConstruction);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long bridgeConstructionId = cityObjectImporter.doImport(bridgeConstruction, featureType);

		// import bridge construction element information
		// primary id
		preparedStatement.setLong(1, bridgeConstructionId);

		// brid:class
		if (bridgeConstruction.isSetClazz() && bridgeConstruction.getClazz().isSetValue()) {
			preparedStatement.setString(2, bridgeConstruction.getClazz().getValue());
			preparedStatement.setString(3, bridgeConstruction.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// brid:function
		if (bridgeConstruction.isSetFunction()) {
			valueJoiner.join(bridgeConstruction.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(4, valueJoiner.result(0));
			preparedStatement.setString(5, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// brid:usage
		if (bridgeConstruction.isSetUsage()) {
			valueJoiner.join(bridgeConstruction.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// parent id
		if (parent instanceof AbstractBridge)
			preparedStatement.setLong(8, parentId);
		else
			preparedStatement.setNull(8, Types.NULL);

		// brid:lodXTerrainIntersectionCurve
		importGeometryObjectProperties(new MultiCurveProperty[]{
				bridgeConstruction.getLod1TerrainIntersection(),
				bridgeConstruction.getLod2TerrainIntersection(),
				bridgeConstruction.getLod3TerrainIntersection(),
				bridgeConstruction.getLod4TerrainIntersection()
		}, geometryConverter::getMultiCurve, 9);

		// brid:lodXGeometry
		for (int i = 0; i < 4; i++) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = null;
			long geometryId = 0;
			GeometryObject geometryObject = null;

			switch (i) {
			case 0:
				geometryProperty = bridgeConstruction.getLod1Geometry();
				break;
			case 1:
				geometryProperty = bridgeConstruction.getLod2Geometry();
				break;
			case 2:
				geometryProperty = bridgeConstruction.getLod3Geometry();
				break;
			case 3:
				geometryProperty = bridgeConstruction.getLod4Geometry();
				break;
			}

			if (geometryProperty != null) {
				if (geometryProperty.isSetGeometry()) {
					AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
					if (importer.isSurfaceGeometry(abstractGeometry))
						geometryId = surfaceGeometryImporter.doImport(abstractGeometry, bridgeConstructionId);
					else if (importer.isPointOrLineGeometry(abstractGeometry))
						geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
					else
						importer.logOrThrowUnsupportedGeometryMessage(bridgeConstruction, abstractGeometry);

					geometryProperty.unsetGeometry();
				} else {
					String href = geometryProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.BRIDGE_CONSTR_ELEMENT.getName(),
								bridgeConstructionId,
								href,
								"lod" + (i + 1) + "_brep_id"));
					}
				}
			}

			if (geometryId != 0)
				preparedStatement.setLong(13 + i, geometryId);
			else
				preparedStatement.setNull(13 + i, Types.NULL);

			if (geometryObject != null)
				preparedStatement.setObject(17 + i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
			else
				preparedStatement.setNull(17 + i, nullGeometryType, nullGeometryTypeName);
		}

		// brid:lodXImplicitRepresentation
		for (int i = 0; i < 4; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = bridgeConstruction.getLod1ImplicitRepresentation();
				break;
			case 1:
				implicit = bridgeConstruction.getLod2ImplicitRepresentation();
				break;
			case 2:
				implicit = bridgeConstruction.getLod3ImplicitRepresentation();
				break;
			case 3:
				implicit = bridgeConstruction.getLod4ImplicitRepresentation();
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

			if (implicitId != 0)
				preparedStatement.setLong(21 + i, implicitId);
			else
				preparedStatement.setNull(21 + i, Types.NULL);

			if (pointGeom != null)
				preparedStatement.setObject(25 + i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else
				preparedStatement.setNull(25 + i, nullGeometryType, nullGeometryTypeName);

			if (matrixString != null)
				preparedStatement.setString(29 + i, matrixString);
			else
				preparedStatement.setNull(29 + i, Types.VARCHAR);
		}

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(33, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BRIDGE_CONSTR_ELEMENT);

		// brid:boundedBy
		if (bridgeConstruction.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : bridgeConstruction.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, bridgeConstruction, bridgeConstructionId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_THEMATIC_SURFACE.getName(),
								href,
								bridgeConstructionId,
								"bridge_constr_element_id"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(bridgeConstruction, bridgeConstructionId, featureType);

		return bridgeConstructionId;
	}

}
