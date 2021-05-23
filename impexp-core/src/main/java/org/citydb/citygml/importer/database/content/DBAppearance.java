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
import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.citygml.importer.util.LocalAppearanceHandler;
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.SequenceEnum;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.appearance.AbstractSurfaceData;
import org.citygml4j.model.citygml.appearance.Appearance;
import org.citygml4j.model.citygml.appearance.SurfaceDataProperty;
import org.citygml4j.model.citygml.texturedsurface._AbstractAppearance;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.primitives.AbstractSurface;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map.Entry;

public class DBAppearance implements DBImporter {
	private final CityGMLImportManager importer;

	private PreparedStatement psAppearance;
	private DBSurfaceData surfaceDataImporter;
	private TexturedSurfaceConverter texturedSurfaceConverter;
	private AttributeValueJoiner valueJoiner;

	private int batchCounter;
	private boolean replaceGmlId;
	//@todo Replace graph IRI and OOntocityGML prefix with variables set on the GUI
	private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
	private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
	private static final String IRI_GRAPH_OBJECT_REL = "appearance/";
	private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;

	public DBAppearance(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		this.importer = importer;

		replaceGmlId = config.getProject().getImporter().getGmlId().isUUIDModeReplace();
		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
		if (gmlIdCodespace != null)
			gmlIdCodespace = "'" + gmlIdCodespace + "', ";

		String stmt = "insert into " + schema + ".appearance (id, gmlid, " + (gmlIdCodespace != null ? "gmlid_codespace, " : "") +
				"name, name_codespace, description, theme, citymodel_id, cityobject_id) values " +
				"(?, ?, " + (gmlIdCodespace != null ? gmlIdCodespace : "") + "?, ?, ?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			stmt = getSPARQLStatement(gmlIdCodespace);
		}

		psAppearance = batchConn.prepareStatement(stmt);

		surfaceDataImporter = importer.getImporter(DBSurfaceData.class);
		texturedSurfaceConverter = new TexturedSurfaceConverter(this, config, importer);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	private String getSPARQLStatement(String gmlIdCodespace){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_GML_ID + param +
				(gmlIdCodespace != null ? SchemaManagerAdapter.ONTO_GML_ID + param : "") +
				SchemaManagerAdapter.ONTO_NAME + param +
				SchemaManagerAdapter.ONTO_NAME_CODESPACE + param +
				SchemaManagerAdapter.ONTO_DESCRIPTION + param +
				SchemaManagerAdapter.ONTO_THEME + param +
				SchemaManagerAdapter.ONTO_CITY_MODEL_ID + param +
				SchemaManagerAdapter.ONTO_CITY_OBJECT_ID + param +
				".}" +
				"}";

		return stmt;
	}

