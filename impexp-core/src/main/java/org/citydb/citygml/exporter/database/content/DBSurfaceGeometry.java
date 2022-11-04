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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.citydb.citygml.common.database.cache.CacheTable;
import org.citydb.citygml.exporter.CityGMLExportException;
import org.citydb.config.Config;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.citydb.database.adapter.blazegraph.StatementTransformer;
import org.citydb.database.schema.TableEnum;
import org.citydb.database.schema.mapping.AbstractObjectType;
import org.citydb.query.filter.projection.ProjectionFilter;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.gml.GMLClass;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.aggregates.MultiSolid;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.citygml4j.model.gml.geometry.complexes.CompositeSolid;
import org.citygml4j.model.gml.geometry.complexes.CompositeSurface;
import org.citygml4j.model.gml.geometry.primitives.AbstractSolid;
import org.citygml4j.model.gml.geometry.primitives.AbstractSurface;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.Exterior;
import org.citygml4j.model.gml.geometry.primitives.Interior;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;
import org.citygml4j.model.gml.geometry.primitives.OrientableSurface;
import org.citygml4j.model.gml.geometry.primitives.Polygon;
import org.citygml4j.model.gml.geometry.primitives.Sign;
import org.citygml4j.model.gml.geometry.primitives.Solid;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.Triangle;
import org.citygml4j.model.gml.geometry.primitives.TrianglePatchArrayProperty;
import org.citygml4j.model.gml.geometry.primitives.TriangulatedSurface;
import org.citygml4j.util.gmlid.DefaultGMLIdManager;

