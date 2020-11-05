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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;

public class DBAddressToBuilding implements DBImporter {
	private final CityGMLImportManager importer;

	private PreparedStatement psAddressToBuilding;
	private int batchCounter;
	//@todo Replace graph IRI and OntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + "/addresstobuilding/";

	public DBAddressToBuilding(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String stmt = "insert into " + schema + ".address_to_building (building_id, address_id) values " +
				"(?, ?)";

		if (importer.isBlazegraph()) {
			String param = "  ?;";
			stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
					"INSERT DATA" +
					" { GRAPH <" + IRI_GRAPH_OBJECT + "> " +
						"{ ? "+ SchemaManagerAdapter.ONTO_BUILDING_ID + param +
								SchemaManagerAdapter.ONTO_ADDRESS_ID + param  +
						".}" +
					"}";
		}

		psAddressToBuilding = batchConn.prepareStatement(stmt);
	}

	protected void doImport(long addressId, long buildingId) throws CityGMLImportException, SQLException {

		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psAddressToBuilding.setURL(++index, url);
			} catch (MalformedURLException e) {
				psAddressToBuilding.setObject(++index, NodeFactory.createBlankNode());
			}
		}

		psAddressToBuilding.setLong(++index, buildingId);
		psAddressToBuilding.setLong(++index, addressId);

		psAddressToBuilding.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.ADDRESS_TO_BUILDING);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			//psAddressToBuilding.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psAddressToBuilding.close();
	}

}
