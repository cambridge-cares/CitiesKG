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
import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.BuildingFurniture;
import org.citygml4j.model.citygml.building.IntBuildingInstallation;
import org.citygml4j.model.citygml.building.IntBuildingInstallationProperty;
import org.citygml4j.model.citygml.building.InteriorFurnitureProperty;
import org.citygml4j.model.citygml.building.Room;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

public class DBRoom implements DBImporter {
	private final CityGMLImportManager importer;
	private final Connection batchConn;

	private PreparedStatement psRoom;
	private DBCityObject cityObjectImporter;
	private DBSurfaceGeometry surfaceGeometryImporter;
	private DBThematicSurface thematicSurfaceImporter;
	private DBBuildingFurniture buildingFurnitureImporter;
	private DBBuildingInstallation buildingInstallationImporter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;
	private int batchCounter;

	private boolean affineTransformation;
	private int nullGeometryType;
	private String nullGeometryTypeName;

	private String PREFIX_ONTOCITYGML;
	private String IRI_GRAPH_BASE;
	private String IRI_GRAPH_OBJECT;
	private static final String IRI_GRAPH_OBJECT_REL = "room/";

	public DBRoom(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		this.importer = importer;
		this.batchConn = batchConn;

		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		nullGeometryType = importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryType();
		nullGeometryTypeName = importer.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName();
		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;

		String stmt = "insert into " + schema + ".room (id, objectclass_id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"building_id, lod4_multi_surface_id, lod4_solid_id)" +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		// Modification for SPARQL
		if (importer.isBlazegraph()) {
			PREFIX_ONTOCITYGML = importer.getOntoCityGmlPrefix();
			IRI_GRAPH_BASE = importer.getGraphBaseIri();
			IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
			stmt = getSPARQLStatement();
		}

