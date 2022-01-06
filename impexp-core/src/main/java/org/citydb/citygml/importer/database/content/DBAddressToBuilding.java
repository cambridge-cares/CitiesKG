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

import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;

public class DBAddressToBuilding extends AbstractDBImporter {

	public DBAddressToBuilding(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		super(batchConn, config, importer);
	}

	@Override
	protected String getTableName() {
		return TableEnum.ADDRESS_TO_BUILDING.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "addresstobuilding/";
	}

	@Override
	protected String getSQLStatement(){
		return "insert into " + sqlSchema + ".address_to_building (building_id, address_id) values " +
				"(?, ?)";
	}

	@Override
	protected String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + ">" +
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_BUILDING_ID + param +
				SchemaManagerAdapter.ONTO_ADDRESS_ID + param  +
				".}" +
				"}";
		return stmt;
	}

	protected void doImport(long addressId, long buildingId) throws CityGMLImportException, SQLException {

		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(iriGraphObject + uuid + "/");
				preparedStatement.setURL(++index, url);
			} catch (MalformedURLException e) {
				preparedStatement.setObject(++index, NodeFactory.createBlankNode());
			}
		}

		preparedStatement.setLong(++index, buildingId);
		preparedStatement.setLong(++index, addressId);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.ADDRESS_TO_BUILDING);
	}

	protected void doImport(URL addressURL, URL buildingURL) throws CityGMLImportException, SQLException {

		int index = 0;

		preparedStatement.setURL(++index, addressURL);
		preparedStatement.setURL(++index, buildingURL);
		preparedStatement.setURL(++index, addressURL);
		preparedStatement.addBatch();

		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.ADDRESS_TO_BUILDING);
	}

}
