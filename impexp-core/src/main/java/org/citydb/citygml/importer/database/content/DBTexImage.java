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

import org.apache.jena.graph.NodeFactory;
import org.citydb.citygml.common.database.xlink.DBXlinkTextureFile;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.ConcurrentLockManager;
import org.citydb.citygml.importer.util.ExternalFileChecker;
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.SequenceEnum;
import org.citydb.database.schema.TableEnum;
import org.citydb.log.Logger;
import org.citygml4j.model.citygml.appearance.AbstractTexture;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class DBTexImage implements DBImporter {
	private final ConcurrentLockManager lockManager = ConcurrentLockManager.getInstance(DBTexImage.class);
	private final Logger log = Logger.getInstance();
	private final CityGMLImportManager importer;
	private PreparedStatement psInsertStmt;	

	private ExternalFileChecker externalFileChecker;
	private MessageDigest md5;
	private boolean importTextureImage;
	private int batchCounter;
	//@todo Replace graph IRI and OOntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + "/teximage/";

	public DBTexImage(Connection connection, Config config, CityGMLImportManager importer) throws SQLException {
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();
		importTextureImage = config.getProject().getImporter().getAppearances().isSetImportTextureFiles();
		externalFileChecker = importer.getExternalFileChecker();

		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		}

		String stmt = "insert into " + schema + ".tex_image (id, tex_image_uri, tex_mime_type, tex_mime_type_codespace) values " +
				"(?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			String param = "  ?;";
			stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
					"INSERT DATA" +
					" { GRAPH <" + IRI_GRAPH_OBJECT + "> " +
						"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
							SchemaManagerAdapter.ONTO_TEX_IMAGE_URI + param +
							SchemaManagerAdapter.ONTO_TEX_MIME_TYPE + param +
							SchemaManagerAdapter.ONTO_TEX_MIME_TYPE_CODESPACE + param +
						".}" +
					"}";
		}

		psInsertStmt = connection.prepareStatement(stmt);
	}

	public long doImport(AbstractTexture abstractTexture, long surfaceDataId) throws CityGMLImportException, SQLException {
		String imageURI = abstractTexture.getImageURI().trim();
		if (imageURI.isEmpty())
			return 0;

		long texImageId = 0;
		String md5URI = toHexString(md5.digest(imageURI.getBytes()));

		Map.Entry<String, String> fileInfo = null;
		boolean insertIntoTexImage = false;

		// synchronize concurrent processing of the same texture image
		// different texture images however may be processed concurrently
		ReentrantLock lock = lockManager.getLock(md5URI);
		lock.lock();
		try {
			texImageId = importer.getTextureImageId(md5URI);
			if (texImageId == -1) {
				try {
					fileInfo = externalFileChecker.getFileInfo(imageURI);
					texImageId = importer.getNextSequenceValue(SequenceEnum.TEX_IMAGE_ID_SEQ.getName());
					insertIntoTexImage = true;
				} catch (IOException e) {
					log.error("Failed to read image file at '" + imageURI + "': " + e.getMessage());
					texImageId = 0;
				}

				importer.putTextureImageUID(md5URI, texImageId);
			}
		} finally {
			lockManager.releaseLock(md5URI);
			lock.unlock();
		}

		if (insertIntoTexImage) {
			// fill TEX_IMAGE with texture file properties
			String fileName = fileInfo.getValue();
			String mimeType = null;
			String codeSpace = null;

			if (abstractTexture.isSetMimeType()) {
				mimeType = abstractTexture.getMimeType().getValue();
				codeSpace = abstractTexture.getMimeType().getCodeSpace();
			}

			int index = 0;

			if (importer.isBlazegraph()) {
				try {
					String uuid = abstractTexture.getId();
					if (uuid.isEmpty()) {
						uuid = importer.generateNewGmlId();
					}
					URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
					psInsertStmt.setURL(++index, url);
				} catch (MalformedURLException e) {
					psInsertStmt.setObject(++index, NodeFactory.createBlankNode());
				}
			}

			psInsertStmt.setLong(++index, texImageId);
			psInsertStmt.setString(++index, fileName);
			psInsertStmt.setString(++index, mimeType);

			if (codeSpace == null && importer.isBlazegraph()) {
				psInsertStmt.setObject(++index, NodeFactory.createBlankNode());
			} else {
				psInsertStmt.setString(++index, codeSpace);
			}



			psInsertStmt.addBatch();
			if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
				importer.executeBatch(TableEnum.TEX_IMAGE);

			if (importTextureImage) {
				// propagte xlink to import the texture file itself
				importer.propagateXlink(new DBXlinkTextureFile(
						texImageId,
						fileInfo.getKey()));
			}
		}

		return texImageId;
	}

	private String toHexString(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes)
			hexString.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));

		return hexString.toString();
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psInsertStmt.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psInsertStmt.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

}
