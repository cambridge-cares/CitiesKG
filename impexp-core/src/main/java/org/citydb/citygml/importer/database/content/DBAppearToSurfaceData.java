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

public class DBAppearToSurfaceData implements DBImporter {
	private final CityGMLImportManager importer;

	private PreparedStatement psAppearToSurfaceData;
	private int batchCounter;
	//@todo Replace graph IRI and OntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT_REL = "appeartosurfacedata/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;


	public DBAppearToSurfaceData(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String stmt = "insert into " + schema + ".appear_to_surface_data (surface_data_id, appearance_id) values " +
				"(?, ?)";

		if (importer.isBlazegraph()) {
			stmt = getSPARQLStatement();
		}

		psAppearToSurfaceData = batchConn.prepareStatement(stmt);
	}

	private String getSPARQLStatement(){
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


	public void doImport(long surfaceDataId, long appearanceId) throws CityGMLImportException, SQLException {

		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psAppearToSurfaceData.setURL(++index, url);
			} catch (MalformedURLException e) {
				psAppearToSurfaceData.setObject(++index, NodeFactory.createBlankNode());
			}
		}

		psAppearToSurfaceData.setLong(++index, surfaceDataId);
		psAppearToSurfaceData.setLong(++index, appearanceId);


		psAppearToSurfaceData.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.APPEAR_TO_SURFACE_DATA);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psAppearToSurfaceData.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psAppearToSurfaceData.close();
	}

}