	public long doImport(Appearance appearance, Object parentId, boolean isLocalAppearance) throws CityGMLImportException, SQLException {
		long appearanceId = importer.getNextSequenceValue(SequenceEnum.APPEARANCE_ID_SEQ.getName());

		FeatureType featureType = importer.getFeatureType(appearance);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");


		// gml:id
		String origGmlId = appearance.getId();
		if (origGmlId != null)
			appearance.setLocalProperty(CoreConstants.OBJECT_ORIGINAL_GMLID, origGmlId);
		
		if (replaceGmlId) {
			String gmlId = importer.generateNewGmlId();

			// mapping entry
			if (appearance.isSetId())
				importer.putObjectUID(appearance.getId(), appearanceId, gmlId, featureType.getObjectClassId());

			appearance.setId(gmlId);

		} else {
			if (appearance.isSetId())
				importer.putObjectUID(appearance.getId(), appearanceId, featureType.getObjectClassId());
			else
				appearance.setId(importer.generateNewGmlId());
		}

		int index = 0;

		// primary id
		if (importer.isBlazegraph()) {
			try {
				String uuid = appearance.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				URL url = new URL(IRI_GRAPH_OBJECT + uuid + "/");
				psAppearance.setURL(++index, url);
				psAppearance.setURL(++index, url);
				appearance.setLocalProperty(CoreConstants.OBJECT_URIID, url);
			} catch (MalformedURLException e) {
				psAppearance.setObject(++index, NodeFactory.createBlankNode());
			}
    } else {
      psAppearance.setLong(++index, appearanceId);
		}

		psAppearance.setString(++index, appearance.getId());

		// gml:name
		if (appearance.isSetName()) {
			valueJoiner.join(appearance.getName(), Code::getValue, Code::getCodeSpace);
			psAppearance.setString(++index, valueJoiner.result(0));
			psAppearance.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(psAppearance, ++index);
			setBlankNode(psAppearance, ++index);
		} else {
			psAppearance.setNull(++index, Types.VARCHAR);
			psAppearance.setNull(++index, Types.VARCHAR);
		}

		// gml:description
		if (appearance.isSetDescription()) {
			String description = appearance.getDescription().getValue();
			if (description != null)
				description = description.trim();

			psAppearance.setString(++index, description);
		} else if (importer.isBlazegraph()) {
			setBlankNode(psAppearance, ++index);
		} else {
			psAppearance.setNull(++index, Types.VARCHAR);
		}

		// app:theme
		psAppearance.setString(++index, appearance.getTheme());

		// cityobject or citymodel id
		if (isLocalAppearance) {
			if (importer.isBlazegraph()) {
				setBlankNode(psAppearance, ++index);
				psAppearance.setURL(++index, (URL) parentId);
				appearance.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentId);
			} else {
				psAppearance.setNull(++index, Types.NULL);
				psAppearance.setLong(++index, (long) parentId);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(psAppearance, ++index);
			setBlankNode(psAppearance, ++index);
		} else {
			psAppearance.setNull(++index, Types.NULL);
			psAppearance.setNull(++index, Types.NULL);
		}

		psAppearance.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.APPEARANCE);

		// surfaceData members
		if (appearance.isSetSurfaceDataMember()) {
			for (SurfaceDataProperty property : appearance.getSurfaceDataMember()) {
				AbstractSurfaceData surfaceData = property.getSurfaceData();

				if (surfaceData != null) {
					surfaceDataImporter.doImport(surfaceData, appearanceId, isLocalAppearance);
					property.unsetSurfaceData();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.APPEAR_TO_SURFACE_DATA.getName(),
								appearanceId,
								"APPEARANCE_ID",
								href,
								"SURFACE_DATA_ID"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(appearance, appearanceId, featureType);

		importer.updateObjectCounter(appearance, featureType, appearanceId);
		return appearanceId;
	}

	protected void importLocalAppearance() throws CityGMLImportException, SQLException {
		LocalAppearanceHandler handler = importer.getLocalAppearanceHandler();

		if (handler != null) {
			if (handler.hasAppearances()) {
				for (Entry<Object, List<Appearance>> entry : handler.getAppearances().entrySet()) {
					for (Appearance appearance : entry.getValue())
						doImport(appearance, entry.getKey(), true);
				}
			}

			// reset appearance handler
			handler.reset();
		}
	}

	protected void importTexturedSurface(_AbstractAppearance _appearance, AbstractSurface abstractSurface, long parentId, boolean isFront, String target) throws CityGMLImportException, SQLException {
		texturedSurfaceConverter.convertTexturedSurface(_appearance, abstractSurface, parentId, isFront, target);	
	}

	protected void importTexturedSurfaceXlink(String href, long surfaceGeometryId, long parentId) throws CityGMLImportException, SQLException {
		texturedSurfaceConverter.convertTexturedSurfaceXlink(href, surfaceGeometryId, parentId);
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		texturedSurfaceConverter.flush();

		if (batchCounter > 0) {
			psAppearance.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psAppearance.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

}
