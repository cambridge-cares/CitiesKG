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
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.bridge.AbstractBoundarySurface;
import org.citygml4j.model.citygml.bridge.AbstractBridge;
import org.citygml4j.model.citygml.bridge.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.bridge.BridgeConstructionElement;
import org.citygml4j.model.citygml.bridge.BridgeConstructionElementProperty;
import org.citygml4j.model.citygml.bridge.BridgeInstallation;
import org.citygml4j.model.citygml.bridge.BridgeInstallationProperty;
import org.citygml4j.model.citygml.bridge.BridgePart;
import org.citygml4j.model.citygml.bridge.BridgePartProperty;
import org.citygml4j.model.citygml.bridge.BridgeRoom;
import org.citygml4j.model.citygml.bridge.IntBridgeInstallation;
import org.citygml4j.model.citygml.bridge.IntBridgeInstallationProperty;
import org.citygml4j.model.citygml.bridge.InteriorBridgeRoomProperty;
import org.citygml4j.model.citygml.core.Address;
import org.citygml4j.model.citygml.core.AddressProperty;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class DBBridge extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBBridgeThematicSurface thematicSurfaceImporter;
	private DBBridgeConstrElement bridgeConstructionImporter;
	private DBBridgeInstallation bridgeInstallationImporter;
	private DBBridgeRoom roomImporter;
	private DBAddress addressImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;

	public DBBridge(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		thematicSurfaceImporter = importer.getImporter(DBBridgeThematicSurface.class);
		bridgeConstructionImporter = importer.getImporter(DBBridgeConstrElement.class);
		bridgeInstallationImporter = importer.getImporter(DBBridgeInstallation.class);
		roomImporter = importer.getImporter(DBBridgeRoom.class);
		addressImporter = importer.getImporter(DBAddress.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}

	@Override
	protected String getTableName() {
		return TableEnum.BRIDGE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "bridge/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".bridge (id, bridge_parent_id, bridge_root_id, class, class_codespace, function, function_codespace, usage, usage_codespace, year_of_construction, year_of_demolition, is_movable, " +
				"lod1_terrain_intersection, lod2_terrain_intersection, lod3_terrain_intersection, lod4_terrain_intersection, lod2_multi_curve, lod3_multi_curve, lod4_multi_curve, " +
				"lod1_multi_surface_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id, " +
				"lod1_solid_id, lod2_solid_id, lod3_solid_id, lod4_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		return "NOT IMPLEMENTED.";
	}

	protected long doImport(AbstractBridge bridge) throws CityGMLImportException, SQLException {
		return doImport(bridge, 0, 0);
	}

	public long doImport(AbstractBridge bridge, long parentId, long rootId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(bridge);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long bridgeId = cityObjectImporter.doImport(bridge, featureType);
		if (rootId == 0)
			rootId = bridgeId;

		int index = 0;

		// import bridge information
		// primary id
		preparedStatement.setLong(++index, bridgeId);

		// parent bridge id
		if (parentId != 0)
			preparedStatement.setLong(++index, parentId);
		else
			preparedStatement.setNull(++index, Types.NULL);

		// root building id
		preparedStatement.setLong(++index, rootId);

		// brid:class
		if (bridge.isSetClazz() && bridge.getClazz().isSetValue()) {
			preparedStatement.setString(++index, bridge.getClazz().getValue());
			preparedStatement.setString(++index, bridge.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// brid:function
		if (bridge.isSetFunction()) {
			valueJoiner.join(bridge.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// brid:usage
		if (bridge.isSetUsage()) {
			valueJoiner.join(bridge.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// brid:yearOfConstruction
		if (bridge.isSetYearOfConstruction()) {
			preparedStatement.setObject(++index, bridge.getYearOfConstruction());
		} else {
			preparedStatement.setNull(++index, Types.DATE);
		}

		// brid:yearOfDemolition
		if (bridge.isSetYearOfDemolition()) {
			preparedStatement.setObject(++index, bridge.getYearOfDemolition());
		} else {
			preparedStatement.setNull(11, Types.DATE);
		}

		// brid:isMovable
		if (bridge.isSetIsMovable())
			preparedStatement.setInt(++index, bridge.getIsMovable() ? 1 : 0);
		else
			preparedStatement.setNull(++index, Types.NULL);

		// brid:lodXTerrainIntersectionCurve
		index = importGeometryObjectProperties(new MultiCurveProperty[]{
				bridge.getLod1TerrainIntersection(),
				bridge.getLod2TerrainIntersection(),
				bridge.getLod3TerrainIntersection(),
				bridge.getLod4TerrainIntersection()
		}, geometryConverter::getMultiCurve, index);

		// brid:lodXMultiCurve
		index = importGeometryObjectProperties(new MultiCurveProperty[]{
				bridge.getLod2MultiCurve(),
				bridge.getLod3MultiCurve(),
				bridge.getLod4MultiCurve()
		}, geometryConverter::getMultiCurve, index);

		// brid:lodXMultiSurface
		index = importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				bridge.getLod1MultiSurface(),
				bridge.getLod2MultiSurface(),
				bridge.getLod3MultiSurface(),
				bridge.getLod4MultiSurface()
		}, new int[]{1, 2, 3, 4}, "_multi_surface_id", index);

		// brid:lodXSolid
		index = importSurfaceGeometryProperties(new SolidProperty[]{
				bridge.getLod1Solid(),
				bridge.getLod2Solid(),
				bridge.getLod3Solid(),
				bridge.getLod4Solid()
		}, new int[]{1, 2, 3, 4}, "_solid_id", index);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(++index, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BRIDGE);

		// brid:boundedBy
		if (bridge.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : bridge.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, bridge, bridgeId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_THEMATIC_SURFACE.getName(),
								href,
								bridgeId,
								"bridge_id"));
					}
				}
			}
		}

		// brid:outerBridgeConstructionElement
		if (bridge.isSetOuterBridgeConstructionElement()) {
			for (BridgeConstructionElementProperty property : bridge.getOuterBridgeConstructionElement()) {
				BridgeConstructionElement construction = property.getBridgeConstructionElement();

				if (construction != null) {
					bridgeConstructionImporter.doImport(construction, bridge, bridgeId);
					property.unsetBridgeConstructionElement();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_CONSTR_ELEMENT.getName(),
								href,
								bridgeId,
								"bridge_id"));
					}
				}
			}
		}

		// bridg:outerBridgeInstallation
		if (bridge.isSetOuterBridgeInstallation()) {
			for (BridgeInstallationProperty property : bridge.getOuterBridgeInstallation()) {
				BridgeInstallation installation = property.getBridgeInstallation();

				if (installation != null) {
					bridgeInstallationImporter.doImport(installation, bridge, bridgeId);
					property.unsetBridgeInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_INSTALLATION.getName(),
								href,
								bridgeId,
								"bridge_id"));
					}
				}
			}
		}

		// brid:interiorBridgeInstallation
		if (bridge.isSetInteriorBridgeInstallation()) {
			for (IntBridgeInstallationProperty property : bridge.getInteriorBridgeInstallation()) {
				IntBridgeInstallation installation = property.getIntBridgeInstallation();

				if (installation != null) {
					bridgeInstallationImporter.doImport(installation, bridge, bridgeId);
					property.unsetIntBridgeInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_INSTALLATION.getName(),
								href,
								bridgeId,
								"bridge_id"));
					}
				}
			}
		}

		// brid:interiorBridgeRoom
		if (bridge.isSetInteriorBridgeRoom()) {
			for (InteriorBridgeRoomProperty property : bridge.getInteriorBridgeRoom()) {
				BridgeRoom room = property.getBridgeRoom();

				if (room != null) {
					roomImporter.doImport(room, bridgeId);
					property.unsetBridgeRoom();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_ROOM.getName(),
								href,
								bridgeId,
								"bridge_id"));
					}
				}
			}
		}

		// brid:consistsOfBridgePart
		if (bridge.isSetConsistsOfBridgePart()) {
			for (BridgePartProperty property : bridge.getConsistsOfBridgePart()) {
				BridgePart bridgePart = property.getBridgePart();

				if (bridgePart != null) {
					doImport(bridgePart, bridgeId, rootId);
					property.unsetBridgePart();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0)
						importer.logOrThrowUnsupportedXLinkMessage(bridge, BridgePart.class, href);
				}
			}
		}

		// bridg:address
		if (bridge.isSetAddress()) {
			for (AddressProperty property : bridge.getAddress()) {
				Address address = property.getAddress();

				if (address != null) {
					addressImporter.importBridgeAddress(address, bridgeId);
					property.unsetAddress();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.ADDRESS_TO_BRIDGE.getName(),
								bridgeId,
								"BRIDGE_ID",
								href,
								"ADDRESS_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(bridge, bridgeId, featureType);

		return bridgeId;
	}

}
