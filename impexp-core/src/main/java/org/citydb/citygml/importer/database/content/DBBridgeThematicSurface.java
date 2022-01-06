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
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.bridge.AbstractBoundarySurface;
import org.citygml4j.model.citygml.bridge.AbstractBridge;
import org.citygml4j.model.citygml.bridge.AbstractOpening;
import org.citygml4j.model.citygml.bridge.BridgeConstructionElement;
import org.citygml4j.model.citygml.bridge.BridgeInstallation;
import org.citygml4j.model.citygml.bridge.BridgeRoom;
import org.citygml4j.model.citygml.bridge.IntBridgeInstallation;
import org.citygml4j.model.citygml.bridge.OpeningProperty;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

public class DBBridgeThematicSurface extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBSurfaceGeometry surfaceGeometryImporter;
	private DBBridgeOpening openingImporter;

	public DBBridgeThematicSurface(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		openingImporter = importer.getImporter(DBBridgeOpening.class);
	}

	@Override
	protected String getTableName() {
		return TableEnum.BRIDGE_THEMATIC_SURFACE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "bridgethematicsurface/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".bridge_thematic_surface (id, objectclass_id, bridge_id, bridge_room_id, bridge_installation_id, bridge_constr_element_id, " +
				"lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?)";
	}

	protected long doImport(AbstractBoundarySurface boundarySurface) throws CityGMLImportException, SQLException {
		return doImport(boundarySurface, null, 0);
	}

	public long doImport(AbstractBoundarySurface boundarySurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(boundarySurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(boundarySurface, featureType);

		// import boundary surface information
		// primary id
		preparedStatement.setLong(1, boundarySurfaceId);

		// objectclass id
		preparedStatement.setInt(2, featureType.getObjectClassId());

		// parentId
		if (parent instanceof AbstractBridge) {
			preparedStatement.setLong(3, parentId);
			preparedStatement.setNull(4, Types.NULL);
			preparedStatement.setNull(5, Types.NULL);
			preparedStatement.setNull(6, Types.NULL);
		} else if (parent instanceof BridgeRoom) {
			preparedStatement.setNull(3, Types.NULL);
			preparedStatement.setLong(4, parentId);
			preparedStatement.setNull(5, Types.NULL);
			preparedStatement.setNull(6, Types.NULL);
		} else if (parent instanceof BridgeInstallation
				|| parent instanceof IntBridgeInstallation) {
			preparedStatement.setNull(3, Types.NULL);
			preparedStatement.setNull(4, Types.NULL);
			preparedStatement.setLong(5, parentId);
			preparedStatement.setNull(6, Types.NULL);
		} else if (parent instanceof BridgeConstructionElement) {
			preparedStatement.setNull(3, Types.NULL);
			preparedStatement.setNull(4, Types.NULL);
			preparedStatement.setNull(5, Types.NULL);
			preparedStatement.setLong(6, parentId);
		} else {
			preparedStatement.setNull(3, Types.NULL);
			preparedStatement.setNull(4, Types.NULL);
			preparedStatement.setNull(5, Types.NULL);
			preparedStatement.setNull(6, Types.NULL);
		}

		// brid:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				boundarySurface.getLod2MultiSurface(),
				boundarySurface.getLod3MultiSurface(),
				boundarySurface.getLod4MultiSurface()
		}, new int[]{2, 3, 4}, "_multi_surface_id", 7);

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.BRIDGE_THEMATIC_SURFACE);

		// brid:opening
		if (boundarySurface.isSetOpening()) {
			for (OpeningProperty property : boundarySurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, boundarySurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.BRIDGE_OPEN_TO_THEM_SRF.getName(),
								boundarySurfaceId,
								"BRIDGE_THEMATIC_SURFACE_ID",
								href,
								"BRIDGE_OPENING_ID"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(boundarySurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

}
