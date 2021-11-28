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
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.building.*;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class DBThematicSurface extends AbstractDBImporter {

	private DBCityObject cityObjectImporter;
	private DBOpening openingImporter;

	public DBThematicSurface(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		openingImporter = importer.getImporter(DBOpening.class);
	}

	@Override
	protected String getTableName() {
		return TableEnum.THEMATIC_SURFACE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "thematicsurface/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".thematic_surface (id, objectclass_id, building_id, room_id, building_installation_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?)";
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? " + SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param +
				SchemaManagerAdapter.ONTO_BUILDING_ID + param +
				SchemaManagerAdapter.ONTO_ROOM_ID + param +
				SchemaManagerAdapter.ONTO_BUILDING_INSTALLATION_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID + param +
				".}" +
				"}";

		return stmt;
	}


	protected long doImport(AbstractBoundarySurface boundarySurface) throws CityGMLImportException, SQLException {
		return doImport(boundarySurface, null, 0);
	}

	public long doImport(AbstractBoundarySurface boundarySurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(boundarySurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		objectId = cityObjectImporter.doImport(boundarySurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information

		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + boundarySurface.getId() + "/");
				preparedStatement.setURL(++index, objectURL);
				preparedStatement.setURL(++index, objectURL);
				boundarySurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
				setBlankNode(preparedStatement, ++index);
			}
			boundarySurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			preparedStatement.setLong(++index, objectId);
		}
		// primary id
//		psThematicSurface.setLong(++index, objectId);

		// objectclass id
		preparedStatement.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				preparedStatement.setURL(++index, parentURL);
				setBlankNode(preparedStatement, ++index);
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setLong(++index, parentId);
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
				preparedStatement.setURL(++index, parentURL);
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setLong(++index, parentId);
				preparedStatement.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
				setBlankNode(preparedStatement, ++index);
				preparedStatement.setURL(++index, parentURL);
			} else {
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
				setBlankNode(preparedStatement, ++index);
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setNull(++index, Types.NULL);
				preparedStatement.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		index = importSurfaceGeometryProperty(new MultiSurfaceProperty[]{
				boundarySurface.getLod2MultiSurface(),
				boundarySurface.getLod3MultiSurface(),
				boundarySurface.getLod4MultiSurface()
		}, new int[]{2, 3, 4}, "_multi_surface_id", index);


		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (boundarySurface.isSetOpening()) {
			for (OpeningProperty property : boundarySurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, boundarySurface, objectId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								objectId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(boundarySurface, objectId, featureType);

		return objectId;
	}

}
