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
package org.citydb.citygml.exporter.database.content;

import org.citydb.citygml.exporter.CityGMLExportException;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.database.schema.mapping.FeatureType;
import org.citydb.query.filter.lod.LodFilter;
import org.citydb.query.filter.lod.LodIterator;
import org.citydb.query.filter.projection.CombinedProjectionFilter;
import org.citydb.query.filter.projection.ProjectionFilter;
import org.citydb.sqlbuilder.schema.Table;
import org.citydb.sqlbuilder.select.Select;
import org.citydb.sqlbuilder.select.join.JoinFactory;
import org.citydb.sqlbuilder.select.operator.comparison.ComparisonName;
import org.citygml4j.model.citygml.bridge.AbstractBoundarySurface;
import org.citygml4j.model.citygml.bridge.AbstractBridge;
import org.citygml4j.model.citygml.bridge.AbstractOpening;
import org.citygml4j.model.citygml.bridge.BridgeConstructionElement;
import org.citygml4j.model.citygml.bridge.BridgeInstallation;
import org.citygml4j.model.citygml.bridge.BridgeRoom;
import org.citygml4j.model.citygml.bridge.Door;
import org.citygml4j.model.citygml.bridge.IntBridgeInstallation;
import org.citygml4j.model.citygml.bridge.OpeningProperty;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.core.*;
import org.citygml4j.model.gml.GMLClass;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.module.citygml.CityGMLModuleType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class DBBridgeThematicSurface extends AbstractFeatureExporter<AbstractBoundarySurface> {
	private DBSurfaceGeometry geometryExporter;
	private DBCityObject cityObjectExporter;
	private DBImplicitGeometry implicitGeometryExporter;
	private DBAddress addressExporter;

	private String bridgeModule;
	private LodFilter lodFilter;
	private boolean useXLink;
	private Set<String> surfaceADEHookTables;
	private Set<String> openingADEHookTables;
	private Set<String> addressADEHookTables;

	public DBBridgeThematicSurface(Connection connection, CityGMLExportManager exporter) throws CityGMLExportException, SQLException {
		super(AbstractBoundarySurface.class, connection, exporter);

		CombinedProjectionFilter boundarySurfaceProjectionFilter = exporter.getCombinedProjectionFilter(TableEnum.BRIDGE_THEMATIC_SURFACE.getName());
		CombinedProjectionFilter openingProjectionFilter = exporter.getCombinedProjectionFilter(TableEnum.BRIDGE_OPENING.getName());
		bridgeModule = exporter.getTargetCityGMLVersion().getCityGMLModule(CityGMLModuleType.BRIDGE).getNamespaceURI();
		lodFilter = exporter.getLodFilter();
		String schema = exporter.getDatabaseAdapter().getConnectionDetails().getSchema();
		useXLink = exporter.getExportConfig().getXlink().getFeature().isModeXLink();

		table = new Table(TableEnum.BRIDGE_THEMATIC_SURFACE.getName(), schema);
		Table opening = new Table(TableEnum.BRIDGE_OPENING.getName(), schema);
		Table address = new Table(TableEnum.ADDRESS.getName(), schema);

		select = new Select().addProjection(table.getColumn("id", "tsid"), table.getColumn("objectclass_id"));
		if (boundarySurfaceProjectionFilter.containsProperty("lod2MultiSurface", bridgeModule)) select.addProjection(table.getColumn("lod2_multi_surface_id"));
		if (boundarySurfaceProjectionFilter.containsProperty("lod3MultiSurface", bridgeModule)) select.addProjection(table.getColumn("lod3_multi_surface_id"));
		if (boundarySurfaceProjectionFilter.containsProperty("lod4MultiSurface", bridgeModule)) select.addProjection(table.getColumn("lod4_multi_surface_id"));
		if (boundarySurfaceProjectionFilter.containsProperty("opening", bridgeModule)) {
			Table openingToThemSurface = new Table(TableEnum.BRIDGE_OPEN_TO_THEM_SRF.getName(), schema);
			select.addJoin(JoinFactory.left(openingToThemSurface, "bridge_thematic_surface_id", ComparisonName.EQUAL_TO, table.getColumn("id")))
			.addJoin(JoinFactory.left(opening, "id", ComparisonName.EQUAL_TO, openingToThemSurface.getColumn("bridge_opening_id")))
			.addProjection(opening.getColumn("id", "opid"), opening.getColumn("objectclass_id", "opobjectclass_id"));
			if (openingProjectionFilter.containsProperty("lod3MultiSurface", bridgeModule)) select.addProjection(opening.getColumn("lod3_multi_surface_id", "oplod3_multi_surface_id"));
			if (openingProjectionFilter.containsProperty("lod4MultiSurface", bridgeModule)) select.addProjection(opening.getColumn("lod4_multi_surface_id", "oplod4_multi_surface_id"));
			if (openingProjectionFilter.containsProperty("lod3ImplicitRepresentation", bridgeModule)) 
				select.addProjection(opening.getColumn("lod3_implicit_rep_id"), exporter.getGeometryColumn(opening.getColumn("lod3_implicit_ref_point")), opening.getColumn("lod3_implicit_transformation"));
			if (openingProjectionFilter.containsProperty("lod4ImplicitRepresentation", bridgeModule)) 
				select.addProjection(opening.getColumn("lod4_implicit_rep_id"), exporter.getGeometryColumn(opening.getColumn("lod4_implicit_ref_point")), opening.getColumn("lod4_implicit_transformation"));
			if (openingProjectionFilter.containsProperty("address", bridgeModule)) {
				select.addJoin(JoinFactory.left(address, "id", ComparisonName.EQUAL_TO, opening.getColumn("address_id")))
				.addProjection(opening.getColumn("address_id"), address.getColumn("street"), address.getColumn("house_number"), address.getColumn("po_box"), address.getColumn("zip_code"), address.getColumn("city"),
						address.getColumn("state"), address.getColumn("country"), address.getColumn("xal_source"), exporter.getGeometryColumn(address.getColumn("multi_point")));
			}
		}

		// add joins to ADE hook tables
		if (exporter.hasADESupport()) {
			surfaceADEHookTables = exporter.getADEHookTables(TableEnum.BRIDGE_THEMATIC_SURFACE);
			openingADEHookTables = exporter.getADEHookTables(TableEnum.BRIDGE_OPENING);
			addressADEHookTables = exporter.getADEHookTables(TableEnum.ADDRESS);
			if (surfaceADEHookTables != null) addJoinsToADEHookTables(surfaceADEHookTables, table);
			if (openingADEHookTables != null) addJoinsToADEHookTables(openingADEHookTables, opening);
			if (addressADEHookTables != null) addJoinsToADEHookTables(addressADEHookTables, address);
		}

		cityObjectExporter = exporter.getExporter(DBCityObject.class);
		geometryExporter = exporter.getExporter(DBSurfaceGeometry.class);
		implicitGeometryExporter = exporter.getExporter(DBImplicitGeometry.class);
		addressExporter = exporter.getExporter(DBAddress.class);
	}

	protected Collection<AbstractBoundarySurface> doExport(AbstractBridge parent, long parentId) throws CityGMLExportException, SQLException {
		return doExport(parentId, null, null, getOrCreateStatement("bridge_id"));
	}

	protected Collection<AbstractBoundarySurface> doExport(BridgeInstallation parent, long parentId) throws CityGMLExportException, SQLException {
		return doExport(parentId, null, null, getOrCreateStatement("bridge_installation_id"));
	}

	protected Collection<AbstractBoundarySurface> doExport(IntBridgeInstallation parent, long parentId) throws CityGMLExportException, SQLException {
		return doExport(parentId, null, null, getOrCreateStatement("bridge_installation_id"));
	}

	protected Collection<AbstractBoundarySurface> doExport(BridgeConstructionElement parent, long parentId) throws CityGMLExportException, SQLException {
		return doExport(parentId, null, null, getOrCreateStatement("bridge_constr_element_id"));
	}

	protected Collection<AbstractBoundarySurface> doExport(BridgeRoom parent, long parentId) throws CityGMLExportException, SQLException {
		return doExport(parentId, null, null, getOrCreateStatement("bridge_room_id"));
	}

	@Override
	protected boolean doExport(AbstractBuilding object, String id, FeatureType featureType) throws CityGMLExportException, SQLException {
		return false;
	}

	@Override
	protected Collection<AbstractBoundarySurface> doExport(long id, AbstractBoundarySurface root, FeatureType rootType, PreparedStatement ps) throws CityGMLExportException, SQLException {
		ps.setLong(1, id);

		try (ResultSet rs = ps.executeQuery()) {
			long currentBoundarySurfaceId = 0;
			AbstractBoundarySurface boundarySurface = null;
			ProjectionFilter boundarySurfaceProjectionFilter = null;
			HashMap<Long, AbstractBoundarySurface> boundarySurfaces = new HashMap<>();

			while (rs.next()) {
				long boundarySurfaceId = rs.getLong("tsid");

				if (boundarySurfaceId != currentBoundarySurfaceId || boundarySurface == null) {
					currentBoundarySurfaceId = boundarySurfaceId;

					boundarySurface = boundarySurfaces.get(boundarySurfaceId);
					if (boundarySurface == null) {
						FeatureType featureType = null;
						if (boundarySurfaceId == id && root != null) {
							boundarySurface = root;
							featureType = rootType;						
						} else {
							int objectClassId = rs.getInt("objectclass_id");
							featureType = exporter.getFeatureType(objectClassId);
							if (featureType == null)
								continue;

							// create boundary surface object
							boundarySurface = exporter.createObject(featureType.getObjectClassId(), AbstractBoundarySurface.class);
							if (boundarySurface == null) {
								exporter.logOrThrowErrorMessage("Failed to instantiate " + exporter.getObjectSignature(featureType, boundarySurfaceId) + " as boundary surface object.");
								continue;
							}
						}

						// get projection filter
						boundarySurfaceProjectionFilter = exporter.getProjectionFilter(featureType);

						// export city object information
						cityObjectExporter.doExport(boundarySurface, boundarySurfaceId, featureType, boundarySurfaceProjectionFilter);

						LodIterator lodIterator = lodFilter.iterator(2, 4);
						while (lodIterator.hasNext()) {
							int lod = lodIterator.next();

							if (!boundarySurfaceProjectionFilter.containsProperty(new StringBuilder("lod").append(lod).append("MultiSurface").toString(), bridgeModule))
								continue;

							long lodMultiSurfaceId = rs.getLong(new StringBuilder("lod").append(lod).append("_multi_surface_id").toString());
							if (rs.wasNull())
								continue;

							SurfaceGeometry geometry = geometryExporter.doExport(lodMultiSurfaceId);
							if (geometry != null && geometry.getType() == GMLClass.MULTI_SURFACE) {
								MultiSurfaceProperty multiSurfaceProperty = new MultiSurfaceProperty();
								if (geometry.isSetGeometry())
									multiSurfaceProperty.setMultiSurface((MultiSurface)geometry.getGeometry());
								else
									multiSurfaceProperty.setHref(geometry.getReference());

								switch (lod) {
								case 2:
									boundarySurface.setLod2MultiSurface(multiSurfaceProperty);
									break;
								case 3:
									boundarySurface.setLod3MultiSurface(multiSurfaceProperty);
									break;
								case 4:
									boundarySurface.setLod4MultiSurface(multiSurfaceProperty);
									break;
								}
							}
						}

						// delegate export of generic ADE properties
						if (surfaceADEHookTables != null) {
							List<String> adeHookTables = retrieveADEHookTables(surfaceADEHookTables, rs);
							if (adeHookTables != null)
								exporter.delegateToADEExporter(adeHookTables, boundarySurface, boundarySurfaceId, featureType, boundarySurfaceProjectionFilter);
						}

						boundarySurface.setLocalProperty("projection", boundarySurfaceProjectionFilter);
						boundarySurfaces.put(boundarySurfaceId, boundarySurface);
					} else
						boundarySurfaceProjectionFilter = (ProjectionFilter)boundarySurface.getLocalProperty("projection");
				}

				// continue if openings shall not be exported
				if (!lodFilter.containsLodGreaterThanOrEuqalTo(3)
						|| !boundarySurfaceProjectionFilter.containsProperty("opening", bridgeModule))
					continue;

				long openingId = rs.getLong("opid");
				if (rs.wasNull())
					continue;

				// create new opening object
				int objectClassId = rs.getInt("opobjectclass_id");
				AbstractOpening opening = exporter.createObject(objectClassId, AbstractOpening.class);
				if (opening == null) {
					exporter.logOrThrowErrorMessage("Failed to instantiate " + exporter.getObjectSignature(objectClassId, openingId) + " as opening object.");
					continue;
				}

				// get projection filter
				FeatureType openingType = exporter.getFeatureType(objectClassId);
				ProjectionFilter openingProjectionFilter = exporter.getProjectionFilter(openingType);

				// export city object information
				cityObjectExporter.doExport(opening, openingId, openingType, openingProjectionFilter);

				if (opening.isSetId()) {
					// process xlink
					if (exporter.lookupAndPutObjectUID(opening.getId(), openingId, objectClassId)) {
						if (useXLink) {
							OpeningProperty openingProperty = new OpeningProperty();
							openingProperty.setHref("#" + opening.getId());
							boundarySurface.addOpening(openingProperty);
							continue;
						} else
							opening.setId(exporter.generateNewGmlId(opening));	
					}
				}

				LodIterator lodIterator = lodFilter.iterator(3, 4);
				while (lodIterator.hasNext()) {
					int lod = lodIterator.next();

					if (!openingProjectionFilter.containsProperty(new StringBuilder("lod").append(lod).append("MultiSurface").toString(), bridgeModule))
						continue;

					long lodMultiSurfaceId = rs.getLong(new StringBuilder("oplod").append(lod).append("_multi_surface_id").toString());
					if (rs.wasNull()) 
						continue;

					SurfaceGeometry geometry = geometryExporter.doExport(lodMultiSurfaceId);
					if (geometry != null && geometry.getType() == GMLClass.MULTI_SURFACE) {
						MultiSurfaceProperty multiSurfaceProperty = new MultiSurfaceProperty();
						if (geometry.isSetGeometry())
							multiSurfaceProperty.setMultiSurface((MultiSurface)geometry.getGeometry());
						else
							multiSurfaceProperty.setHref(geometry.getReference());

						switch (lod) {
						case 3:
							opening.setLod3MultiSurface(multiSurfaceProperty);
							break;
						case 4:
							opening.setLod4MultiSurface(multiSurfaceProperty);
							break;
						}
					}
				}

				lodIterator.reset();
				while (lodIterator.hasNext()) {
					int lod = lodIterator.next();

					if (!openingProjectionFilter.containsProperty(new StringBuilder("lod").append(lod).append("ImplicitRepresentation").toString(), bridgeModule))
						continue;

					// get implicit geometry details
					long implicitGeometryId = rs.getLong(new StringBuilder("lod").append(lod).append("_implicit_rep_id").toString());
					if (rs.wasNull())
						continue;

					GeometryObject referencePoint = null;
					Object referencePointObj = rs.getObject(new StringBuilder("lod").append(lod).append("_implicit_ref_point").toString());
					if (!rs.wasNull())
						referencePoint = exporter.getDatabaseAdapter().getGeometryConverter().getPoint(referencePointObj);

					String transformationMatrix = rs.getString(new StringBuilder("lod").append(lod).append("_implicit_transformation").toString());

					ImplicitGeometry implicit = implicitGeometryExporter.doExport(implicitGeometryId, referencePoint, transformationMatrix);
					if (implicit != null) {
						ImplicitRepresentationProperty implicitProperty = new ImplicitRepresentationProperty();
						implicitProperty.setObject(implicit);

						switch (lod) {
						case 3:
							opening.setLod3ImplicitRepresentation(implicitProperty);
							break;
						case 4:
							opening.setLod4ImplicitRepresentation(implicitProperty);
							break;
						}
					}
				}

				if (opening instanceof Door && openingProjectionFilter.containsProperty("address", bridgeModule)) {
					long addressId = rs.getLong("address_id");
					if (!rs.wasNull()) {
						AddressProperty addressProperty = addressExporter.doExport(addressId, rs);
						if (addressProperty != null) {
							((Door)opening).addAddress(addressProperty);

							// delegate export of generic ADE properties
							if (addressADEHookTables != null) {
								List<String> adeHookTables = retrieveADEHookTables(addressADEHookTables, rs);
								if (adeHookTables != null) {
									Address address = addressProperty.getAddress();
									FeatureType featureType = exporter.getFeatureType(address);
									exporter.delegateToADEExporter(adeHookTables, address, addressId, featureType, exporter.getProjectionFilter(featureType));
								}
							}
						}
					}
				}

				// delegate export of generic ADE properties
				if (openingADEHookTables != null) {
					List<String> adeHookTables = retrieveADEHookTables(openingADEHookTables, rs);
					if (adeHookTables != null)
						exporter.delegateToADEExporter(adeHookTables, opening, openingId, openingType, openingProjectionFilter);
				}

				// check whether lod filter is satisfied
				if (!exporter.satisfiesLodFilter(opening))
					continue;

				OpeningProperty openingProperty = new OpeningProperty(opening);
				boundarySurface.addOpening(openingProperty);
			}

			// check whether lod filter is satisfied
			if (!lodFilter.preservesGeometry()) {
				for (Iterator<Entry<Long, AbstractBoundarySurface>> iter = boundarySurfaces.entrySet().iterator(); iter.hasNext(); ) {
					boundarySurface = iter.next().getValue();
					if (!exporter.satisfiesLodFilter(boundarySurface))
						iter.remove();
				}
			}

			return boundarySurfaces.values();
		}
	}

	@Override
	protected Collection<org.citygml4j.model.citygml.building.AbstractBoundarySurface> doExport(String id, org.citygml4j.model.citygml.building.AbstractBoundarySurface root, FeatureType rootType, PreparedStatement ps) throws CityGMLExportException, SQLException {
		return null;
	}

	@Override
	protected Collection<AbstractBuilding> doExport(String id, AbstractBuilding root, FeatureType rootType, PreparedStatement ps) throws CityGMLExportException, SQLException {
		return null;
	}

	@Override
	public void doExport(AbstractCityObject cityObject, long cityObjectId, HashSet<Long> generalizesTos) throws CityGMLExportException, SQLException {

	}

	@Override
	public void doExport(AbstractCityObject cityObject, String cityObjectId, HashSet<Long> generalizesTos) throws CityGMLExportException, SQLException {

	}

	@Override
	public boolean doExport(AbstractGML object, long objectId, AbstractObjectType<?> objectType) throws CityGMLExportException, SQLException {
		return false;
	}

	@Override
	public boolean doExport(AbstractGML object, String objectId, AbstractObjectType<?> objectType) throws CityGMLExportException, SQLException {
		return false;
	}

	@Override
	public boolean doExport(AbstractGML object, String objectId, AbstractObjectType<?> objectType, ProjectionFilter projectionFilter) throws CityGMLExportException, SQLException {
		return false;
	}

	@Override
	public void doExport(AbstractCityObject cityObject, String cityObjectId, ProjectionFilter projectionFilter) throws SQLException {

	}
}
