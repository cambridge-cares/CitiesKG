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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.citygml.common.database.xlink.DBXlinkBasic;
import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.citygml.importer.util.AttributeValueJoiner;
import org.citydb.config.Config;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.FeatureType;
import org.citygml4j.model.citygml.tunnel.AbstractBoundarySurface;
import org.citygml4j.model.citygml.tunnel.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.tunnel.HollowSpace;
import org.citygml4j.model.citygml.tunnel.IntTunnelInstallation;
import org.citygml4j.model.citygml.tunnel.IntTunnelInstallationProperty;
import org.citygml4j.model.citygml.tunnel.InteriorFurnitureProperty;
import org.citygml4j.model.citygml.tunnel.TunnelFurniture;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

public class DBTunnelHollowSpace extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBTunnelThematicSurface thematicSurfaceImporter;
	private DBTunnelFurniture tunnelFurnitureImporter;
	private DBTunnelInstallation tunnelInstallationImporter;
	private AttributeValueJoiner valueJoiner;
	
	private boolean hasObjectClassIdColumn;

	public DBTunnelHollowSpace(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBTunnelThematicSurface.class);
		tunnelFurnitureImporter = importer.getImporter(DBTunnelFurniture.class);
		tunnelInstallationImporter = importer.getImporter(DBTunnelInstallation.class);
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected String getTableName() {
		return TableEnum.TUNNEL_HOLLOW_SPACE.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "tunnelhollowspace/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + SQL_SCHEMA + ".tunnel_hollow_space (id, class, class_codespace, function, function_codespace, usage, usage_codespace, tunnel_id, " +
				"lod4_multi_surface_id, lod4_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	@Override
	protected String getSPARQLStatement() {
		return "NOT IMPLEMENTED.";
	}
	
	protected long doImport(HollowSpace hollowSpace) throws CityGMLImportException, SQLException {
		return doImport(hollowSpace, 0);
	}

	public long doImport(HollowSpace hollowSpace, long tunnelId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(hollowSpace);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long hollowSpaceId = cityObjectImporter.doImport(hollowSpace, featureType);

		// import hollow space information
		// primary id
		preparedStatement.setLong(1, hollowSpaceId);

		// tun:class
		if (hollowSpace.isSetClazz() && hollowSpace.getClazz().isSetValue()) {
			preparedStatement.setString(2, hollowSpace.getClazz().getValue());
			preparedStatement.setString(3, hollowSpace.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.VARCHAR);
		}

		// tun:function
		if (hollowSpace.isSetFunction()) {
			valueJoiner.join(hollowSpace.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(4, valueJoiner.result(0));
			preparedStatement.setString(5, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// tun:usage
		if (hollowSpace.isSetUsage()) {
			valueJoiner.join(hollowSpace.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// parent tunnel id
		if (tunnelId != 0)
			preparedStatement.setLong(8, tunnelId);
		else
			preparedStatement.setNull(8, Types.NULL);

		// tun:lod4MultiSurface
		importSurfaceGeometryProperty(hollowSpace.getLod4MultiSurface(), 4, "_multi_surface_id", 9);

		// tun:lod4Solid
		importSurfaceGeometryProperty(hollowSpace.getLod4Solid(), 4, "_solid_id", 10);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(11, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TUNNEL_HOLLOW_SPACE);

		// tun:boundedBy
		if (hollowSpace.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : hollowSpace.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, hollowSpace, hollowSpaceId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_THEMATIC_SURFACE.getName(),
								href,
								hollowSpaceId,
								"tunnel_hollow_space_id"));
					}
				}
			}
		}

		// tun:hollowSpaceInstallation
		if (hollowSpace.isSetHollowSpaceInstallation()) {
			for (IntTunnelInstallationProperty property : hollowSpace.getHollowSpaceInstallation()) {
				IntTunnelInstallation installation = property.getObject();

				if (installation != null) {
					tunnelInstallationImporter.doImport(installation, hollowSpace, hollowSpaceId);
					property.unsetIntTunnelInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_INSTALLATION.getName(),
								href,
								hollowSpaceId,
								"tunnel_hollow_space_id"));
					}
				}
			}
		}

		// tun:interiorFurniture
		if (hollowSpace.isSetInteriorFurniture()) {
			for (InteriorFurnitureProperty property : hollowSpace.getInteriorFurniture()) {
				TunnelFurniture furniture = property.getObject();

				if (furniture != null) {
					tunnelFurnitureImporter.doImport(furniture, hollowSpaceId);
					property.unsetTunnelFurniture();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_FURNITURE.getName(),
								href,
								hollowSpaceId,
								"tunnel_hollow_space_id"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(hollowSpace, hollowSpaceId, featureType);

		return hollowSpaceId;
	}

}