import org.citydb.sqlbuilder.expression.PlaceHolder;
import org.citydb.sqlbuilder.schema.Table;
import org.citydb.sqlbuilder.select.Select;
import org.citydb.sqlbuilder.select.operator.comparison.ComparisonFactory;
import org.citydb.sqlbuilder.select.projection.ConstantColumn;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class DBSurfaceGeometry implements DBExporter {
	private final CityGMLExportManager exporter;

	private PreparedStatement psSelect;
	private PreparedStatement psImport;

	private boolean exportAppearance;
	private boolean useXLink;
	private boolean appendOldGmlId;
	private boolean isImplicit;
	private String gmlIdPrefix;

	private int commitAfter;
	private int batchCounter;
	private String PREFIX_ONTOCITYGML;
	private String IRI_GRAPH_BASE;
	public static String IRI_GRAPH_OBJECT;
	private static final String IRI_GRAPH_OBJECT_REL = "surfacegeometry/";

	private DBSurfaceGeometry geometryExporter;
	private final Connection connection;

	public DBSurfaceGeometry(Connection connection, CacheTable cacheTable, CityGMLExportManager exporter, Config config) throws SQLException, CityGMLExportException {
		this.exporter = exporter;
		String schema = exporter.getDatabaseAdapter().getConnectionDetails().getSchema();
		this.connection = connection;

		exportAppearance = config.getInternal().isExportGlobalAppearances();
		if (exportAppearance) {
			commitAfter = exporter.getDatabaseAdapter().getMaxBatchSize();
			Integer commitAfterProp = config.getProject().getDatabase().getUpdateBatching().getTempBatchValue();
			if (commitAfterProp != null && commitAfterProp > 0 && commitAfterProp <= exporter.getDatabaseAdapter().getMaxBatchSize())
				commitAfter = commitAfterProp;

			Table table = new Table(TableEnum.TEXTUREPARAM.getName(), schema);
			Select select = new Select().addProjection(new ConstantColumn(new PlaceHolder<>()));
			if (exporter.getDatabaseAdapter().getSQLAdapter().requiresPseudoTableInSelect()) select.setPseudoTable(exporter.getDatabaseAdapter().getSQLAdapter().getPseudoTableName());
			select.addSelection(ComparisonFactory.exists(new Select().addProjection(new ConstantColumn(1).withFromTable(table))
					.addSelection(ComparisonFactory.equalTo(table.getColumn("surface_geometry_id"), new PlaceHolder<>()))));
			
			psImport = cacheTable.getConnection().prepareStatement("insert into " + cacheTable.getTableName() + " " + select.toString());
		}

		useXLink = exporter.getExportConfig().getXlink().getGeometry().isModeXLink();
		if (!useXLink) {
			appendOldGmlId = exporter.getExportConfig().getXlink().getGeometry().isSetAppendId();
			gmlIdPrefix = exporter.getExportConfig().getXlink().getGeometry().getIdPrefix();
		}



		if (exporter.isBlazegraph()) {
			PREFIX_ONTOCITYGML = exporter.getOntoCityGmlPrefix();
			IRI_GRAPH_BASE = exporter.getGraphBaseIri();
			IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;

			String  stmt = getSPARQLStatement().toString();
			psSelect = connection.prepareStatement(stmt);
		} else{
			Table table = new Table(TableEnum.SURFACE_GEOMETRY.getName(), schema);
			Select select = new Select().addProjection(table.getColumn("id"), table.getColumn("gmlid"), table.getColumn("parent_id"), table.getColumn("is_solid"), table.getColumn("is_composite"),
							table.getColumn("is_triangulated"), table.getColumn("is_xlink"), table.getColumn("is_reverse"),
							exporter.getGeometryColumn(table.getColumn("geometry")), table.getColumn("implicit_geometry"))
					.addSelection(ComparisonFactory.equalTo(table.getColumn("root_id"), new PlaceHolder<>()));

			psSelect = connection.prepareStatement(select.toString());
		}

	}

	private StringBuilder getSPARQLStatement(){
		StringBuilder stmt = new StringBuilder();
		String param = "  ?;";
		stmt = stmt.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"SELECT  ?value ?predicate ?datatype ?isblank ?graph ?model " +
				"WHERE { GRAPH ?graph { ?model  ?predicate  ?value }" +
				"BIND(datatype(?value) AS ?datatype) " +
				"BIND(isBlank(?value) AS ?isblank) " +
				"?model " +  SchemaManagerAdapter.ONTO_ID + param +
				"} ORDER BY ?model"
		);
		return stmt;
	}

	private StringBuilder getChildrenStatement(){
		StringBuilder sparqlString = new StringBuilder();
		String param = "  ?;";
		sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
				"SELECT distinct ?surf " +
				"WHERE { ?surf " + SchemaManagerAdapter.ONTO_PARENT_ID + param +
				SchemaManagerAdapter.ONTO_GEOMETRY + " ?geomtype ." +
				"FILTER (!isBlank(?geomtype)) }");

		return sparqlString;
	}

	protected SurfaceGeometry doExport(long rootId) throws CityGMLExportException, SQLException {
		psSelect.setLong(1, rootId);

		try (ResultSet rs = psSelect.executeQuery()) {
			GeometryTree geomTree = new GeometryTree();

			// firstly, read the geometry entries into a flat geometry tree structure
			while (rs.next()) {
				long id = rs.getLong(1);

				// constructing a geometry node
				GeometryNode geomNode = new GeometryNode();
				geomNode.id = id;				
				geomNode.gmlId = rs.getString(2);
				geomNode.parentId = rs.getLong(3);
				geomNode.isSolid = rs.getBoolean(4);
				geomNode.isComposite = rs.getBoolean(5);
				geomNode.isTriangulated = rs.getBoolean(6);
				geomNode.isXlink = rs.getBoolean(7);
				geomNode.isReverse = rs.getBoolean(8);

				GeometryObject geometry = null;
				Object object = rs.getObject(!isImplicit ? 9 : 10);
				if (!rs.wasNull()) {
					try {
						geometry = exporter.getDatabaseAdapter().getGeometryConverter().getPolygon(object);
					} catch (Exception e) {
						exporter.logOrThrowErrorMessage(new StringBuilder("Skipping ").append(exporter.getGeometrySignature(GMLClass.POLYGON, id))
								.append(": ").append(e.getMessage()).toString());
						continue;
					}
				}

				geomNode.geometry = geometry;

				// put polygon into the geometry tree
				geomTree.insertNode(geomNode, geomNode.parentId);
			}

			// interpret geometry tree as a single abstract geometry
			if (geomTree.root != null)
				return rebuildGeometry(geomTree.getNode(geomTree.root), false, false);
			else {
				exporter.logOrThrowErrorMessage("Failed to interpret geometry object.");
				return null;
			}
		}
	}

	protected SurfaceGeometry doExport(String rootId) throws CityGMLExportException, SQLException {
		URL url = null;
		try {
			url = new URL(rootId);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		psSelect.setURL(1, url);

		try (ResultSet rs = psSelect.executeQuery()) {
			GeometryTree geomTree = new GeometryTree();
			GeometryNode geomNode = new GeometryNode();
			geomTree.root = rootId;
			// firstly, read the geometry entries into a flat geometry tree structure
			geomNode = constructGeomNode(rs);
			//query child
			String stmt = getChildrenStatement().toString();
			PreparedStatement psChildren = connection.prepareStatement(stmt);
			psChildren.setURL(1, url);
			try (ResultSet rsChildren = psChildren.executeQuery()) {
				while (rsChildren.next()) {
					String surfaceGeometryId = rsChildren.getString("surf");
					String stmtChild = getSPARQLStatement().toString();
					PreparedStatement psChild = connection.prepareStatement(stmtChild);
					URL urlChild = null;
					try {
						urlChild = new URL(surfaceGeometryId);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}

					psChild.setURL(1, urlChild);
					try (ResultSet rsChild = psChild.executeQuery()) {
						GeometryNode childGeometry = constructGeomNode(rsChild);
						geomNode.childNodes.add(childGeometry);
					}
				}
			}

			// put polygon into the geometry tree
			geomTree.insertNode(geomNode, geomNode.parentId);

			// interpret geometry tree as a single abstract geometry
			if (geomTree.root != null)
				return rebuildGeometry(geomTree.getNode(geomTree.root), false, false);
			else {
				exporter.logOrThrowErrorMessage("Failed to interpret geometry object.");
				return null;
			}
		}
	}

	protected GeometryNode constructGeomNode(ResultSet rs) throws SQLException, CityGMLExportException {
		GeometryNode geomNode = new GeometryNode();
		while (rs.next()) {
			String predicate = rs.getString("predicate");
			Boolean isBlank = rs.getBoolean("isblank");

			// constructing a geometry node
			if(predicate.contains("#id") && !isBlank){
				geomNode.id = rs.getString("value");
			}else if(predicate.contains("#gmlId")){
				geomNode.gmlId = rs.getString("value");
			}else if(predicate.contains("#parentId") && !isBlank) {
				geomNode.parentId = rs.getString("value");
			}else if(predicate.contains("#isSolid") && !isBlank) {
				if (rs.getInt("value") == 0)
					geomNode.isSolid = false;
				if (rs.getInt("value") == 1)
					geomNode.isSolid = true;
			}else if(predicate.contains("#isComposite") && !isBlank) {
				if (rs.getInt("value") == 0)
					geomNode.isComposite = false;
				if (rs.getInt("value") == 1)
					geomNode.isComposite = true;
			}else if(predicate.contains("#isTriangulated") && !isBlank) {
				if (rs.getInt("value") == 0)
					geomNode.isTriangulated = false;
				if (rs.getInt("value") == 1)
					geomNode.isTriangulated = true;
			}else if(predicate.contains("#isXlink") && !isBlank) {
				if (rs.getInt("value") == 0)
					geomNode.isXlink = false;
				if (rs.getInt("value") == 1)
					geomNode.isXlink = true;
			}else if(predicate.contains("#isReverse") && !isBlank) {
				if (rs.getInt("value") == 0)
					geomNode.isReverse = false;
				if (rs.getInt("value") == 1)
					geomNode.isReverse = true;
			}else if(predicate.contains("#GeometryType")) {
				GeometryObject geometry = null;
				if(!isBlank){
					try {
						Object object = rs.getObject("value");
						String datatype = rs.getString("datatype");
						Geometry geomS = GeoSpatialProcessor.createGeometry(object.toString(), datatype);
						Coordinate[] geomCoord = geomS.getCoordinates();
						double[] geomDouble = new double[geomCoord.length * 3];
						int newI = 0;
						for(int i = 0; i < geomCoord.length; i++){
							if (i == 0)
								geomDouble[newI] = geomCoord[i].getX();
							else
								geomDouble[++newI] = geomCoord[i].getX();

							geomDouble[++newI] = geomCoord[i].getY();
							geomDouble[++newI] = geomCoord[i].getZ();
						}
						int srid = exporter.getSrid();
						geometry = GeometryObject.createPolygon(geomDouble, 3, srid);
					} catch (Exception e) {
						exporter.logOrThrowErrorMessage(new StringBuilder("Skipping ").append(exporter.getGeometrySignature(GMLClass.POLYGON, geomNode.id))
								.append(": ").append(e.getMessage()).toString());
						continue;
					}
				}
				geomNode.geometry = geometry;
			}
		}
		return geomNode;
	}
	protected SurfaceGeometry doExportImplicitGeometry(long rootId) throws CityGMLExportException, SQLException {
		try {
			isImplicit = true;
			return doExport(rootId);
		} finally {
			isImplicit = false;
		}
	}

	private SurfaceGeometry rebuildGeometry(GeometryNode geomNode, boolean isSetOrientableSurface, boolean wasXlink) throws CityGMLExportException, SQLException {
		// try and determine the geometry type
		String srsName = exporter.getDatabaseAdapter().getConnectionMetaData().getReferenceSystem().getGMLSrsName();
		GMLClass surfaceGeometryType = null;
		if (geomNode.geometry != null) {
			surfaceGeometryType = GMLClass.POLYGON;
		} else {

			if (geomNode.childNodes == null || geomNode.childNodes.size() == 0)
				return null;

			if (!geomNode.isTriangulated) {
				if (!geomNode.isSolid && geomNode.isComposite)
					surfaceGeometryType = GMLClass.COMPOSITE_SURFACE;
				else if (geomNode.isSolid && !geomNode.isComposite)
					surfaceGeometryType = GMLClass.SOLID;
				else if (geomNode.isSolid && geomNode.isComposite)
					surfaceGeometryType = GMLClass.COMPOSITE_SOLID;
				else if (!geomNode.isSolid && !geomNode.isComposite) {
					boolean isMultiSolid = true;
					for (GeometryNode childNode : geomNode.childNodes) {
						if (!childNode.isSolid){
							isMultiSolid = false;
							break;
						}
					}

					if (isMultiSolid) 
						surfaceGeometryType = GMLClass.MULTI_SOLID;
					else
						surfaceGeometryType = GMLClass.MULTI_SURFACE;
				}
			} else
				surfaceGeometryType = GMLClass.TRIANGULATED_SURFACE;
		}

		// return if we cannot identify the geometry
		if (surfaceGeometryType == null)
			return null;

		// check for xlinks
		if (geomNode.gmlId != null) {
			if (geomNode.isXlink) {
				if (exporter.lookupAndPutGeometryUID(geomNode.gmlId, geomNode.id)) {

					if (useXLink) {
						// check whether we have to embrace the geometry with an orientableSurface
						if (geomNode.isReverse != isSetOrientableSurface) {
							OrientableSurface orientableSurface = new OrientableSurface();				
							SurfaceProperty surfaceProperty = new SurfaceProperty();
							surfaceProperty.setHref("#" + geomNode.gmlId); 
							orientableSurface.setBaseSurface(surfaceProperty);
							orientableSurface.setOrientation(Sign.MINUS);

							return new SurfaceGeometry(orientableSurface);
						} else
							return new SurfaceGeometry("#" + geomNode.gmlId, surfaceGeometryType);
					} else {
						geomNode.isXlink = false;
						String gmlId = DefaultGMLIdManager.getInstance().generateUUID(gmlIdPrefix);
						if (appendOldGmlId)
							gmlId = new StringBuilder(gmlId).append("-").append(geomNode.gmlId).toString();

						geomNode.gmlId = gmlId;
						return rebuildGeometry(geomNode, isSetOrientableSurface, true);
					}
				}
			} 

			if (exportAppearance && !wasXlink)
				writeToAppearanceCache(geomNode);
		}

		// check whether we have to initialize an orientableSurface
		boolean initOrientableSurface = false;
		if (geomNode.isReverse && !isSetOrientableSurface) {
			isSetOrientableSurface = true;
			initOrientableSurface = true;
		}

		// deal with geometry according to the identified type
		// Polygon
		if (surfaceGeometryType == GMLClass.POLYGON) {
			// try and interpret geometry object from database
			Polygon polygon = new Polygon();
			boolean forceRingIds = false;

			if (geomNode.gmlId != null) {
				polygon.setId(geomNode.gmlId);
				forceRingIds = true;
			}

			// we suppose we have one outer ring and one or more inner rings
			boolean isExterior = true;
			for (int ringIndex = 0; ringIndex < geomNode.geometry.getNumElements(); ringIndex++) {
				List<Double> values = null;

				// check whether we have to reverse the coordinate order
				if (!geomNode.isReverse) { 
					values = geomNode.geometry.getCoordinatesAsList(ringIndex);
				} else {
					values = new ArrayList<Double>(geomNode.geometry.getCoordinates(ringIndex).length);
					double[] coordinates = geomNode.geometry.getCoordinates(ringIndex);
					for (int i = coordinates.length - 3; i >= 0; i -= 3) {
						values.add(coordinates[i]);
						values.add(coordinates[i + 1]);
						values.add(coordinates[i + 2]);
					}
				}

				if (isExterior) {
					LinearRing linearRing = new LinearRing();
					DirectPositionList directPositionList = new DirectPositionList();

					if (forceRingIds)
						linearRing.setId(polygon.getId() + '_' + ringIndex + '_');

					directPositionList.setValue(values);
					directPositionList.setSrsDimension(3);
					linearRing.setPosList(directPositionList);

					Exterior exterior = new Exterior();
					exterior.setRing(linearRing);
					polygon.setExterior(exterior);

					isExterior = false;
				} else {
					LinearRing linearRing = new LinearRing();
					DirectPositionList directPositionList = new DirectPositionList();

					if (forceRingIds)
						linearRing.setId(polygon.getId() + '_' + ringIndex + '_');

					directPositionList.setValue(values);
					directPositionList.setSrsDimension(3);
					linearRing.setPosList(directPositionList);

					Interior interior = new Interior();
					interior.setRing(linearRing);
					polygon.addInterior(interior);
				}
			}

			// check whether we have to embrace the polygon with an orientableSurface
			if (initOrientableSurface || (isSetOrientableSurface && !geomNode.isReverse)) {
				OrientableSurface orientableSurface = new OrientableSurface();				
				SurfaceProperty surfaceProperty = new SurfaceProperty();
				surfaceProperty.setSurface(polygon);
				orientableSurface.setBaseSurface(surfaceProperty);
				orientableSurface.setOrientation(Sign.MINUS);

				return new SurfaceGeometry(orientableSurface);
			} else
				return new SurfaceGeometry(polygon);
		}

		// compositeSurface
		else if (surfaceGeometryType == GMLClass.COMPOSITE_SURFACE) {
			CompositeSurface compositeSurface = new CompositeSurface();

			if (geomNode.gmlId != null)
				compositeSurface.setId(geomNode.gmlId);

			for (GeometryNode childNode : geomNode.childNodes) {
				SurfaceGeometry geomMember = rebuildGeometry(childNode, isSetOrientableSurface, wasXlink);

				if (geomMember != null) {
					AbstractGeometry absGeom = geomMember.getGeometry();
					SurfaceProperty surfaceMember = new SurfaceProperty();

					if (absGeom != null) {
						switch (geomMember.getType()) {
						case POLYGON:
						case ORIENTABLE_SURFACE:
						case COMPOSITE_SURFACE:
						case TRIANGULATED_SURFACE:
							surfaceMember.setSurface((AbstractSurface)absGeom);
							break;						
						default:
							surfaceMember = null;
						}
					} else {
						surfaceMember.setHref(geomMember.getReference());
					}

					if (surfaceMember != null)
						compositeSurface.addSurfaceMember(surfaceMember);
				}
			}

			if (compositeSurface.isSetSurfaceMember()) {
				// check whether we have to embrace the compositeSurface with an orientableSurface
				if (initOrientableSurface || (isSetOrientableSurface && !geomNode.isReverse)) {
					OrientableSurface orientableSurface = new OrientableSurface();				
					SurfaceProperty surfaceProperty = new SurfaceProperty();
					surfaceProperty.setSurface(compositeSurface);
					orientableSurface.setBaseSurface(surfaceProperty);
					orientableSurface.setOrientation(Sign.MINUS);

					return new SurfaceGeometry(orientableSurface);
				} else
					return new SurfaceGeometry(compositeSurface);
			}					

			return null;
		}

		// compositeSolid
		else if (surfaceGeometryType == GMLClass.COMPOSITE_SOLID) {
			CompositeSolid compositeSolid = new CompositeSolid();

			if (geomNode.gmlId != null)
				compositeSolid.setId(geomNode.gmlId);

			for (GeometryNode childNode : geomNode.childNodes) {
				SurfaceGeometry geomMember = rebuildGeometry(childNode, isSetOrientableSurface, wasXlink);

				if (geomMember != null) {
					AbstractGeometry absGeom = geomMember.getGeometry();
					SolidProperty solidMember = new SolidProperty();

					if (absGeom != null) {					
						switch (geomMember.getType()) {
						case SOLID:
						case COMPOSITE_SOLID:
							solidMember.setSolid((AbstractSolid)absGeom);
							break;
						default:
							solidMember = null;
						}
					} else {
						solidMember.setHref(geomMember.getReference());
					}

					if (solidMember != null)
						compositeSolid.addSolidMember(solidMember);
				}
			}

			if (compositeSolid.isSetSolidMember())
				return new SurfaceGeometry(compositeSolid);

			return null;
		}

		// a simple solid
		else if (surfaceGeometryType == GMLClass.SOLID) {
			Solid solid = new Solid();

			if (geomNode.gmlId != null)
				solid.setId(geomNode.gmlId);

			// we strongly assume solids contain one single CompositeSurface
			// as exterior. Nothing else is interpreted here...
			if (geomNode.childNodes.size() == 1) {
				SurfaceGeometry geomMember = rebuildGeometry(geomNode.childNodes.get(0), isSetOrientableSurface, wasXlink);

				if (geomMember != null) {
					AbstractGeometry absGeom = geomMember.getGeometry();
					SurfaceProperty surfaceProperty = new SurfaceProperty();

					if (absGeom != null) {
						switch (geomMember.getType()) {
						case COMPOSITE_SURFACE:
						case ORIENTABLE_SURFACE:
							surfaceProperty.setSurface((AbstractSurface)absGeom);
							break;
						default:
							surfaceProperty = null;
						}
					} else {
						surfaceProperty.setHref(geomMember.getReference());
					}

					if (surfaceProperty != null)
						solid.setExterior(surfaceProperty);
				}
			}

			if (solid.isSetExterior())
				return new SurfaceGeometry(solid);

			return null;
		}

		// multiSolid
		else if (surfaceGeometryType == GMLClass.MULTI_SOLID) {
			MultiSolid multiSolid = new MultiSolid();

			if (geomNode.gmlId != null)
				multiSolid.setId(geomNode.gmlId);

			for (GeometryNode childNode : geomNode.childNodes) {
				SurfaceGeometry geomMember = rebuildGeometry(childNode, isSetOrientableSurface, wasXlink);

				if (geomMember != null) {
					AbstractGeometry absGeom = geomMember.getGeometry();
					SolidProperty solidMember = new SolidProperty();

					if (absGeom != null) {
						switch (geomMember.getType()) {
						case SOLID:
						case COMPOSITE_SOLID:
							solidMember.setSolid((AbstractSolid)absGeom);
							break;
						default:
							solidMember = null;
						}
					} else {
						solidMember.setHref(geomMember.getReference());
					}

					if (solidMember != null)
						multiSolid.addSolidMember(solidMember);
				}
			}

			if (multiSolid.isSetSolidMember())
				return new SurfaceGeometry(multiSolid);

			return null;

		}

		// multiSurface
		else if (surfaceGeometryType == GMLClass.MULTI_SURFACE){
			MultiSurface multiSurface = new MultiSurface();

			if (geomNode.gmlId != null)
				multiSurface.setId(geomNode.gmlId);
				multiSurface.setSrsDimension(3);
				multiSurface.setSrsName(srsName);

			for (GeometryNode childNode : geomNode.childNodes) {
				SurfaceGeometry geomMember = rebuildGeometry(childNode, isSetOrientableSurface, wasXlink);

				if (geomMember != null) {
					AbstractGeometry absGeom = geomMember.getGeometry();
					SurfaceProperty surfaceMember = new SurfaceProperty();

					if (absGeom != null) {
						switch (geomMember.getType()) {
						case POLYGON:
						case ORIENTABLE_SURFACE:
						case COMPOSITE_SURFACE:
						case TRIANGULATED_SURFACE:
							surfaceMember.setSurface((AbstractSurface)absGeom);
							break;
						default:
							surfaceMember = null;
						}
					} else {
						surfaceMember.setHref(geomMember.getReference());
					}

					if (surfaceMember != null)
						multiSurface.addSurfaceMember(surfaceMember);
				}
			}

			if (multiSurface.isSetSurfaceMember())
				return new SurfaceGeometry(multiSurface);

			return null;
		}

		// triangulatedSurface
		else if (surfaceGeometryType == GMLClass.TRIANGULATED_SURFACE) {
			TriangulatedSurface triangulatedSurface = new TriangulatedSurface();

			if (geomNode.gmlId != null)
				triangulatedSurface.setId(geomNode.gmlId);

			TrianglePatchArrayProperty triangleArray = new TrianglePatchArrayProperty();
			for (GeometryNode childNode : geomNode.childNodes) {
				SurfaceGeometry geomMember = rebuildGeometry(childNode, isSetOrientableSurface, wasXlink);

				if (geomMember != null) {
					// we are only expecting polygons...
					AbstractGeometry absGeom = geomMember.getGeometry();					

					if (geomMember.getType() == GMLClass.POLYGON) {
						// we do not have to deal with xlinks here...
						if (absGeom != null) {
							// convert polygon to trianglePatch
							Triangle triangle = new Triangle();
							Polygon polygon = (Polygon)absGeom;

							if (polygon.isSetExterior()) {								
								triangle.setExterior(polygon.getExterior());
								triangleArray.addTriangle(triangle);
							}							
						}
					}
				}
			}

			if (triangleArray.isSetTriangle() && !triangleArray.getTriangle().isEmpty()) {
				triangulatedSurface.setTrianglePatches(triangleArray);

				// check whether we have to embrace the compositeSurface with an orientableSurface
				if (initOrientableSurface || (isSetOrientableSurface && !geomNode.isReverse)) {
					OrientableSurface orientableSurface = new OrientableSurface();				
					SurfaceProperty surfaceProperty = new SurfaceProperty();
					surfaceProperty.setSurface(triangulatedSurface);
					orientableSurface.setBaseSurface(surfaceProperty);
					orientableSurface.setOrientation(Sign.MINUS);

					return new SurfaceGeometry(orientableSurface);
				} else
					return new SurfaceGeometry(triangulatedSurface);
			}

			return null;
		}

		return null;
	}

	private void writeToAppearanceCache(GeometryNode geomNode) throws SQLException {
		psImport.setObject(1, geomNode.id);
		psImport.setObject(2, geomNode.id);
		psImport.addBatch();
		batchCounter++;

		if (batchCounter == commitAfter) {
			psImport.executeBatch();
			batchCounter = 0;
		}
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

	@Override
	public void close() throws SQLException {
		psSelect.close();

		if (psImport != null) {
			psImport.executeBatch();
			psImport.close();
		}
	}

	private class GeometryNode {
		protected Object id;
		protected String gmlId;
		protected Object parentId;
		protected boolean isSolid;
		protected boolean isComposite;
		protected boolean isTriangulated;
		protected boolean isXlink;
		protected boolean isReverse;
		protected GeometryObject geometry;
		protected List<GeometryNode> childNodes;

		public GeometryNode() {
			childNodes = new ArrayList<GeometryNode>();
		}
	}

	private class GeometryTree {
		Object root;
		private HashMap<Object, GeometryNode> geometryTree;

		public GeometryTree() {
			geometryTree = new HashMap<Object, GeometryNode>();
		}

		public void insertNode(GeometryNode geomNode, Object parentId) {

			if (parentId == null)
				root = geomNode.id;

			if (geometryTree.containsKey(geomNode.id)) {

				// we have inserted a pseudo node previously
				// so fill that one with life...
				GeometryNode pseudoNode = geometryTree.get(geomNode.id);
				pseudoNode.id = geomNode.id;
				pseudoNode.gmlId = geomNode.gmlId;
				pseudoNode.parentId = geomNode.parentId;
				pseudoNode.isSolid = geomNode.isSolid;
				pseudoNode.isComposite = geomNode.isComposite;
				pseudoNode.isTriangulated = geomNode.isTriangulated;
				pseudoNode.isXlink = geomNode.isXlink;
				pseudoNode.isReverse = geomNode.isReverse;
				pseudoNode.geometry = geomNode.geometry;

				geomNode = pseudoNode;

			} else {
				// identify hierarchy nodes and place them
				// into the tree
				if (geomNode.geometry == null || parentId == null)
					geometryTree.put(geomNode.id, geomNode);
			}

			// make the node known to its parent...
			if (parentId != null) {
				GeometryNode parentNode = geometryTree.get(parentId);

				if (parentNode == null) {
					// there is no entry so far, so lets create a
					// pseudo node
					parentNode = new GeometryNode();
					geometryTree.put(parentId, parentNode);
				}

				parentNode.childNodes.add(geomNode);
			}
		}

		public GeometryNode getNode(Object entryId) {
			return geometryTree.get(entryId);
		}
	}
}
