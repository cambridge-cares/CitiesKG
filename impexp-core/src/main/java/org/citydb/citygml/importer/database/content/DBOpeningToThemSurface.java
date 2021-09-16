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
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.building.AbstractOpening;
import org.citygml4j.model.citygml.core.AbstractCityObject;

public class DBOpeningToThemSurface implements DBImporter {
	private final CityGMLImportManager importer;
	private final Connection batchConn;

	private PreparedStatement psOpeningToThemSurface;
	private int batchCounter;

	private String PREFIX_ONTOCITYGML;
	private String IRI_GRAPH_BASE;
	private String IRI_GRAPH_OBJECT;
	private static final String IRI_GRAPH_OBJECT_REL = "openingtothemsurface/";

	public DBOpeningToThemSurface(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.importer = importer;
		this.batchConn = batchConn;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String stmt = "insert into " + schema + ".opening_to_them_surface (opening_id, thematic_surface_id) values " +
				"(?, ?)";

		// Modification for SPARQL
		if (importer.isBlazegraph()) {
			PREFIX_ONTOCITYGML = importer.getOntoCityGmlPrefix();
			IRI_GRAPH_BASE = importer.getGraphBaseIri();
			IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
			stmt = getSPARQLStatement();
		}
		psOpeningToThemSurface = batchConn.prepareStatement(stmt);
	}

	private String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + ">" +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_OPENING_ID + param +
				SchemaManagerAdapter.ONTO_THEMSURFACE_ID + param  +
				".}" +
				"}";
		return stmt;
	}

	protected void doImport(long openingId, long thematicSurfaceId) throws CityGMLImportException, SQLException {

		psOpeningToThemSurface.setLong(1, openingId);
		psOpeningToThemSurface.setLong(2, thematicSurfaceId);

		psOpeningToThemSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.OPENING_TO_THEM_SURFACE);
	}

	protected void doImport(URL openingURL, URL thematicSurfaceURL) throws CityGMLImportException, SQLException {

		int index = 0;

		psOpeningToThemSurface.setURL(++index, openingURL);
		psOpeningToThemSurface.setURL(++index, thematicSurfaceURL);
		psOpeningToThemSurface.setURL(++index, openingURL);
		psOpeningToThemSurface.addBatch();

		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.OPENING_TO_THEM_SURFACE);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psOpeningToThemSurface.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psOpeningToThemSurface.close();
	}

}
