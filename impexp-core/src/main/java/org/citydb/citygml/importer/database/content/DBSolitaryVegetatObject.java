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

import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.citygml.vegetation.SolitaryVegetationObject;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;

public class DBSolitaryVegetatObject extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private GeometryConverter geometryConverter;
	private DBImplicitGeometry implicitGeometryImporter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;
	private boolean affineTransformation;

	public DBSolitaryVegetatObject(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
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
		return TableEnum.SOLITARY_VEGETAT_OBJECT.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "solitaryvegetatobject/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".solitary_vegetat_object (id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"species, species_codespace, height, height_unit, trunk_diameter, trunk_diameter_unit, crown_diameter, crown_diameter_unit, " +
				"lod1_brep_id, lod2_brep_id, lod3_brep_id, lod4_brep_id, " +
				"lod1_other_geom, lod2_other_geom, lod3_other_geom, lod4_other_geom, " +
				"lod1_implicit_rep_id, lod2_implicit_rep_id, lod3_implicit_rep_id, lod4_implicit_rep_id, " +
				"lod1_implicit_ref_point, lod2_implicit_ref_point, lod3_implicit_ref_point, lod4_implicit_ref_point, " +
				"lod1_implicit_transformation, lod2_implicit_transformation, lod3_implicit_transformation, lod4_implicit_transformation" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	protected long doImport(SolitaryVegetationObject vegetationObject) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(vegetationObject);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long vegetationObjectId = cityObjectImporter.doImport(vegetationObject, featureType);

		// import solitary vegetation object information
		// primary id
		preparedStatement.setLong(1, vegetationObjectId);

		// veg:class
		if (vegetationObject.isSetClazz() && vegetationObject.getClazz().isSetValue()) {
			preparedStatement.setString(2, vegetationObject.getClazz().getValue());
			preparedStatement.setString(3, vegetationObject.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// veg:function
		if (vegetationObject.isSetFunction()) {
			valueJoiner.join(vegetationObject.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(4, valueJoiner.result(0));
			preparedStatement.setString(5, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// veg:usage
		if (vegetationObject.isSetUsage()) {
			valueJoiner.join(vegetationObject.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// veg:species
		if (vegetationObject.isSetSpecies() && vegetationObject.getSpecies().isSetValue()) {
			preparedStatement.setString(8, vegetationObject.getSpecies().getValue());
			preparedStatement.setString(9, vegetationObject.getSpecies().getCodeSpace());
		} else {
			preparedStatement.setNull(8, Types.VARCHAR);
			preparedStatement.setNull(9, Types.VARCHAR);
		}

		// veg:height
		if (vegetationObject.isSetHeight() && vegetationObject.getHeight().isSetValue()) {
			preparedStatement.setDouble(10, vegetationObject.getHeight().getValue());
			preparedStatement.setString(11, vegetationObject.getHeight().getUom());
		} else {
			preparedStatement.setNull(10, Types.NULL);
			preparedStatement.setNull(11, Types.VARCHAR);
		}

		// veg:trunkDiameter
		if (vegetationObject.isSetTrunkDiameter() && vegetationObject.getTrunkDiameter().isSetValue()) {
			preparedStatement.setDouble(12, vegetationObject.getTrunkDiameter().getValue());
			preparedStatement.setString(13, vegetationObject.getTrunkDiameter().getUom());
		} else {
			preparedStatement.setNull(12, Types.NULL);
			preparedStatement.setNull(13, Types.VARCHAR);
		}

		// veg:crownDiameter
		if (vegetationObject.isSetCrownDiameter() && vegetationObject.getCrownDiameter().isSetValue()) {
			preparedStatement.setDouble(14, vegetationObject.getCrownDiameter().getValue());
			preparedStatement.setString(15, vegetationObject.getCrownDiameter().getUom());
		} else {
			preparedStatement.setNull(14, Types.NULL);
			preparedStatement.setNull(15, Types.VARCHAR);
		}

		// veg:lodXGeometry
		for (int i = 0; i < 4; i++) {
			GeometryProperty<? extends AbstractGeometry> geometryProperty = null;
			long geometryId = 0;
			GeometryObject geometryObject = null;

			switch (i) {
			case 0:
				geometryProperty = vegetationObject.getLod1Geometry();
				break;
			case 1:
				geometryProperty = vegetationObject.getLod2Geometry();
				break;
			case 2:
				geometryProperty = vegetationObject.getLod3Geometry();
				break;
			case 3:
				geometryProperty = vegetationObject.getLod4Geometry();
				break;
			}

			if (geometryProperty != null) {
				if (geometryProperty.isSetGeometry()) {
					AbstractGeometry abstractGeometry = geometryProperty.getGeometry();
					if (importer.isSurfaceGeometry(abstractGeometry))
						geometryId = surfaceGeometryImporter.doImport(abstractGeometry, vegetationObjectId);
					else if (importer.isPointOrLineGeometry(abstractGeometry))
						geometryObject = geometryConverter.getPointOrCurveGeometry(abstractGeometry);
					else 
						importer.logOrThrowUnsupportedGeometryMessage(vegetationObject, abstractGeometry);

					geometryProperty.unsetGeometry();
				} else {
					String href = geometryProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.SOLITARY_VEGETAT_OBJECT.getName(),
								vegetationObjectId, 
								href, 
								"lod" + (i + 1) + "_brep_id"));
					}
				}
			}

			if (geometryId != 0)
				preparedStatement.setLong(16 + i, geometryId);
			else
				preparedStatement.setNull(16 + i, Types.NULL);

			if (geometryObject != null)
				preparedStatement.setObject(20 + i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(geometryObject, batchConn));
			else
				preparedStatement.setNull(20 + i, nullGeometryType, nullGeometryTypeName);
		}

		// veg:lodXImplicitRepresentation
		for (int i = 0; i < 4; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = vegetationObject.getLod1ImplicitRepresentation();
				break;
			case 1:
				implicit = vegetationObject.getLod2ImplicitRepresentation();
				break;
			case 2:
				implicit = vegetationObject.getLod3ImplicitRepresentation();
				break;
			case 3:
				implicit = vegetationObject.getLod4ImplicitRepresentation();
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
				preparedStatement.setLong(24 + i, implicitId);
			else
				preparedStatement.setNull(24 + i, Types.NULL);

			if (pointGeom != null)
				preparedStatement.setObject(28 + i, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else
				preparedStatement.setNull(28 + i, nullGeometryType, nullGeometryTypeName);

			if (matrixString != null)
				preparedStatement.setString(32 + i, matrixString);
			else
				preparedStatement.setNull(32 + i, Types.VARCHAR);
		}

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(36, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.SOLITARY_VEGETAT_OBJECT);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(vegetationObject, vegetationObjectId, featureType);

		return vegetationObjectId;
	}

}
