/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * Copyright 2013 - 2017
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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;

public class DBTunnelOpenToThemSrf implements DBImporter {
	private final CityGMLImportManager importer;

	private PreparedStatement psTunnelOpenToThemSrf;
	private int batchCounter;

	public DBTunnelOpenToThemSrf(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		StringBuilder stmt = new StringBuilder()
				.append("insert into ").append(schema).append(".tunnel_open_to_them_srf (tunnel_opening_id, tunnel_thematic_surface_id) values ")
				.append("(?, ?)");
		psTunnelOpenToThemSrf = batchConn.prepareStatement(stmt.toString());
	}

	protected void doImport(long openingId, long thematicSurfaceId) throws CityGMLImportException, SQLException {
		psTunnelOpenToThemSrf.setLong(1, openingId);
		psTunnelOpenToThemSrf.setLong(2, thematicSurfaceId);

		psTunnelOpenToThemSrf.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TUNNEL_OPEN_TO_THEM_SRF);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psTunnelOpenToThemSrf.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psTunnelOpenToThemSrf.close();
	}

}
