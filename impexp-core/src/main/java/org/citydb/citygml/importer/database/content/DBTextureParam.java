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
import org.citydb.citygml.importer.util.LocalAppearanceHandler.SurfaceGeometryTarget;
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;

public class DBTextureParam implements DBImporter {
	private final Connection batchConn;
	private final CityGMLImportManager importer;

	private PreparedStatement psTextureParam;
	private int batchCounter;
	//@todo Replace graph IRI and OntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + "/textureparam/";

	public DBTextureParam(Connection batchConn, Config config, CityGMLImportManager importer) throws SQLException {
		this.batchConn = batchConn;
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String texCoordListStmt = "insert into " + schema + ".textureparam (surface_geometry_id, is_texture_parametrization, " +
				"world_to_texture, texture_coordinates, surface_data_id) values " +
				"(?, ?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			String param = "  ?;";
			texCoordListStmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
					"INSERT DATA" +
					" { GRAPH <" + IRI_GRAPH_OBJECT + "> " +
						"{ ? "+ SchemaManagerAdapter.ONTO_SURFACE_GEOMETRY_ID + param +
								SchemaManagerAdapter.ONTO_IS_TEXTURE_PARAMETRIZATION + param  +
								SchemaManagerAdapter.ONTO_WORLD_TO_TEXTURE + param  +
								SchemaManagerAdapter.ONTO_TEXTURE_COORDINATES + param +
								SchemaManagerAdapter.ONTO_SURFACE_DATA_ID + param +
						".}" +
					"}";
		}

		psTextureParam = batchConn.prepareStatement(texCoordListStmt);
	}

	protected void doImport(SurfaceGeometryTarget target, long surfaceDataId) throws CityGMLImportException, SQLException {
		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psTextureParam.setURL(++index, url);
			} catch (MalformedURLException e) {
				setBlankNode(psTextureParam, ++index);
			}
		}

		psTextureParam.setLong(++index, target.getSurfaceGeometryId());
		psTextureParam.setInt(++index, 1);
		if (importer.isBlazegraph())  {
			setBlankNode(psTextureParam, ++index);
		} else {
			psTextureParam.setNull(++index, Types.VARCHAR);
		}
		psTextureParam.setObject(++index, importer.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(target.compileTextureCoordinates(), batchConn));
		psTextureParam.setLong(++index, surfaceDataId);

		addBatch();
	}

	protected void doImport(String worldToTexture, long surfaceDataId, long surfaceGeometryId) throws CityGMLImportException, SQLException {
		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psTextureParam.setURL(++index, url);
			} catch (MalformedURLException e) {
				setBlankNode(psTextureParam, ++index);
			}
		}

		psTextureParam.setLong(++index, surfaceGeometryId);
		psTextureParam.setInt(++index, 1);
		psTextureParam.setString(++index, worldToTexture);
		if (importer.isBlazegraph())  {
			setBlankNode(psTextureParam, ++index);
		} else {
			psTextureParam.setNull(++index, importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
					importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());
		}

		psTextureParam.setLong(++index, surfaceDataId);

		addBatch();
	}

	protected void doImport(long surfaceDataId, long surfaceGeometryId) throws CityGMLImportException, SQLException {
		int index = 0;

		if (importer.isBlazegraph()) {
			try {
				String uuid = importer.generateNewGmlId();
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psTextureParam.setURL(++index, url);
			} catch (MalformedURLException e) {
				setBlankNode(psTextureParam, ++index);
			}
		}

		psTextureParam.setLong(++index, surfaceGeometryId);
		psTextureParam.setInt(++index, 0);
		if (importer.isBlazegraph())  {
			setBlankNode(psTextureParam, ++index);
			setBlankNode(psTextureParam, ++index);
		} else {
			psTextureParam.setNull(++index, Types.VARCHAR);
			psTextureParam.setNull(++index, importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
					importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());
		}

		psTextureParam.setLong(++index, surfaceDataId);

		addBatch();
	}

	private void addBatch() throws CityGMLImportException, SQLException {
		psTextureParam.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TEXTUREPARAM);		
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psTextureParam.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psTextureParam.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

}
