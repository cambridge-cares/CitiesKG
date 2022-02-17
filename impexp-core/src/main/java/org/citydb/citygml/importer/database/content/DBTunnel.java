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
import org.citygml4j.model.citygml.tunnel.AbstractBoundarySurface;
import org.citygml4j.model.citygml.tunnel.AbstractTunnel;
import org.citygml4j.model.citygml.tunnel.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.tunnel.HollowSpace;
import org.citygml4j.model.citygml.tunnel.IntTunnelInstallation;
import org.citygml4j.model.citygml.tunnel.IntTunnelInstallationProperty;
import org.citygml4j.model.citygml.tunnel.InteriorHollowSpaceProperty;
import org.citygml4j.model.citygml.tunnel.TunnelInstallation;
import org.citygml4j.model.citygml.tunnel.TunnelInstallationProperty;
import org.citygml4j.model.citygml.tunnel.TunnelPart;
import org.citygml4j.model.citygml.tunnel.TunnelPartProperty;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class DBTunnel extends AbstractDBImporter {
	private DBCityObject cityObjectImporter;
	private DBTunnelThematicSurface thematicSurfaceImporter;
	private DBTunnelInstallation tunnelInstallationImporter;
	private DBTunnelHollowSpace hollowSpaceImporter;
	private GeometryConverter geometryConverter;
	private AttributeValueJoiner valueJoiner;

	private boolean hasObjectClassIdColumn;

	public DBTunnel(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		super(batchConn, config, importer);
		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		thematicSurfaceImporter = importer.getImporter(DBTunnelThematicSurface.class);
		tunnelInstallationImporter = importer.getImporter(DBTunnelInstallation.class);
		hollowSpaceImporter = importer.getImporter(DBTunnelHollowSpace.class);
		geometryConverter = importer.getGeometryConverter();
		valueJoiner = importer.getAttributeValueJoiner();
	}

	@Override
	protected void preconstructor(Config config) {
		hasObjectClassIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(4, 0, 0) >= 0;
	}

	@Override
	protected String getTableName() {
		return TableEnum.TUNNEL.getName();
	}

	@Override
	protected String getIriGraphObjectRel() {
		return "tunnel/";
	}

	@Override
	protected String getSQLStatement() {
		return "insert into " + sqlSchema + ".tunnel (id, tunnel_parent_id, tunnel_root_id, class, class_codespace, function, function_codespace, usage, usage_codespace, year_of_construction, year_of_demolition, " +
				"lod1_terrain_intersection, lod2_terrain_intersection, lod3_terrain_intersection, lod4_terrain_intersection, lod2_multi_curve, lod3_multi_curve, lod4_multi_curve, " +
				"lod1_multi_surface_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id, " +
				"lod1_solid_id, lod2_solid_id, lod3_solid_id, lod4_solid_id" +
				(hasObjectClassIdColumn ? ", objectclass_id) " : ") ") +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
				(hasObjectClassIdColumn ? ", ?)" : ")");
	}

	protected long doImport(AbstractTunnel tunnel) throws CityGMLImportException, SQLException {
		return doImport(tunnel, 0, 0);
	}

	public long doImport(AbstractTunnel tunnel, long parentId, long rootId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(tunnel);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long tunnelId = cityObjectImporter.doImport(tunnel, featureType);
		if (rootId == 0)
			rootId = tunnelId;

		// import tunnel information
		// primary id
		preparedStatement.setLong(1, tunnelId);

		// parent tunnel id
		if (parentId != 0)
			preparedStatement.setLong(2, parentId);
		else
			preparedStatement.setNull(2, Types.NULL);

		// root tunnel id
		preparedStatement.setLong(3, rootId);

		// tun:class
		if (tunnel.isSetClazz() && tunnel.getClazz().isSetValue()) {
			preparedStatement.setString(4, tunnel.getClazz().getValue());
			preparedStatement.setString(5, tunnel.getClazz().getCodeSpace());
		} else {
			preparedStatement.setNull(4, Types.VARCHAR);
			preparedStatement.setNull(5, Types.VARCHAR);
		}

		// tun:function
		if (tunnel.isSetFunction()) {
			valueJoiner.join(tunnel.getFunction(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(6, valueJoiner.result(0));
			preparedStatement.setString(7, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(6, Types.VARCHAR);
			preparedStatement.setNull(7, Types.VARCHAR);
		}

		// tun:usage
		if (tunnel.isSetUsage()) {
			valueJoiner.join(tunnel.getUsage(), Code::getValue, Code::getCodeSpace);
			preparedStatement.setString(8, valueJoiner.result(0));
			preparedStatement.setString(9, valueJoiner.result(1));
		} else {
			preparedStatement.setNull(8, Types.VARCHAR);
			preparedStatement.setNull(9, Types.VARCHAR);
		}

		// tun:yearOfConstruction
		if (tunnel.isSetYearOfConstruction()) {
			preparedStatement.setObject(10, tunnel.getYearOfConstruction());
		} else {
			preparedStatement.setNull(10, Types.DATE);
		}

		// tun:yearOfDemolition
		if (tunnel.isSetYearOfDemolition()) {
			preparedStatement.setObject(11, tunnel.getYearOfDemolition());
		} else {
			preparedStatement.setNull(11, Types.DATE);
		}

		// tun:lodXTerrainIntersectionCurve
		importGeometryObjectProperties(new MultiCurveProperty[]{
				tunnel.getLod1TerrainIntersection(),
				tunnel.getLod2TerrainIntersection(),
				tunnel.getLod3TerrainIntersection(),
				tunnel.getLod4TerrainIntersection()
		}, geometryConverter::getMultiCurve, 12);

		// tun:lodXMultiCurve
		importGeometryObjectProperties(new MultiCurveProperty[]{
				tunnel.getLod2MultiCurve(),
				tunnel.getLod3MultiCurve(),
				tunnel.getLod4MultiCurve()
		}, geometryConverter::getMultiCurve, 16);

		// tun:lodXMultiSurface
		importSurfaceGeometryProperties(new MultiSurfaceProperty[]{
				tunnel.getLod1MultiSurface(),
				tunnel.getLod2MultiSurface(),
				tunnel.getLod3MultiSurface(),
				tunnel.getLod4MultiSurface()
		}, new int[]{1, 2, 3, 4}, "_multi_surface_id", 19);

		// tun:lodXSolid
		importSurfaceGeometryProperties(new SolidProperty[]{
				tunnel.getLod1Solid(),
				tunnel.getLod2Solid(),
				tunnel.getLod3Solid(),
				tunnel.getLod4Solid()
		}, new int[]{1, 2, 3, 4}, "_solid_id", 23);

		// objectclass id
		if (hasObjectClassIdColumn)
			preparedStatement.setLong(27, featureType.getObjectClassId());

		preparedStatement.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.TUNNEL);

		// tun:boundedBy
		if (tunnel.isSetBoundedBySurface()) {
			for (BoundarySurfaceProperty property : tunnel.getBoundedBySurface()) {
				AbstractBoundarySurface boundarySurface = property.getBoundarySurface();

				if (boundarySurface != null) {
					thematicSurfaceImporter.doImport(boundarySurface, tunnel, tunnelId);
					property.unsetBoundarySurface();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_THEMATIC_SURFACE.getName(),
								href,
								tunnelId,
								"tunnel_id"));
					}
				}
			}
		}

		// tun:outerTunnelInstallation
		if (tunnel.isSetOuterTunnelInstallation()) {
			for (TunnelInstallationProperty property : tunnel.getOuterTunnelInstallation()) {
				TunnelInstallation installation = property.getTunnelInstallation();

				if (installation != null) {
					tunnelInstallationImporter.doImport(installation, tunnel, tunnelId);
					property.unsetTunnelInstallation();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_INSTALLATION.getName(),
								href,
								tunnelId,
								"tunnel_id"));
					}
				}
			}
		}

		// tun:interiorTunnelInstallation
		if (tunnel.isSetInteriorTunnelInstallation()) {
			for (IntTunnelInstallationProperty property : tunnel.getInteriorTunnelInstallation()) {
				IntTunnelInstallation installation = property.getIntTunnelInstallation();

				if (installation != null) {
					tunnelInstallationImporter.doImport(installation, tunnel, tunnelId);
					property.unsetIntTunnelInstallation();
				} else {
					// xlink
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_INSTALLATION.getName(),
								href,
								tunnelId,
								"tunnel_id"));
					}
				}
			}
		}

		// tun:interiorHollowSpace
		if (tunnel.isSetInteriorHollowSpace()) {
			for (InteriorHollowSpaceProperty property : tunnel.getInteriorHollowSpace()) {
				HollowSpace hollowSpace = property.getHollowSpace();

				if (hollowSpace != null) {
					hollowSpaceImporter.doImport(hollowSpace, tunnelId);
					property.unsetHollowSpace();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.TUNNEL_HOLLOW_SPACE.getName(),
								href,
								tunnelId,
								"tunnel_id"));
					}
				}
			}
		}

		// tun:consistsOfTunnelPart
		if (tunnel.isSetConsistsOfTunnelPart()) {
			for (TunnelPartProperty property : tunnel.getConsistsOfTunnelPart()) {
				TunnelPart tunnelPart = property.getTunnelPart();

				if (tunnelPart != null) {
					doImport(tunnelPart, tunnelId, rootId);
					property.unsetTunnelPart();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0)
						importer.logOrThrowUnsupportedXLinkMessage(tunnel, TunnelPart.class, href);
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(tunnel, tunnelId, featureType);

		return tunnelId;
	}

}
