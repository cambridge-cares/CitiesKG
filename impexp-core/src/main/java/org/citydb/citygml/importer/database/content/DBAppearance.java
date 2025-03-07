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
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map.Entry;

public class DBAppearance extends AbstractDBImporter {
	private DBSurfaceData surfaceDataImporter;
	private TexturedSurfaceConverter texturedSurfaceConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean replaceGmlId;
	private String gmlIdCodespace;

	public DBAppearance(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceDataImporter = importer.getImporter(DBSurfaceData.class);
		texturedSurfaceConverter = new TexturedSurfaceConverter(this, config, importer);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		replaceGmlId = config.getProject().getImporter().getGmlId().isUUIDModeReplace();
		gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
		if (gmlIdCodespace != null)
			gmlIdCodespace = "'" + gmlIdCodespace + "', ";
	}

	@Override
	protected String getTableName() {
		return TableEnum.APPEARANCE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "appearance/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".appearance (id, gmlid, " + (gmlIdCodespace != null ? "gmlid_codespace, " : "") +
				"name, name_codespace, description, theme, citymodel_id, cityobject_id) values " +
				"(?, ?, " + (gmlIdCodespace != null ? gmlIdCodespace : "") + "?, ?, ?, ?, ?, ?)";
	}

	@Override
	protected String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + "> " +
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
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
				URL url = new URL(iriGraphObject + uuid + "/");
				preparedStatement.setURL(++index, url);
				preparedStatement.setURL(++index, url);
				appearance.setLocalProperty(CoreConstants.OBJECT_URIID, url);
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
			}
    } else {
      preparedStatement.setLong(++index, appearanceId);
		}

		preparedStatement.setString(++index, appearance.getId());

		// gml:name
		if (appearance.isSetName()) {
			valueJoiner.join(appearance.getName(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// gml:description
		if (appearance.isSetDescription()) {
			String description = appearance.getDescription().getValue();
			if (description != null)
				description = description.trim();

			preparedStatement.setString(++index, description);
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// app:theme
		preparedStatement.setString(++index, appearance.getTheme());

		// cityobject or citymodel id
		if (isLocalAppearance) {
			if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
				preparedStatement.setURL(++index, (URL) parentId);
				appearance.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentId);
			} else {
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setLong(++index, (long) parentId);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.NULL);
			preparedStatement.setNull(++index, Types.NULL);
		}

		preparedStatement.addBatch();
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

}
