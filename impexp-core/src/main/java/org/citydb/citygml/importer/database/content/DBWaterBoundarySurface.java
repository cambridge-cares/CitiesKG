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

import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.waterbody.AbstractWaterBoundarySurface;
import org.citygml4j.model.citygml.waterbody.WaterBody;
import org.citygml4j.model.citygml.waterbody.WaterSurface;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class DBWaterBoundarySurface extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBWaterBodToWaterBndSrf bodyToSurfaceImporter;

	public DBWaterBoundarySurface(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		bodyToSurfaceImporter = importer.getImporter(DBWaterBodToWaterBndSrf.class);
	}

	@Override
	protected String getTableName() {
		return TableEnum.WATERBOUNDARY_SURFACE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "waterboundarysurface/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".waterboundary_surface (id, objectclass_id, water_level, water_level_codespace, " +
				"lod2_surface_id, lod3_surface_id, lod4_surface_id) values " +
				"(?, ?, ?, ?, ?, ?, ?)";
	}

	@Override
	protected String getSPARQLStatement() {
		return "NOT IMPLEMENTED.";
	}

	protected long doImport(AbstractWaterBoundarySurface waterBoundarySurface) throws CityGMLImportException, SQLException {
		return doImport(waterBoundarySurface, null, 0);
	}

	public long doImport(AbstractWaterBoundarySurface waterBoundarySurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(waterBoundarySurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long waterBoundarySurfaceId = cityObjectImporter.doImport(waterBoundarySurface, featureType);

		// import boundary surface information
		// primary id
		preparedStatement.setLong(1, waterBoundarySurfaceId);

		// objectclass id
		preparedStatement.setLong(2, featureType.getObjectClassId());

		// wtr:waterLevel
		if (waterBoundarySurface instanceof WaterSurface 
				&& ((WaterSurface)waterBoundarySurface).isSetWaterLevel() && ((WaterSurface)waterBoundarySurface).getWaterLevel().isSetValue()) {
			preparedStatement.setString(3, ((WaterSurface)waterBoundarySurface).getWaterLevel().getValue());
			preparedStatement.setString(4, ((WaterSurface)waterBoundarySurface).getWaterLevel().getCodeSpace());
		} else {
			preparedStatement.setNull(3, Types.NULL);
			preparedStatement.setNull(4, Types.VARCHAR);
		}

		// wtr:lodXMultiSurface
		importSurfaceGeometryProperties(new SurfaceProperty[]{
				waterBoundarySurface.getLod2Surface(),
				waterBoundarySurface.getLod3Surface(),
				waterBoundarySurface.getLod4Surface()
		}, new int[]{2, 3, 4}, "_surface_id", 5);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.WATERBOUNDARY_SURFACE);

		// boundary surface to water body
		if (parent instanceof WaterBody)
			bodyToSurfaceImporter.doImport(waterBoundarySurfaceId, parentId);
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(waterBoundarySurface, waterBoundarySurfaceId, featureType);

		return waterBoundarySurfaceId;
	}

}
