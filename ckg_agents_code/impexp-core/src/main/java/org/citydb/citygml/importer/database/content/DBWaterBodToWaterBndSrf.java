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



import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;

public class DBWaterBodToWaterBndSrf extends AbstractDBImporter {

	public DBWaterBodToWaterBndSrf(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		super(batchConn, config, importer);
	}

	@Override
	protected String getTableName() {
		return TableEnum.WATERBOD_TO_WATERBND_SRF.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "waterbodtowaterbndsrf/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".waterbod_to_waterbnd_srf (waterboundary_surface_id, waterbody_id) values " +
				"(?, ?)";
	}

	protected void doImport(long waterSurfaceId, long waterBodyId) throws CityGMLImportException, SQLException {
		preparedStatement.setLong(1, waterSurfaceId);
		preparedStatement.setLong(2, waterBodyId);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.WATERBOD_TO_WATERBND_SRF);
	}

}