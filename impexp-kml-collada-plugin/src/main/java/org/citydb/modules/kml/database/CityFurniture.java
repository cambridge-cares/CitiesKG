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

import net.opengis.kml._2.PlacemarkType;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.config.Config;
import org.citydb.config.project.kmlExporter.*;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.database.adapter.blazegraph.OptimizedSparqlQuery;
import org.citydb.database.adapter.blazegraph.StatementTransformer;
import org.citydb.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.modules.kml.util.AffineTransformer;
import org.citydb.modules.kml.util.BalloonTemplateHandler;
import org.citydb.modules.kml.util.ElevationServiceHandler;
import org.citydb.query.Query;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class CityFurniture extends KmlGenericObject{
	private final Logger log = Logger.getInstance();

	public static final String STYLE_BASIS_NAME = "Furniture";

	public CityFurniture(Connection connection,
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
		return config.getProject().getKmlExporter().getCityFurnitureDisplayForms();
	}

	public ColladaOptions getColladaOptions() {
		return config.getProject().getKmlExporter().getCityFurnitureColladaOptions();
	}

	public Balloon getBalloonSettings() {
		return config.getProject().getKmlExporter().getCityFurnitureBalloon();
	}

	public String getStyleBasisName() {
		return STYLE_BASIS_NAME;
	}

	public void read(KmlSplittingResult work) {
		PreparedStatement psQuery = null;
		ResultSet rs = null;
		boolean existGS = false;
		ArrayList<ResultSet> sparqlGeom;
		OptimizedSparqlQuery optquery = new OptimizedSparqlQuery(databaseAdapter);

		try {
			int lodToExportFrom = config.getProject().getKmlExporter().getLodToExportFrom();
			currentLod = lodToExportFrom == 5 ? 4: lodToExportFrom;
			int minLod = lodToExportFrom == 5 ? 1: lodToExportFrom;

			while (currentLod >= minLod) {
				if (!work.getDisplayForm().isAchievableFromLoD(currentLod))
					break;

				try {
					String query = null;
					if (isBlazegraph) {
						query = StatementTransformer.getCityFurnitureQuery();
						psQuery = connection.prepareStatement(query);

						String baseURL = StatementTransformer.getIriObjectBase() + "cityfurniture/";
						URL url = null;
						try {
							url = new URL(baseURL + work.getGmlId() + "/");
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}

						psQuery.setURL(1, url);
						rs = psQuery.executeQuery();

						if (rs.isBeforeFirst()) {
							rs.next();
							break;
						}


					} else {
						query = queries.getCityFurnitureBasisData(currentLod);
						psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
						for (int i = 1; i <= getParameterCount(query); i++)
							psQuery.setLong(i, (long)work.getId());

						rs = psQuery.executeQuery();
						if (rs.isBeforeFirst()) {
							rs.next();
							if (rs.getLong(4) != 0 || rs.getLong(1) != 0)
								break; // result set not empty
						}
					}

				} catch (Exception e) {
					log.error("SQL error while querying the highest available LOD: " + e.getMessage());
					try { if (rs != null) rs.close(); } catch (SQLException sqle) {}
					try { if (psQuery != null) psQuery.close(); } catch (SQLException sqle) {}
					rs = null;
				}

				currentLod--;
			}

			if (rs == null) { // result empty, give up
				String fromMessage = " from LoD" + lodToExportFrom;
				if (lodToExportFrom == 5) {
					if (work.getDisplayForm().getForm() == DisplayForm.COLLADA)
						fromMessage = ". LoD1 or higher required";
					else
						fromMessage = " from any LoD";
				}
				log.info("Could not display object " + work.getGmlId() + " as " + work.getDisplayForm().getName() + fromMessage + ".");
			}

			else { // result not empty
				// decide whether explicit or implicit geometry
				AffineTransformer transformer = null;
				String query = null;
				if (isBlazegraph){
					String cityObjectId = rs.getString(1);
					System.out.println("CityObjectID" + cityObjectId);

					try { rs.close(); } catch (SQLException sqle) {}
					try { psQuery.close(); } catch (SQLException sqle) {}

					query = StatementTransformer.getSPARQLStatement_BuildingPartGeometry();
					psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

					URL url = null;
					try {
						url = new URL(cityObjectId);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					psQuery.setURL(1, url);  // set sgRootId

				} else {
					long sgRootId = rs.getLong(4);
					if (sgRootId == 0) {
						sgRootId = rs.getLong(1);
						transformer = getAffineTransformer(rs, 2, 3);
					}

					try { rs.close(); } catch (SQLException sqle) {}
					try { psQuery.close(); } catch (SQLException sqle) {}
					rs = null;

					query = queries.getCityFurnitureQuery(currentLod,
							work.getDisplayForm(),
							transformer != null,
							work.getDisplayForm().getForm() == DisplayForm.COLLADA && !config.getProject().getKmlExporter().getAppearanceTheme().equals(KmlExporter.THEME_NONE));
					psQuery = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
					psQuery.setLong(1, sgRootId);
				}
				rs = psQuery.executeQuery();


				kmlExporterManager.updateFeatureTracker(work);

				String cityFurnitureId = StatementTransformer.getIriObjectBase() + "cityfurniture/" +  work.getGmlId() + "/";
				sparqlGeom = optquery.getSPARQLAggregateGeometriesForCityFurniture(connection, cityFurnitureId);

				if (!sparqlGeom.isEmpty()){
					existGS = false;
				}
				// get the proper displayForm (for highlighting)
				int indexOfDf = getDisplayForms().indexOf(work.getDisplayForm());
				if (indexOfDf != -1)
					work.setDisplayForm(getDisplayForms().get(indexOfDf));

				switch (work.getDisplayForm().getForm()) {
				case DisplayForm.FOOTPRINT:
					if (isBlazegraph){
						kmlExporterManager.print(createPlacemarksForFootprint_geospatial(sparqlGeom, work, existGS, null),
								work, getBalloonSettings().isBalloonContentInSeparateFile());
					} else {
						kmlExporterManager.print(createPlacemarksForFootprint(rs, work, transformer),
								work,
								getBalloonSettings().isBalloonContentInSeparateFile());
					}
					break;

				case DisplayForm.EXTRUDED:
					PreparedStatement psQuery2 = null;
					ResultSet rs2 = null;

					try {
						query = queries.getExtrusionHeight();
						psQuery2 = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
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
								psQuery2.setLong(i, (long)work.getId());
						}

						rs2 = psQuery2.executeQuery();
						rs2.next();

						double measuredHeight = 0;
						boolean reversePointOrder = false;
						if (isBlazegraph) {
							String envelop = rs2.getString(1);
							measuredHeight = Building.extractHeight(envelop);
							kmlExporterManager.print(createPlacemarksForExtruded_geospatial(sparqlGeom, work, measuredHeight, reversePointOrder, existGS, null), work, getBalloonSettings().isBalloonContentInSeparateFile());
						} else {
							measuredHeight = rs2.getDouble("envelope_measured_height");
							kmlExporterManager.print(createPlacemarksForExtruded(rs, work, measuredHeight, false, transformer),
									work, getBalloonSettings().isBalloonContentInSeparateFile());
						}
						break;
					} finally {
						try { if (rs2 != null) rs2.close(); } catch (SQLException e) {}
						try { if (psQuery2 != null) psQuery2.close(); } catch (SQLException e) {}
					}

					case DisplayForm.GEOMETRY:
						setGmlId(work.getGmlId());
						setId(work.getId());
						if (isBlazegraph) {
							kmlExporterManager.print(createPlacemarksForGeometry_geospatial(rs, work), work, getBalloonSettings().isBalloonContentInSeparateFile());
						} else {

							if (this.query.isSetTiling()) { // region
								if (work.getDisplayForm().isHighlightingEnabled())
									kmlExporterManager.print(createPlacemarksForHighlighting(rs, work, transformer, false), work, getBalloonSettings().isBalloonContentInSeparateFile());

								kmlExporterManager.print(createPlacemarksForGeometry(rs, work, transformer, false), work, getBalloonSettings().isBalloonContentInSeparateFile());
							} else { // reverse order for single objects
								kmlExporterManager.print(createPlacemarksForGeometry(rs, work, transformer, false), work, getBalloonSettings().isBalloonContentInSeparateFile());
								if (work.getDisplayForm().isHighlightingEnabled())
									kmlExporterManager.print(createPlacemarksForHighlighting(rs, work, transformer, false), work, getBalloonSettings().isBalloonContentInSeparateFile());
							}
							break;
						}
						break;

					case DisplayForm.COLLADA:
						fillGenericObjectForCollada(rs, config.getProject().getKmlExporter().getCityFurnitureColladaOptions().isGenerateTextureAtlases(),  transformer, false);
						String currentgmlId = getGmlId();
						setGmlId(work.getGmlId());
						setId(work.getId());

//						System.out.println(work.getId());
						//fillGenericObjectForCollada(rs, config.getProject().getKmlExporter().getCityFurnitureColladaOptions().isGenerateTextureAtlases(),  transformer, false);

						if (currentgmlId != null && !currentgmlId.equals(work.getGmlId()) && getGeometryAmount() > GEOMETRY_AMOUNT_WARNING)
							log.info("Object " + work.getGmlId() + " has more than " + GEOMETRY_AMOUNT_WARNING + " geometries. This may take a while to process...");

						List<Point3d> anchorCandidates = getOrigins();
						double zOffset = getZOffsetFromConfigOrDB(Long.parseLong((String) work.getId()));
						if (zOffset == Double.MAX_VALUE) {
							zOffset = getZOffsetFromGEService(Long.parseLong((String) work.getId()), anchorCandidates);
						}
						setZOffset(zOffset);

						ColladaOptions colladaOptions = getColladaOptions();
						setIgnoreSurfaceOrientation(colladaOptions.isIgnoreSurfaceOrientation());
						try {
							if (work.getDisplayForm().isHighlightingEnabled())
								kmlExporterManager.print(createPlacemarksForHighlighting(rs, work, transformer, false), work, getBalloonSettings().isBalloonContentInSeparateFile());
						} catch (Exception ioe) {
							log.logStackTrace(ioe);
						}

						break;
				}
			}
		} catch (SQLException sqlEx) {
			log.error("SQL error while querying city object " + work.getGmlId() + ": " + sqlEx.getMessage());
		} catch (JAXBException jaxbEx) {
			log.error("XML error while working on city object " + work.getGmlId() + ": " + jaxbEx.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			if (rs != null)
				try { rs.close(); } catch (SQLException e) {}
			if (psQuery != null)
				try { psQuery.close(); } catch (SQLException e) {}
		}
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

}