		psRoom = batchConn.prepareStatement(stmt);

		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBThematicSurface.class);
		buildingFurnitureImporter = importer.getImporter(DBBuildingFurniture.class);
		buildingInstallationImporter = importer.getImporter(DBBuildingInstallation.class);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	private String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +  // add BASE by SYL
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? " + SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_CLASS + param +
				SchemaManagerAdapter.ONTO_CLASS_CODESPACE + param +
				SchemaManagerAdapter.ONTO_FUNCTION + param +
				SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE + param +
				SchemaManagerAdapter.ONTO_USAGE + param +
				SchemaManagerAdapter.ONTO_USAGE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_BUILDING_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_SOLID_ID + param +
				(hasObjectClassIdColumn ? SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param : "") +
				".}" +
				"}";

		return stmt;
	}

	protected long doImport(Room room) throws CityGMLImportException, SQLException {
		return doImport(room, 0);
	}

	public long doImport(Room room, long buildingId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(room);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long roomId = cityObjectImporter.doImport(room, featureType);

		URL objectURL = null;
		int index = 0;

		// import room information
		if (importer.isBlazegraph()) {
			try {
				String uuid = room.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(IRI_GRAPH_OBJECT + uuid + "/");
			} catch (MalformedURLException e) {
				psRoom.setObject(++index, NodeFactory.createBlankNode());
			}
			psRoom.setURL(++index, objectURL);
			// primary id
			psRoom.setURL(++index, objectURL);
			room.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			// primary id
			psRoom.setLong(++index, roomId);
		}

		// bldg:class
		if (room.isSetClazz() && room.getClazz().isSetValue()) {
			psRoom.setString(++index, room.getClazz().getValue());
			psRoom.setString(++index, room.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(psRoom, ++index);
			setBlankNode(psRoom, ++index);
		}else {
			psRoom.setNull(2, Types.VARCHAR);
			psRoom.setNull(3, Types.VARCHAR);
		}

		// bldg:function
		if (room.isSetFunction()) {
			valueJoiner.join(room.getFunction(), Code::getValue, Code::getCodeSpace);
			psRoom.setString(++index, valueJoiner.result(0));
			psRoom.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(psRoom, ++index);
			setBlankNode(psRoom, ++index);
		}else {
			psRoom.setNull(4, Types.VARCHAR);
			psRoom.setNull(5, Types.VARCHAR);
		}

		// bldg:usage
		if (room.isSetUsage()) {
			valueJoiner.join(room.getUsage(), Code::getValue, Code::getCodeSpace);
			psRoom.setString(++index, valueJoiner.result(0));
			psRoom.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(psRoom, ++index);
			setBlankNode(psRoom, ++index);
		}else {
			psRoom.setNull(6, Types.VARCHAR);
			psRoom.setNull(7, Types.VARCHAR);
		}

		// parent building id
		if (buildingId != 0)
			psRoom.setLong(++index, buildingId);
		else if (importer.isBlazegraph())
			setBlankNode(psRoom, ++index);
		else
			psRoom.setNull(8, Types.NULL);

		// bldg:lod4MultiSurface
		long geometryId = 0;
		if (room.isSetLod4MultiSurface()) {
			MultiSurfaceProperty multiSurfacePropery = room.getLod4MultiSurface();

			if (multiSurfacePropery.isSetMultiSurface()) {
				geometryId = surfaceGeometryImporter.doImport(multiSurfacePropery.getMultiSurface(), roomId);
				multiSurfacePropery.unsetMultiSurface();
			} else {
				String href = multiSurfacePropery.getHref();
				if (href != null && href.length() != 0) {
					importer.propagateXlink(new DBXlinkSurfaceGeometry(
							TableEnum.ROOM.getName(),
							roomId, 
							href, 
							"lod4_multi_surface_id"));
				}
			}
		} 

		if (geometryId != 0)
			psRoom.setLong(++index, geometryId);
		else if (importer.isBlazegraph())
			setBlankNode(psRoom, ++index);
		else
			psRoom.setNull(9, Types.NULL);

		// bldg:lod4Solid
		geometryId = 0;
		if (room.isSetLod4Solid()) {
			SolidProperty solidProperty = room.getLod4Solid();

			if (solidProperty.isSetSolid()) {
				geometryId = surfaceGeometryImporter.doImport(solidProperty.getSolid(), roomId);
				solidProperty.unsetSolid();
			} else {
				String href = solidProperty.getHref();
				if (href != null && href.length() != 0) {
					importer.propagateXlink(new DBXlinkSurfaceGeometry(
							TableEnum.ROOM.getName(),
							roomId,
							href, 
							"lod4_solid_id"));
				}
			}
		} 

		if (geometryId != 0)
			psRoom.setLong(++index, geometryId);
		else if (importer.isBlazegraph())
			setBlankNode(psRoom, ++index);
		else
			psRoom.setNull(10, Types.NULL);

		// objectclass id
		if (hasObjectClassIdColumn)
			psRoom.setLong(11, featureType.getObjectClassId());

		psRoom.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.ROOM);

		// bldg:boundedBy
		if (room.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : room.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, room, roomId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.THEMATIC_SURFACE.getName(),
								href,
								roomId,
								"room_id"));
					}
				}
			}
		}

		// bldg:roomInstallation
		if (room.isSetRoomInstallation()) {
			for (IntBuildingInstallationProperty property : room.getRoomInstallation()) {
				IntBuildingInstallation installation = property.getIntBuildingInstallation();

				if (installation != null) {
					buildingInstallationImporter.doImport(installation, room, roomId);
					property.unsetIntBuildingInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BUILDING_INSTALLATION.getName(),
								href,
								roomId,
								"room_id"));
					}
				}
			}
		}

		// bldg:interiorFurniture
		if (room.isSetInteriorFurniture()) {
			for (InteriorFurnitureProperty property : room.getInteriorFurniture()) {
				BuildingFurniture furniture = property.getBuildingFurniture();

				if (furniture != null) {
					buildingFurnitureImporter.doImport(furniture, roomId);
					property.unsetBuildingFurniture();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BUILDING_INSTALLATION.getName(),
								href,
								roomId,
								"room_id"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(room, roomId, featureType);

		return roomId;
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psRoom.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psRoom.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}

}
