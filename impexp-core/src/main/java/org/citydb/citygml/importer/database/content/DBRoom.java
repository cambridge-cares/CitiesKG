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
import java.sql.SQLException;
import java.sql.Types;

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
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.BuildingFurniture;
import org.citygml4j.model.citygml.building.IntBuildingInstallation;
import org.citygml4j.model.citygml.building.IntBuildingInstallationProperty;
import org.citygml4j.model.citygml.building.InteriorFurnitureProperty;
import org.citygml4j.model.citygml.building.Room;
import org.citygml4j.model.gml.basicTypes.Code;

public class DBRoom extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBThematicSurface thematicSurfaceImporter;
	private DBBuildingFurniture buildingFurnitureImporter;
	private DBBuildingInstallation buildingInstallationImporter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;

	private boolean affineTransformation;

	public DBRoom(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		affineTransformation = config.getProject().getImporter().getAffineTransformation().isEnabled();
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBThematicSurface.class);
		buildingFurnitureImporter = importer.getImporter(DBBuildingFurniture.class);
		buildingInstallationImporter = importer.getImporter(DBBuildingInstallation.class);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}

	@Override
	protected String getTableName() {
		return TableEnum.ROOM.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "room/";
	}

	@Override
	protected String getSQLStatement() {
		return  "insert into " + sqlSchema + ".room (id, objectclass_id, class, class_codespace, function, function_codespace, usage, usage_codespace, " +
				"building_id, lod4_multi_surface_id, lod4_solid_id)" +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}

	@Override
	protected String getSPARQLStatement() {
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + prefixOntoCityGML + "> " +
				"BASE <" + iriGraphBase + "> " +  // add BASE by SYL
				"INSERT DATA" +
				" { GRAPH <" + iriGraphObjectRel + "> " +
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
				objectURL = new URL(iriGraphObject + uuid + "/");
			} catch (MalformedURLException e) {
				setBlankNode(preparedStatement, ++index);
			}
			preparedStatement.setURL(++index, objectURL);
			// primary id
			preparedStatement.setURL(++index, objectURL);
			room.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
		} else {
			// primary id
			preparedStatement.setLong(++index, roomId);
		}

		// bldg:class
		if (room.isSetClazz() && room.getClazz().isSetValue()) {
			preparedStatement.setString(++index, room.getClazz().getValue());
			preparedStatement.setString(++index, room.getClazz().getCodeSpace());
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// bldg:function
		if (room.isSetFunction()) {
			valueJoiner.join(room.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// bldg:usage
		if (room.isSetUsage()) {
			valueJoiner.join(room.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(++index, valueJoiner.result(0));
			preparedStatement.setString(++index, valueJoiner.result(1));
		} else if (importer.isBlazegraph()) {
			setBlankNode(preparedStatement, ++index);
			setBlankNode(preparedStatement, ++index);
		}else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// parent building id
		if (buildingId != 0)
			preparedStatement.setLong(++index, buildingId);
		else if (importer.isBlazegraph())
			setBlankNode(preparedStatement, ++index);
		else
			preparedStatement.setNull(8, Types.NULL);

		// bldg:lod4MultiSurface
		importSurfaceGeometryProperty(room.getLod4MultiSurface(), 4, "_multi_surface_id", 9);

		// bldg:lod4Solid
		importSurfaceGeometryProperty(room.getLod4Solid(), 4, "_solid_id", 10);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(++index, featureType.getObjectClassId());

		preparedStatement.addBatch();
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

}
