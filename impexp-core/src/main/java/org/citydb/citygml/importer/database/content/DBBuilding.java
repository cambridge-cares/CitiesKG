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
import org.citydb.config.Config;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.util.CoreConstants;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.BuildingInstallation;
import org.citygml4j.model.citygml.building.BuildingInstallationProperty;
import org.citygml4j.model.citygml.building.BuildingPart;
import org.citygml4j.model.citygml.building.BuildingPartProperty;
import org.citygml4j.model.citygml.building.IntBuildingInstallation;
import org.citygml4j.model.citygml.building.IntBuildingInstallationProperty;
import org.citygml4j.model.citygml.building.InteriorRoomProperty;
import org.citygml4j.model.citygml.building.Room;
import org.citygml4j.model.citygml.core.Address;
import org.citygml4j.model.citygml.core.AddressProperty;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class DBBuilding extends AbstractDBImporter {

	private DBCityObject cityObjectImporter;
	private DBThematicSurface thematicSurfaceImporter;
	private DBBuildingInstallation buildingInstallationImporter;
	private DBRoom roomImporter;
	private DBAddress addressImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;

	public DBBuilding(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBThematicSurface.class);
		buildingInstallationImporter = importer.getImporter(DBBuildingInstallation.class);
		roomImporter = importer.getImporter(DBRoom.class);
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
		return TableEnum.BUILDING.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "building/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".building (id, building_parent_id, building_root_id, class, class_codespace, function, function_codespace, usage, usage_codespace, year_of_construction, year_of_demolition, " +
				"roof_type, roof_type_codespace, measured_height, measured_height_unit, storeys_above_ground, storeys_below_ground, storey_heights_above_ground, storey_heights_ag_unit, storey_heights_below_ground, storey_heights_bg_unit, " +
				"lod1_terrain_intersection, lod2_terrain_intersection, lod3_terrain_intersection, lod4_terrain_intersection, lod2_multi_curve, lod3_multi_curve, lod4_multi_curve, " +
				"lod0_footprint_id, lod0_roofprint_id, lod1_multi_surface_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id, " +
				"lod1_solid_id, lod2_solid_id, lod3_solid_id, lod4_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + "> " +  // add BASE by SYL
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
				"{ ? " + SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID + param +
				SchemaManagerAdapter.ONTO_BUILDING_ROOT_ID + param +
				SchemaManagerAdapter.ONTO_CLASS + param +
				SchemaManagerAdapter.ONTO_CLASS_CODESPACE + param +
				SchemaManagerAdapter.ONTO_FUNCTION + param +
				SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE + param +
				SchemaManagerAdapter.ONTO_USAGE + param +
				SchemaManagerAdapter.ONTO_USAGE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_YEAR_CONSTRUCTION + param +
				SchemaManagerAdapter.ONTO_YEAR_DEMOLITION + param +
				SchemaManagerAdapter.ONTO_ROOF_TYPE + param +
				SchemaManagerAdapter.ONTO_ROOF_TYPE_CODESPACE + param +
				SchemaManagerAdapter.ONTO_MEASURED_HEIGHT + param +
				SchemaManagerAdapter.ONTO_MEASURED_HEIGHT_UNIT + param +
				SchemaManagerAdapter.ONTO_STOREYS_ABOVE_GROUND + param +
				SchemaManagerAdapter.ONTO_STOREYS_BELLOW_GROUND + param +
				SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_ABOVE_GROUND + param +
				SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_AG_UNIT + param +
				SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_BELLOW_GROUND + param +
				SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_BG_UNIT + param +
				SchemaManagerAdapter.ONTO_LOD1_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD2_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD3_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD4_TERRAIN_INTERSECTION + param +
				SchemaManagerAdapter.ONTO_LOD2_MULTI_CURVE + param +
				SchemaManagerAdapter.ONTO_LOD3_MULTI_CURVE + param +
				SchemaManagerAdapter.ONTO_LOD4_MULTI_CURVE + param +
				SchemaManagerAdapter.ONTO_FOOTPRINT_ID + param +
				SchemaManagerAdapter.ONTO_ROOFPRINT_ID + param +
				SchemaManagerAdapter.ONTO_LOD1_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID + param +
				SchemaManagerAdapter.ONTO_LOD1_SOLID_ID + param +
				SchemaManagerAdapter.ONTO_LOD2_SOLID_ID + param +
				SchemaManagerAdapter.ONTO_LOD3_SOLID_ID + param +
				SchemaManagerAdapter.ONTO_LOD4_SOLID_ID + param +
				(hasObjectClassIdColumn ? SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param : "") +
				".}" +
				"}";

		return stmt;
	}

	protected long doImport(AbstractBuilding building) throws CityGMLImportException, SQLException {
		return doImport(building, 0, 0);
	}

	public long doImport(AbstractBuilding building, long parentId, long rootId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(building);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		objectId = cityObjectImporter.doImport(building, featureType);
		if (rootId == 0)
			rootId = objectId;

		int index = 0;
		URL objectURL = null;
		URL rootURL = null;
		URL parentURL = null;


		// import building information
		if (importer.isBlazegraph()) {
			try {
				String uuid = building.getId();
				if (uuid.isEmpty()) {
					uuid = importer.generateNewGmlId();
				}
				objectURL = new URL(iriGraphObject + uuid + "/");
			} catch (MalformedURLException e) {
				preparedStatement.setObject(++index, NodeFactory.createBlankNode());
			}
			preparedStatement.setURL(++index, objectURL);
			// primary id
			preparedStatement.setURL(++index, objectURL);
			building.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			if (building.isSetParent()) {
				// parent building id
				if (featureType.getObjectClassId() == 25) {
					parentURL = (URL) ((AbstractGML) ((BuildingPartProperty) building.getParent()).getParent())
							.getLocalProperty(CoreConstants.OBJECT_URIID);
				} else {
					parentURL = (URL) ((AbstractGML) building.getParent()).getLocalProperty(
							CoreConstants.OBJECT_URIID);
				}
				preparedStatement.setURL(++index, parentURL);
				building.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
			} else {
				setBlankNode(preparedStatement, ++index);
			}
			// root building id
			if (rootId == objectId) {
				rootURL = objectURL;
			} else if (rootId == parentId) {
				rootURL = parentURL;
			}
			preparedStatement.setURL(++index, rootURL);
			building.setLocalProperty(CoreConstants.OBJECT_ROOT_URIID, parentURL);
		} else {
			// import building information
			// primary id
			preparedStatement.setLong(++index, objectId);

			// parent building id
			if (parentId != 0) {
				preparedStatement.setLong(++index, parentId);
			} else {
				preparedStatement.setNull(++index, Types.NULL);
			}

			// root building id
			preparedStatement.setLong(++index, rootId);
		}

		// bldg:class
		if (building.isSetClazz() && building.getClazz().isSetValue()) {
			preparedStatement.setString(++index, building.getClazz().getValue());
			preparedStatement.setString(++index, building.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:function
		if (building.isSetFunction()) {
			valueJoiner.join(building.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			String codespace = valueJoiner.result(1);
			if (importer.isBlazegraph() &&  codespace == null) { // psBuilding setString for jenastatement requires non-Null and pgstatement doesn't require
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, codespace);
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:usage
		if (building.isSetUsage()) {
			valueJoiner.join(building.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:yearOfConstruction
		if (building.isSetYearOfConstruction()) {
			preparedStatement.setObject(++index, building.getYearOfConstruction());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.DATE);
		}

		// bldg:yearOfDemolition
		if (building.isSetYearOfDemolition()) {
			preparedStatement.setObject(++index, building.getYearOfDemolition());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.DATE);
		}

		// bldg:roofType
		if (building.isSetRoofType() && building.getRoofType().isSetValue()) {
			preparedStatement.setString(++index, building.getRoofType().getValue());
			if (building.getRoofType().getCodeSpace() == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, building.getRoofType().getCodeSpace());
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:measuredHeight
		if (building.isSetMeasuredHeight() && building.getMeasuredHeight().isSetValue()) {
			preparedStatement.setDouble(++index, building.getMeasuredHeight().getValue());
			if (building.getMeasuredHeight().getUom() == null && importer.isBlazegraph()) {
				setBlankNode(preparedStatement, ++index);
			} else {
				preparedStatement.setString(++index, building.getMeasuredHeight().getUom());
			}
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.DOUBLE);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:storeysAboveGround
		if (building.isSetStoreysAboveGround()) {
			preparedStatement.setInt(++index, building.getStoreysAboveGround());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.NULL);
		}

		// bldg:storeysBelowGround
		if (building.isSetStoreysBelowGround()) {
			preparedStatement.setInt(++index, building.getStoreysBelowGround());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.NULL);
		}

		// bldg:storeyHeightsAboveGround
		if (building.isSetStoreyHeightsAboveGround()) {
			valueJoiner.join(" ", building.getStoreyHeightsAboveGround().getDoubleOrNull(),
					v -> v.isSetDouble() ? v.getDouble().toString() : v.getNull().getValue());

			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, building.getStoreyHeightsAboveGround().getUom());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:storeyHeightsBelowGround
		if (building.isSetStoreyHeightsBelowGround()) {
			valueJoiner.join(" ", building.getStoreyHeightsBelowGround().getDoubleOrNull(),
					v -> v.isSetDouble() ? v.getDouble().toString() : v.getNull().getValue());

			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, building.getStoreyHeightsBelowGround().getUom());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		} else {
			preparedStatement.setNull(++index, Types.VARCHAR);
			preparedStatement.setNull(++index, Types.VARCHAR);
		}

		// bldg:lodXTerrainIntersectionCurve
		index = importGeometryObjectProperties(new MultiCurveProperty[]{
				building.getLod1TerrainIntersection(),
				building.getLod2TerrainIntersection(),
				building.getLod3TerrainIntersection(),
				building.getLod4TerrainIntersection()
		}, geometryConverter::getMultiCurve, index);

		// bldg:lodXMultiCurve
		index = importGeometryObjectProperties(new MultiCurveProperty[]{
				building.getLod2MultiCurve(),
				building.getLod3MultiCurve(),
				building.getLod4MultiCurve()
		}, geometryConverter::getMultiCurve, index);

		// bldg:lod0FootPrint and bldg:lod0RoofEdge
		index = importSurfaceGeometryProperty(building.getLod0FootPrint(), 0, "_footprint_id", index);
		index = importSurfaceGeometryProperty(building.getLod0RoofEdge(), 0, "_roofprint_id", index);

		// bldg:lodXMultiSurface
		index = importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				building.getLod1MultiSurface(),
				building.getLod2MultiSurface(),
				building.getLod3MultiSurface(),
				building.getLod4MultiSurface()
		}, new int[]{1, 2, 3, 4}, "_multi_surface_id", index);

		// bldg:lodXSolid
		index = importSurfaceGeometryProperties(new SolidProperty[]{
				building.getLod1Solid(),
				building.getLod2Solid(),
				building.getLod3Solid(),
				building.getLod4Solid()
		}, new int[]{1, 2, 3, 4}, "_solid_id", index);


		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(++index, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BUILDING);

		// bldg:boundedBy
		if (building.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : building.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, building, objectId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.THEMATIC_SURFACE.getName(),
								href,
								objectId,
								"building_id"));
					}
				}
			}
		}

		// bldg:outerBuildingInstallation
		if (building.isSetOuterBuildingInstallation()) {
			for (BuildingInstallationProperty property : building.getOuterBuildingInstallation()) {
				BuildingInstallation installation = property.getBuildingInstallation();

				if (installation != null) {
					buildingInstallationImporter.doImport(installation, building, objectId);
					property.unsetBuildingInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BUILDING_INSTALLATION.getName(),
								href,
								objectId,
								"building_id"));
					}
				}
			}
		}

		// bldg:interiorBuildingInstallation
		if (building.isSetInteriorBuildingInstallation()) {
			for (IntBuildingInstallationProperty property : building.getInteriorBuildingInstallation()) {
				IntBuildingInstallation installation = property.getIntBuildingInstallation();

				if (installation != null) {
					buildingInstallationImporter.doImport(installation, building, objectId);
					property.unsetIntBuildingInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BUILDING_INSTALLATION.getName(),
								href,
								objectId,
								"building_id"));
					}
				}
			}
		}

		// bldg:interiorRoom
		if (building.isSetInteriorRoom()) {
			for (InteriorRoomProperty property : building.getInteriorRoom()) {
				Room room = property.getRoom();

				if (room != null) {
					roomImporter.doImport(room, objectId);
					property.unsetRoom();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.ROOM.getName(),
								href,
								objectId,
								"building_id"));
					}
				}
			}
		}

		// bldg:consistsOfBuildingPart
		if (building.isSetConsistsOfBuildingPart()) {
			for (BuildingPartProperty property : building.getConsistsOfBuildingPart()) {
				BuildingPart buildingPart = property.getBuildingPart();

				if (buildingPart != null) {
					doImport(buildingPart, objectId, rootId);
					property.unsetBuildingPart();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0)
						importer.logOrThrowUnsupportedXLinkMessage(building, BuildingPart.class, href);
				}
			}
		}

		// bldg:address
		if (building.isSetAddress()) {
			for (AddressProperty property : building.getAddress()) {
				Address address = property.getAddress();

				if (address != null) {
					addressImporter.importBuildingAddress(address, objectId);
					property.unsetAddress();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.ADDRESS_TO_BUILDING.getName(),
								objectId,
								"BUILDING_ID",
								href,
								"ADDRESS_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(building, objectId, featureType);

		return objectId;
	}

}
