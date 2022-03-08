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

import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.vegetation.PlantCover;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiSolidProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

public class DBPlantCover extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;

	public DBPlantCover(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}
	
	@Override
	protected String getTableName() {
		return TableEnum.PLANT_COVER.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "plantcover/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".plant_cover (id, class, class_codespace, function, function_codespace, usage, usage_codespace, average_height, average_height_unit, " +
				"lod1_multi_surface_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id, " +
				"lod1_multi_solid_id, lod2_multi_solid_id, lod3_multi_solid_id, lod4_multi_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	protected long doImport(PlantCover plantCover) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(plantCover);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long plantCoverId = cityObjectImporter.doImport(plantCover, featureType);

		// import plant cover information
		// primary id
		preparedStatement.setLong(1, plantCoverId);

		// veg:class
		if (plantCover.isSetClazz() && plantCover.getClazz().isSetValue()) {
			preparedStatement.setString(2, plantCover.getClazz().getValue());
			preparedStatement.setString(3, plantCover.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// veg:function
		if (plantCover.isSetFunction()) {
			valueJoiner.join(plantCover.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(4, valueJoiner.result(0));
			preparedStatement.setString(5, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// veg:usage
		if (plantCover.isSetUsage()) {
			valueJoiner.join(plantCover.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// veg:averageHeight
		if (plantCover.isSetAverageHeight() && plantCover.getAverageHeight().isSetValue()) {
			preparedStatement.setDouble(8, plantCover.getAverageHeight().getValue());
			preparedStatement.setString(9, plantCover.getAverageHeight().getUom());
		} else {
			preparedStatement.setNull(8, Types.NULL);
			preparedStatement.setNull(9, Types.VARCHAR);
		}

		// veg:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				plantCover.getLod1MultiSurface(),
				plantCover.getLod2MultiSurface(),
				plantCover.getLod3MultiSurface(),
				plantCover.getLod4MultiSurface()
		}, new int[]{1, 2, 3, 4}, "_multi_surface_id", 10);

		// veg:lodXMultiSolid
		importSurfaceGeometryProperties(new MultiSolidProperty[]{
				plantCover.getLod1MultiSolid(),
				plantCover.getLod2MultiSolid(),
				plantCover.getLod3MultiSolid(),
				plantCover.getLod4MultiSolid()
		}, new int[]{1, 2, 3, 4}, "_multi_solid_id", 14);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(18, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.PLANT_COVER);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(plantCover, plantCoverId, featureType);

		return plantCoverId;
	}

}
