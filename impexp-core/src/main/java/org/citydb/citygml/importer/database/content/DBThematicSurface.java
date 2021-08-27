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
import org.citydb.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class DBThematicSurface implements DBImporter {
	private final CityGMLImportManager importer;

	private PreparedStatement psThematicSurface;
	private DBCityObject cityObjectImporter;
	private DBSurfaceGeometry surfaceGeometryImporter;
	private DBOpening openingImporter;
	private int batchCounter;
	private String PREFIX_ONTOCITYGML;
	private String IRI_GRAPH_BASE;
	private String IRI_GRAPH_OBJECT;
	private static final String IRI_GRAPH_OBJECT_REL = "thematicsurface/";

	public DBThematicSurface(Connection batchConn, Config config, CityGMLImportManager importer) throws CityGMLImportException, SQLException {
		this.importer = importer;

		String schema = importer.getDatabaseAdapter().getConnectionDetails().getSchema();

		String stmt = "insert into " + schema + ".thematic_surface (id, objectclass_id, building_id, room_id, building_installation_id, lod2_multi_surface_id, lod3_multi_surface_id, lod4_multi_surface_id) values " +
				"(?, ?, ?, ?, ?, ?, ?, ?)";

		if (importer.isBlazegraph()) {
			PREFIX_ONTOCITYGML = importer.getOntoCityGmlPrefix();
			IRI_GRAPH_BASE = importer.getGraphBaseIri();
			IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
			stmt = getSPARQLStatement();
		}

		psThematicSurface = batchConn.prepareStatement(stmt);

		surfaceGeometryImporter = importer.getImporter(DBSurfaceGeometry.class);
		cityObjectImporter = importer.getImporter(DBCityObject.class);
		openingImporter = importer.getImporter(DBOpening.class);
	}

	private String getSPARQLStatement(){
		String param = "  ?;";
		String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"BASE <" + IRI_GRAPH_BASE + "> " +
				"INSERT DATA" +
				" { GRAPH <" + IRI_GRAPH_OBJECT_REL + "> " +
				"{ ? "+ SchemaManagerAdapter.ONTO_ID + param +
				SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID + param  +
				SchemaManagerAdapter.ONTO_BUILDING_ID + param  +
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

	protected long doImport(CeilingSurface ceilingSurface) throws CityGMLImportException, SQLException {
		return doImport(ceilingSurface, null, 0);
	}

	protected long doImport(InteriorWallSurface interiorWallSurface) throws CityGMLImportException, SQLException {
		return doImport(interiorWallSurface, null, 0);
	}

	protected long doImport(FloorSurface floorSurface) throws CityGMLImportException, SQLException {
		return doImport(floorSurface, null, 0);
	}

	protected long doImport(RoofSurface roofSurface) throws CityGMLImportException, SQLException {
		return doImport(roofSurface, null, 0);
	}

	protected long doImport(WallSurface wallSurface ) throws CityGMLImportException, SQLException {
		return doImport(wallSurface, null, 0);
	}

	protected long doImport(GroundSurface groundSurface) throws CityGMLImportException, SQLException {
		return doImport(groundSurface, null, 0);
	}

	protected long doImport(ClosureSurface closureSurface) throws CityGMLImportException, SQLException {
		return doImport(closureSurface, null, 0);
	}

	protected long doImport(OuterCeilingSurface outerCeilingSurface) throws CityGMLImportException, SQLException {
		return doImport(outerCeilingSurface, null, 0);
	}

	protected long doImport(OuterFloorSurface outerFloorSurface) throws CityGMLImportException, SQLException {
		return doImport(outerFloorSurface, null, 0);
	}

	public long doImport(AbstractBoundarySurface boundarySurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(boundarySurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(boundarySurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + boundarySurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				boundarySurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			boundarySurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
    } else {
      psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
			case 0:
				multiSurfaceProperty = boundarySurface.getLod2MultiSurface();
				break;
			case 1:
				multiSurfaceProperty = boundarySurface.getLod3MultiSurface();
				break;
			case 2:
				multiSurfaceProperty = boundarySurface.getLod4MultiSurface();
				break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId, 
								href, 
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
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
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),								
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}
		
		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(boundarySurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(CeilingSurface ceilingSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(ceilingSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(ceilingSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + ceilingSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				ceilingSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			ceilingSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = ceilingSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = ceilingSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = ceilingSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (ceilingSurface.isSetOpening()) {
			for (OpeningProperty property : ceilingSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, ceilingSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(ceilingSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(InteriorWallSurface interiorWallSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(interiorWallSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(interiorWallSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + interiorWallSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				interiorWallSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			interiorWallSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = interiorWallSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = interiorWallSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = interiorWallSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (interiorWallSurface.isSetOpening()) {
			for (OpeningProperty property : interiorWallSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, interiorWallSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(interiorWallSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(FloorSurface floorSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(floorSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(floorSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + floorSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				floorSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			floorSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = floorSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = floorSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = floorSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (floorSurface.isSetOpening()) {
			for (OpeningProperty property : floorSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, floorSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(floorSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(RoofSurface roofSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(roofSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(roofSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + roofSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				roofSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			roofSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = roofSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = roofSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = roofSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (roofSurface.isSetOpening()) {
			for (OpeningProperty property : roofSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, roofSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(roofSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(WallSurface wallSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(wallSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(wallSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + wallSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				wallSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			wallSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = wallSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = wallSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = wallSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (wallSurface.isSetOpening()) {
			for (OpeningProperty property : wallSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, wallSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(wallSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(GroundSurface groundSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(groundSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(groundSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + groundSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				groundSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			groundSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = groundSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = groundSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = groundSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (groundSurface.isSetOpening()) {
			for (OpeningProperty property : groundSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, groundSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(groundSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(ClosureSurface closureSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(closureSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(closureSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + closureSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				closureSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			closureSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = closureSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = closureSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = closureSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (closureSurface.isSetOpening()) {
			for (OpeningProperty property : closureSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, closureSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(closureSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(OuterCeilingSurface outerCeilingSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(outerCeilingSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(outerCeilingSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + outerCeilingSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				outerCeilingSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			outerCeilingSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = outerCeilingSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = outerCeilingSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = outerCeilingSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (outerCeilingSurface.isSetOpening()) {
			for (OpeningProperty property : outerCeilingSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, outerCeilingSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(outerCeilingSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	public long doImport(OuterFloorSurface outerFloorSurface, AbstractCityObject parent, long parentId) throws CityGMLImportException, SQLException {
		FeatureType featureType = importer.getFeatureType(outerFloorSurface);
		if (featureType == null)
			throw new SQLException("Failed to retrieve feature type.");

		// import city object information
		long boundarySurfaceId = cityObjectImporter.doImport(outerFloorSurface, featureType);

		URL objectURL;
		URL parentURL = (URL) parent.getLocalProperty(CoreConstants.OBJECT_URIID);
		int index = 0;

		// import boundary surface information
		// primary id
		if (importer.isBlazegraph()) {
			try {
				objectURL = new URL(IRI_GRAPH_OBJECT + outerFloorSurface.getId() + "/");
				psThematicSurface.setURL(++index, objectURL);
				psThematicSurface.setURL(++index, objectURL);
				outerFloorSurface.setLocalProperty(CoreConstants.OBJECT_URIID, objectURL);
			} catch (MalformedURLException e) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			}
			outerFloorSurface.setLocalProperty(CoreConstants.OBJECT_PARENT_URIID, parentURL);
		} else {
			psThematicSurface.setLong(++index, boundarySurfaceId);
		}

		// objectclass id
		psThematicSurface.setInt(++index, featureType.getObjectClassId());

		// parent id
		if (parent instanceof AbstractBuilding) {

			if (importer.isBlazegraph()) {
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof Room) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		} else if (parent instanceof BuildingInstallation
				|| parent instanceof IntBuildingInstallation) {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				psThematicSurface.setURL(++index, parentURL);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setLong(++index, parentId);
			}
		} else {
			if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		// bldg:lodXMultiSurface
		for (int i = 0; i < 3; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
				case 0:
					multiSurfaceProperty = outerFloorSurface.getLod2MultiSurface();
					break;
				case 1:
					multiSurfaceProperty = outerFloorSurface.getLod3MultiSurface();
					break;
				case 2:
					multiSurfaceProperty = outerFloorSurface.getLod4MultiSurface();
					break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.doImport(multiSurfaceProperty.getMultiSurface(), boundarySurfaceId);
					if (multiSurfaceId != 0) {
						if (!importer.isBlazegraph()) {
							psThematicSurface.setLong(++index, multiSurfaceId);
						} else {
							try {
								psThematicSurface.setURL(
										++index,
										new URL(DBSurfaceGeometry.IRI_GRAPH_OBJECT + multiSurfaceProperty.getMultiSurface().getId() + "/"));
							} catch (MalformedURLException e) {
								new CityGMLImportException(e);
							}
						}
					} else if (importer.isBlazegraph()) {
						setBlankNode(psThematicSurface, ++index);
					} else {
						psThematicSurface.setNull(++index, Types.NULL);
					}
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					String href = multiSurfaceProperty.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkSurfaceGeometry(
								TableEnum.THEMATIC_SURFACE.getName(),
								boundarySurfaceId,
								href,
								"lod" + (i + 2) + "_multi_surface_id"));
					}
				}
			} else if (importer.isBlazegraph()) {
				setBlankNode(psThematicSurface, ++index);
			} else {
				psThematicSurface.setNull(++index, Types.NULL);
			}
		}

		psThematicSurface.addBatch();
		if (++batchCounter == importer.getDatabaseAdapter().getMaxBatchSize())
			importer.executeBatch(TableEnum.THEMATIC_SURFACE);

		// bldg:opening
		if (outerFloorSurface.isSetOpening()) {
			for (OpeningProperty property : outerFloorSurface.getOpening()) {
				AbstractOpening opening = property.getOpening();

				if (opening != null) {
					openingImporter.doImport(opening, outerFloorSurface, boundarySurfaceId);
					property.unsetOpening();
				} else {
					String href = property.getHref();
					if (href != null && href.length() != 0) {
						importer.propagateXlink(new DBXlinkBasic(
								TableEnum.OPENING_TO_THEM_SURFACE.getName(),
								boundarySurfaceId,
								"THEMATIC_SURFACE_ID",
								href,
								"OPENING_ID"));
					}
				}
			}
		}

		// ADE-specific extensions
		if (importer.hasADESupport())
			importer.delegateToADEImporter(outerFloorSurface, boundarySurfaceId, featureType);

		return boundarySurfaceId;
	}

	@Override
	public void executeBatch() throws CityGMLImportException, SQLException {
		if (batchCounter > 0) {
			psThematicSurface.executeBatch();
			batchCounter = 0;
		}
	}

	@Override
	public void close() throws CityGMLImportException, SQLException {
		psThematicSurface.close();
	}

	/**
	 * Sets blank nodes on PreparedStatements. Used with SPARQL which does not support nulls.
	 */
	private void setBlankNode(PreparedStatement smt, int index) throws CityGMLImportException {
		importer.setBlankNode(smt, index);
	}
}
