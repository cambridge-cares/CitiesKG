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
package org.citydb.modules.kml.database;

import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.PlacemarkType;
import org.citydb.config.Config;
import org.citydb.config.project.kmlExporter.Balloon;
import org.citydb.config.project.kmlExporter.ColladaOptions;
import org.citydb.config.project.kmlExporter.DisplayForm;
import org.citydb.config.project.kmlExporter.Lod0FootprintMode;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.database.adapter.blazegraph.StatementTransformer;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.modules.kml.util.BalloonTemplateHandler;
import org.citydb.modules.kml.util.ElevationServiceHandler;
import org.citydb.query.Query;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Building extends KmlGenericObject{
	private final Logger log = Logger.getInstance();

	public static final String STYLE_BASIS_NAME = ""; // "Building"
	public boolean isBlazegraph = kmlExporterManager.isBlazegraph();

	public Building(Connection connection,
			Query query,
			KmlExporterManager kmlExporterManager,
			net.opengis.kml._2.ObjectFactory kmlFactory,
			AbstractDatabaseAdapter databaseAdapter,
			BlobExportAdapter textureExportAdapter,
			ElevationServiceHandler elevationServiceHandler,
			BalloonTemplateHandler balloonTemplateHandler,
			EventDispatcher eventDispatcher,
			Config config) {

		super(connection,
				query,
				kmlExporterManager,
				kmlFactory,
				databaseAdapter,
				textureExportAdapter,
				elevationServiceHandler,
				balloonTemplateHandler,
				eventDispatcher,
				config);
	}

	protected List<DisplayForm> getDisplayForms() {
		return config.getProject().getKmlExporter().getBuildingDisplayForms();
	}

	public ColladaOptions getColladaOptions() {
		return config.getProject().getKmlExporter().getBuildingColladaOptions();
	}

	public Balloon getBalloonSettings() {
		return config.getProject().getKmlExporter().getBuildingBalloon();
	}

	public String getStyleBasisName() {
		return STYLE_BASIS_NAME;
	}

	public void read(KmlSplittingResult work) {
		List<PlacemarkType> placemarks = new ArrayList<>();
		PreparedStatement psQuery = null;
		ResultSet rs = null;

		try {
			String query = queries.getBuildingPartsFromBuilding();

			// TODO: Translate the query
			if (isBlazegraph) {
				query = StatementTransformer.getSPARQLStatement_BuildingParts(query);
			}
			psQuery = connection.prepareStatement(query);

			// Modified by Shiying
			if (isBlazegraph) {
				String baseURL = StatementTransformer.getIriObjectBase() + "building/";
				URL url = null;
				try {
					url = new URL(baseURL + work.getGmlId()+"/");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				psQuery.setURL(1, url);   // setURL will add <> around the URL
			} else {
				for (int i = 1; i <= getParameterCount(query); i++) {
					psQuery.setLong(i, Long.class.cast(work.getId()));
				}
			}
			rs = psQuery.executeQuery();

			List<PlacemarkType> placemarkBPart = null;
			while (rs.next()) {

				if (isBlazegraph){
					String buildingPartId = rs.getString(1);
					placemarkBPart = readBuildingPart(buildingPartId, work);
				}else{
					long buildingPartId = rs.getLong(1);
					placemarkBPart = readBuildingPart(buildingPartId, work);
				}

				if (placemarkBPart != null)
					placemarks.addAll(placemarkBPart);
			}
		} catch (SQLException sqlEx) {
			log.error("SQL error while getting building parts for building " + work.getGmlId() + ": " + sqlEx.getMessage());
			return;
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException sqle) {} 
			try { if (psQuery != null) psQuery.close(); } catch (SQLException sqle) {}
		}

		if (placemarks.size() == 0) {
			int lodToExportFrom = config.getProject().getKmlExporter().getLodToExportFrom();
			String fromMessage = " from LoD" + lodToExportFrom;
			if (lodToExportFrom == 5) {
				if (work.getDisplayForm().getForm() == DisplayForm.COLLADA)
					fromMessage = ". LoD1 or higher required";
				else
					fromMessage = " from any LoD";
			}
			log.info("Could not display object " + work.getGmlId()  + " as " + work.getDisplayForm().getName() + fromMessage + ".");
		}
		
		else {
			try {
				// compact list before exporting
				for (int i = 0; i < placemarks.size(); i++) {
					PlacemarkType placemark1 = placemarks.get(i);
					if (placemark1 == null) continue;
					MultiGeometryType multiGeometry1 = (MultiGeometryType) placemark1.getAbstractGeometryGroup().getValue();
					for (int j = i+1; j < placemarks.size(); j++) {
						PlacemarkType placemark2 = placemarks.get(j);
						if (placemark2 == null || !placemark1.getId().equals(placemark2.getId())) continue;
						// compact since ids are identical
						MultiGeometryType multiGeometry2 = (MultiGeometryType) placemark2.getAbstractGeometryGroup().getValue();
						multiGeometry1.getAbstractGeometryGroup().addAll(multiGeometry2.getAbstractGeometryGroup());
						placemarks.set(j, null); // polygons transfered, placemark exhausted
					}
				}

				kmlExporterManager.updateFeatureTracker(work);
				kmlExporterManager.print(placemarks,
						work,
						getBalloonSettings().isBalloonContentInSeparateFile());
			} catch (JAXBException jaxbEx) {
				//
			}
		}
	}

	private <T> List<PlacemarkType> readBuildingPart(T buildingPartId, KmlSplittingResult work) {
		PreparedStatement psQuery = null;
		ResultSet rs = null;
		boolean reversePointOrder = false;
		// Shiying: we need to add a variable to differentiate two different cases in FOOTPRINT/EXTRUDED, if GroundSurface exists
		boolean existGS = false;

		try {
			currentLod = config.getProject().getKmlExporter().getLodToExportFrom();
			int displayForm = work.getDisplayForm().getForm();
			Lod0FootprintMode lod0FootprintMode = config.getProject().getKmlExporter().getLod0FootprintMode();

			// we handle FOOTPRINT/EXTRUDED differently than GEOMETRY/COLLADA
			if (displayForm >= DisplayForm.GEOMETRY) {

				if (currentLod == 5) {
					// find the highest available LOD to export from. to increase performance, 
					// this is just a light-weight query that only checks for the main exterior 
					// building shell without appearances 
					while (--currentLod > 0) {
						if (!work.getDisplayForm().isAchievableFromLoD(currentLod)) 
							break;

						try {
							String query = queries.getBuildingPartQuery(currentLod, lod0FootprintMode, work.getDisplayForm(), true);
							psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
							for (int i = 1; i <= getParameterCount(query); i++)
								psQuery.setLong(i, Long.class.cast(buildingPartId));

							rs = psQuery.executeQuery();
							if (rs.isBeforeFirst())
								break;
						} catch (SQLException e) {
							log.error("SQL error while querying the highest available LOD: " + e.getMessage());
							try { connection.commit(); } catch (SQLException sqle) {}
						} finally {
							try { if (rs != null) rs.close(); } catch (SQLException sqle) {} 
							try { if (psQuery != null) psQuery.close(); } catch (SQLException sqle) {}
							rs = null;
						}
					}
				}

				// ok, if we have an LOD to export from, we issue a heavy-weight query to get 
				// the building geometry including sub-features and appearances 
				if (currentLod > 0 && work.getDisplayForm().isAchievableFromLoD(currentLod)) {
					try {
						String query = queries.getBuildingPartQuery(currentLod, lod0FootprintMode, work.getDisplayForm(), false);
						psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						for (int i = 1; i <= getParameterCount(query); i++)
							psQuery.setLong(i, Long.class.cast(buildingPartId));

						rs = psQuery.executeQuery();
					} catch (SQLException e) {
						log.error("SQL error while querying geometries in LOD " + currentLod + ": " + e.getMessage());
						try { if (psQuery != null) psQuery.close(); } catch (SQLException sqle) {}
						try { connection.commit(); } catch (SQLException sqle) {}
						rs = null;
					}
				}
			}
			// DisplayForm: FOOTPRINT/EXTRUDED
			else {
				int minLod = currentLod;
				if (currentLod == 5) {
					currentLod = 4;
					minLod = 0;
				}

				while (currentLod >= minLod) {
					if(!work.getDisplayForm().isAchievableFromLoD(currentLod)) 
						break;

					try {
						// first, check whether we have an LOD0 geometry or a GroundSurface
						String query = queries.getBuildingPartQuery(currentLod, lod0FootprintMode, work.getDisplayForm(), false);
						if (isBlazegraph) {
							query = StatementTransformer.getSPARQLStatement_BuildingPartQuery(query);
						}
						psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						if (isBlazegraph) {
							URL url = null;
							try {
								url = new URL((String)buildingPartId);
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
							psQuery.setURL(1, url);
						}else{
							for (int i = 1; i <= getParameterCount(query); i++)
								psQuery.setLong(i, Long.class.cast(buildingPartId));
						}

						rs = psQuery.executeQuery();

						///// Temporary solution for TWA's bad performance of querying two different graph in one query. ~ 3min
						boolean TWA = databaseAdapter.getConnectionDetails().getServer().contains("theworldavatar");
						if (isBlazegraph && TWA) {
							String fixedlod2MSid = null;
							if (rs.next()) {
								fixedlod2MSid = rs.getString(1);
							}
							String query2 = StatementTransformer.getSPARQLStatement_BuildingPartQuery_part2();
							psQuery = connection.prepareStatement(query2, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
							URL url = null;

							if (fixedlod2MSid !=null){
								try {
									url = new URL(fixedlod2MSid);
									psQuery.setURL(1, url);
									rs = psQuery.executeQuery();
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
							}

						}
						///////////////////// End of Temporary solution
						//@Note: (Shiying) isBeforeFirst() returns different results between Blazegraph and PostGIS for emptySet
						//@Note: If the resultset is not empty, it will jump to extraction
						if (rs.next()){
							existGS = true;
							rs.beforeFirst(); // reset the cursor to avoid missing first row
							break;
						}

						try { rs.close(); } catch (SQLException sqle) {} 
						try { psQuery.close(); } catch (SQLException sqle) {}
					} catch (SQLException e) {
						log.error("SQL error while querying geometries in LOD " + currentLod + ": " + e.getMessage());
						try { if (rs != null) rs.close(); } catch (SQLException sqle) {} 
						try { if (psQuery != null) psQuery.close(); } catch (SQLException sqle) {}
						try { connection.commit(); } catch (SQLException sqle) {}
					}

					if (currentLod > 0 || currentLod == 0 && lod0FootprintMode == Lod0FootprintMode.ROOFPRINT_PRIOR_FOOTPRINT) {
						// second, try and generate a footprint by aggregating geometries						
						reversePointOrder = true;
						int groupBasis = 4;

						try { // Two cases: Blazegraph or Postgres
							String query = null;

							if (isBlazegraph){
								//@TODO: StatementTransformer with optimized SPARQL query including value assignment for TWA
								//String subquery = StatementTransformer.getSPARQLqueryStage2(query, (String)currentLod);

							}else{
								// Initial implementation for SQL including value assignment
								query = queries.getBuildingPartAggregateGeometries(0.001,
										DatabaseConnectionPool.getInstance().getActiveDatabaseAdapter().getUtil().get2DSrid(dbSrs),
										currentLod,
										Math.pow(groupBasis, 4),
										Math.pow(groupBasis, 3),
										Math.pow(groupBasis, 2));
								psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

								for (int i = 1; i <= getParameterCount(query); i++)
									psQuery.setLong(i, Long.class.cast(buildingPartId));
							}


							if (isBlazegraph) {
								URL url = null;
								try {
									url = new URL((String)buildingPartId);
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
								psQuery.setURL(1, url);
								psQuery.setURL(2, url);
								psQuery.setURL(3, url);
							}
							else {

							}

							// Execution of SQL query and the last stage of SPARQL query
							rs = psQuery.executeQuery();

							if (rs.isBeforeFirst()) {
								rs.next();
								existGS = false;
								if (rs.getObject(1) != null) {
									rs.beforeFirst();
									break;
								}
							}

							try { rs.close(); } catch (SQLException sqle) {} 
							try { psQuery.close(); } catch (SQLException sqle) {}
							rs = null;
						} catch (SQLException e) {
							log.error("Error at : " + buildingPartId);
							log.error("SQL error while aggregating geometries in LOD " + currentLod + ": " + e.getMessage());
							try { if (rs != null) rs.close(); } catch (SQLException sqle) {} 
							try { if (psQuery != null) psQuery.close(); } catch (SQLException sqle) {}
							try { connection.commit(); } catch (SQLException sqle) {}
							rs = null;
						}
					}

					currentLod--;
					reversePointOrder = false;
				}
			}

			if (rs != null && rs.isBeforeFirst()) { // if result of Line 334 or287 is not empty. This step will process the result.

				switch (work.getDisplayForm().getForm()) {
				case DisplayForm.FOOTPRINT:
					return createPlacemarksForFootprint(rs, work);

				case DisplayForm.EXTRUDED:  // @TODO: process the data
					PreparedStatement psQuery2 = null;
					ResultSet rs2 = null;

					try {
						String query = queries.getExtrusionHeight();

						psQuery2 = connection.prepareStatement(query);
						if (isBlazegraph){
							URL url = null;
							try {
								url = new URL(StatementTransformer.getIriObjectBase() + "cityobject/" + work.getGmlId()+"/");
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
							psQuery2.setURL(1, url);
						} else {
							for (int i = 1; i <= getParameterCount(query); i++)
								psQuery2.setLong(i, Long.class.cast(buildingPartId));
						}
						rs2 = psQuery2.executeQuery();
						rs2.next();

						double measuredHeight = 0;
						if (isBlazegraph) {
							//log.info("Processing : " + buildingPartId);
							String envelop = rs2.getString(1);
							measuredHeight = extractHeight(envelop);
							return createPlacemarksForExtruded_geospatial(rs, work, measuredHeight, reversePointOrder, existGS, null);
						} else {
							measuredHeight = rs2.getDouble("envelope_measured_height");
							return createPlacemarksForExtruded(rs, work, measuredHeight, reversePointOrder);
						}

					} finally {
						try { if (rs2 != null) rs2.close(); } catch (SQLException e) {}
						try { if (psQuery2 != null) psQuery2.close(); } catch (SQLException e) {}
					}

				case DisplayForm.GEOMETRY:
					setGmlId(work.getGmlId());
					//setId(work.getId());
					if (work.getDisplayForm().isHighlightingEnabled()) {
						if (query.isSetTiling()) { // region
							List<PlacemarkType> hlPlacemarks = createPlacemarksForHighlighting(rs, work, true);
							hlPlacemarks.addAll(createPlacemarksForGeometry(rs, work, true));
							return hlPlacemarks;
						}
						else { // reverse order for single buildings
							List<PlacemarkType> placemarks = createPlacemarksForGeometry(rs, work, true);
							placemarks.addAll(createPlacemarksForHighlighting(rs, work, true));
							return placemarks;
						}
					}
					return createPlacemarksForGeometry(rs, work, true);

				case DisplayForm.COLLADA:
					fillGenericObjectForCollada(rs, config.getProject().getKmlExporter().getBuildingColladaOptions().isGenerateTextureAtlases(), true); // fill and refill
					String currentgmlId = getGmlId();
					setGmlId(work.getGmlId());
					//setId(work.getId());

					if (currentgmlId != null && !currentgmlId.equals(work.getGmlId()) && getGeometryAmount() > GEOMETRY_AMOUNT_WARNING)
						log.info("Object " + work.getGmlId() + " has more than " + GEOMETRY_AMOUNT_WARNING + " geometries. This may take a while to process...");

					List<Point3d> anchorCandidates = getOrigins(); // setOrigins() called mainly for the side-effect
					double zOffset = getZOffsetFromConfigOrDB(Long.class.cast(work.getId()));
					if (zOffset == Double.MAX_VALUE) {
						zOffset = getZOffsetFromGEService(Long.class.cast(work.getId()), anchorCandidates);
					}
					setZOffset(zOffset);

					ColladaOptions colladaOptions = getColladaOptions();
					setIgnoreSurfaceOrientation(colladaOptions.isIgnoreSurfaceOrientation());
					try {
						if (work.getDisplayForm().isHighlightingEnabled()) {
							return createPlacemarksForHighlighting(rs, work, true);
						}
						// just COLLADA, no KML
						List<PlacemarkType> dummy = new ArrayList<>();
						dummy.add(null);
						return dummy;
					}
					catch (Exception ioe) {
						log.logStackTrace(ioe);
					}
				}
			}
		} catch (SQLException sqlEx) {
			log.error("SQL error while querying city object " + work.getGmlId() + ": " + sqlEx.getMessage());
			return null;
		} finally {
			if (rs != null)
				try { rs.close(); } catch (SQLException e) {}
			if (psQuery != null)
				try { psQuery.close(); } catch (SQLException e) {}
		}

		return null; // nothing found 
	}

	public PlacemarkType createPlacemarkForColladaModel() throws SQLException {
		double[] originInWGS84 = convertPointCoordinatesToWGS84(new double[] {getOrigin().x,
				getOrigin().y,
				getOrigin().z});
		setLocation(reducePrecisionForXorY(originInWGS84[0]),
				reducePrecisionForXorY(originInWGS84[1]),
				reducePrecisionForZ(originInWGS84[2]));

		return super.createPlacemarkForColladaModel();
	}
	// Created by Shiying for Blazegraph
	public double extractHeight (String envelop){
		String[] arrOfStr = envelop.split("#");
		//double[] heights = new double[5];

		ArrayList<Double> heights = new ArrayList<Double>();
		for (int k = 1; k <= 5; k++){
			//System.out.println(arrOfStr[(k*3)-1]);
			//heights[k-1] = Double.valueOf(arrOfStr[(k*3)-1]);
			heights.add(Double.valueOf(arrOfStr[(k*3)-1]));
		}
		double max = Collections.max(heights);
		double min = Collections.min(heights);

		return max-min;
	}

}
