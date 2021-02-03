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
import java.sql.Types;

import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.SequenceEnum;
import org.citydb.database.schema.TableEnum;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.core.ExternalObject;
import org.citygml4j.model.citygml.core.ExternalReference;
import org.citygml4j.model.gml.base.AbstractGML;

public class DBExternalReference implements DBImporter {
	private final CityGMLImportManager importer;

	private PreparedStatement psExternalReference;
	private int batchCounter;
	//@todo Replace graph IRI and OntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT_REL = "externalreference/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;

	public DBExternalReference(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String stmt = "insert into " + schema + ".external_reference (id, infosys, name, uri, cityobject_id) values " +
				"(" + importer.getDatabaseAdapter().getSQLAdapter().getNextSequenceValue(SequenceEnum.EXTERNAL_REFERENCE_ID_SEQ.getName()) +
				", ?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			stmt = getSPARQLStatement();
		}

		psExternalReference = batchConn.prepareStatement(stmt);
	}

	private String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_INFO_SYS + param +
				SchemaManagerAdapter.ONTO_NAME + param +
				SchemaManagerAdapter.ONTO_URI + param +
				SchemaManagerAdapter.ONTO_CITY_OBJECT_ID + param +
				".}" +
				"}";
		return stmt;
	}


	protected void doImport(ExternalReference externalReference, long cityObjectId) throws CityGMLImportException, SQLException {
		boolean isBlazegraph = importer.getDatabaseAdapter().getDatabaseType().value().equals(DatabaseType.BLAZE.value());

		int index = 0;

		if (isBlazegraph) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psExternalReference.setURL(++index, url);
				psExternalReference.setURL(++index, url);
			} catch (MalformedURLException e) {
				psExternalReference.setObject(++index, NodeFactory.createBlankNode());
				psExternalReference.setObject(++index, NodeFactory.createBlankNode());
			}
		}

		// core:informationSystem
		if (externalReference.isSetInformationSystem())
			psExternalReference.setString(++index, externalReference.getInformationSystem());
		else
			psExternalReference.setNull(++index, Types.VARCHAR);

		// core:externalObject
		if (externalReference.isSetExternalObject()) {
			ExternalObject externalObject = externalReference.getExternalObject();

			// core:name
			if (externalObject.isSetName()) {
				psExternalReference.setString(++index, externalObject.getName());
			} else if (isBlazegraph) {
				psExternalReference.setObject(++index, NodeFactory.createBlankNode());
			} else {
				psExternalReference.setNull(++index, Types.VARCHAR);
			}

			// core:uri
			if (externalObject.isSetUri()) {
				if (isBlazegraph) {
					try {
						URL extURL = new URL(externalObject.getUri());
						psExternalReference.setURL(++index, extURL);
					} catch (MalformedURLException e) {
						throw new CityGMLImportException(e);
					}
        } else {
          psExternalReference.setString(++index, externalObject.getUri());
				}
			} else if (isBlazegraph) {
				psExternalReference.setObject(++index, NodeFactory.createBlankNode());
			} else {
				psExternalReference.setNull(++index, Types.VARCHAR);
			}
		} else if (isBlazegraph) {
			psExternalReference.setObject(++index, NodeFactory.createBlankNode());
			psExternalReference.setObject(++index, NodeFactory.createBlankNode());
		} else {
			psExternalReference.setNull(++index, Types.VARCHAR);
			psExternalReference.setNull(++index, Types.VARCHAR);
		}

		// cityObjectId
		if (isBlazegraph) {
			psExternalReference.setURL(++index, (URL) ((AbstractGML)externalReference.getParent())
					.getLocalProperty(CoreConstants.OBJECT_URIID));
		} else {
			psExternalReference.setLong(++index, cityObjectId);
		}


		psExternalReference.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.EXTERNAL_REFERENCE);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psExternalReference.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psExternalReference.close();
	}

}
