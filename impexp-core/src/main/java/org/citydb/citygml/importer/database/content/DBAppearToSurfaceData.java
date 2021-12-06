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

public class DBAppearToSurfaceData extends AbstractDBImporter {

	public DBAppearToSurfaceData(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		super(batchConn,config,importer);
	}

	@Override
	protected String getTableName() {
		return TableEnum.APPEAR_TO_SURFACE_DATA.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "appeartosurfacedata/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".appear_to_surface_data (surface_data_id, appearance_id) values " +
				"(?, ?)";
	}

	@Override
	protected String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_SURFACE_DATA_ID + param +
				SchemaManagerAdapter.ONTO_APPEARANCE_ID + param  +
				".}" +
				"}";

		return stmt;
	}

	public void doImport(Object surfaceDataId, Object appearanceId) throws CityGMLImportException, SQLException {

		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				preparedStatement.setURL(++index, url);
				preparedStatement.setURL(++index, (URL) surfaceDataId);
				preparedStatement.setURL(++index, (URL) appearanceId);
			} catch (MalformedURLException e) {
				preparedStatement.setObject(++index, NodeFactory.createBlankNode());
			}
		} else {
			preparedStatement.setLong(++index, (long) surfaceDataId);
			preparedStatement.setLong(++index, (long) appearanceId);
		}

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.APPEAR_TO_SURFACE_DATA);

	}

}
