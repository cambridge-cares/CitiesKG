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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.bridge.AbstractBoundarySurface;
import org.citygml4j.model.citygml.bridge.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.bridge.BridgeFurniture;
import org.citygml4j.model.citygml.bridge.BridgeRoom;
import org.citygml4j.model.citygml.bridge.IntBridgeInstallation;
import org.citygml4j.model.citygml.bridge.IntBridgeInstallationProperty;
import org.citygml4j.model.citygml.bridge.InteriorFurnitureProperty;
import org.citygml4j.model.gml.basicTypes.Code;

public class DBBridgeRoom extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBBridgeThematicSurface thematicSurfaceImporter;
	private DBBridgeFurniture bridgeFurnitureImporter;
	private DBBridgeInstallation bridgeInstallationImporter;
	private AttributeValueJoiner valueJoiner;
	private boolean hasObjectClassIdColumn;

	public DBBridgeRoom(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBBridgeThematicSurface.class);
		bridgeFurnitureImporter = importer.getImporter(DBBridgeFurniture.class);
		bridgeInstallationImporter = importer.getImporter(DBBridgeInstallation.class);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}
	
	@Override
	protected String getTableName() {
		return TableEnum.BRIDGE_ROOM.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "bridgeroom/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".bridge_room (id, class, class_codespace, function, function_codespace, usage, usage_codespace, bridge_id, " +
				"lod4_multi_surface_id, lod4_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	protected long doImport(BridgeRoom bridgeRoom) throws CityGMLImportException, SQLException {
		return doImport(bridgeRoom, 0);
	}

	public long doImport(BridgeRoom bridgeRoom, long bridgeId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(bridgeRoom);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long bridgeRoomId = cityObjectImporter.doImport(bridgeRoom, featureType);

		// import bridge room information
		// primary id
		preparedStatement.setLong(1, bridgeRoomId);

		// brid:class
		if (bridgeRoom.isSetClazz() && bridgeRoom.getClazz().isSetValue()) {
			preparedStatement.setString(2, bridgeRoom.getClazz().getValue());
			preparedStatement.setString(3, bridgeRoom.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// brid:function
		if (bridgeRoom.isSetFunction()) {
			valueJoiner.join(bridgeRoom.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(4, valueJoiner.result(0));
			preparedStatement.setString(5, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// brid:usage
		if (bridgeRoom.isSetUsage()) {
			valueJoiner.join(bridgeRoom.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// parent bridge id
		if (bridgeId != 0)
			preparedStatement.setLong(8, bridgeId);
		else
			preparedStatement.setNull(8, Types.NULL);

		// brid:lod4MultiSurface
		importSurfaceGeometryProperty(bridgeRoom.getLod4MultiSurface(), 4, "_multi_surface_id", 9);

		// brid:lod4Solid
		importSurfaceGeometryProperty(bridgeRoom.getLod4Solid(), 4, "_solid_id", 10);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(11, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BRIDGE_ROOM);

		// brid:boundedBy
		if (bridgeRoom.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : bridgeRoom.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, bridgeRoom, bridgeRoomId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_THEMATIC_SURFACE.getName(),
								href,
								bridgeRoomId,
								"bridge_room_id"));
					}
				}
			}
		}

		// brid:bridgeRoomInstallation
		if (bridgeRoom.isSetBridgeRoomInstallation()) {
			for (IntBridgeInstallationProperty property : bridgeRoom.getBridgeRoomInstallation()) {
				IntBridgeInstallation installation = property.getIntBridgeInstallation();

				if (installation != null) {
					bridgeInstallationImporter.doImport(installation, bridgeRoom, bridgeRoomId);
					property.unsetIntBridgeInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_INSTALLATION.getName(),
								href,
								bridgeRoomId,
								"bridge_room_id"));
					}
				}
			}
		}

		// brid:interiorFurniture
		if (bridgeRoom.isSetInteriorFurniture()) {
			for (InteriorFurnitureProperty property : bridgeRoom.getInteriorFurniture()) {
				BridgeFurniture furniture = property.getBridgeFurniture();

				if (furniture != null) {
					bridgeFurnitureImporter.doImport(furniture, bridgeRoomId);
					property.unsetBridgeFurniture();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_FURNITURE.getName(),
								href,
								bridgeRoomId,
								"bridge_room_id"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(bridgeRoom, bridgeRoomId, featureType);
		
		return bridgeRoomId;
	}

}
