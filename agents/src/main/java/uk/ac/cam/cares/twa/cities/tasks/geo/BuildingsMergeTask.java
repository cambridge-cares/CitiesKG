package uk.ac.cam.cares.twa.cities.tasks.geo;

import net.opengis.citygml.building._1.BuildingType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.PolygonType;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.config.geometry.MultiPoint;
import org.citydb.config.geometry.Polygon;
import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Query envelope of building and check spatial relationship of every building for merging.
 * @author Jingya Yan</a>
 * @version $Id$
 */
public class BuildingsMergeTask implements Callable<Void> {

    private final List<String> buildingIris;
    private final ModelContext context;
    private final ThematicSurfaceDiscoveryAgent.Params params;

    private final List<List<String>> mergeBuildingId = new ArrayList<>();

    public BuildingsMergeTask(List<String> buildingIris, ThematicSurfaceDiscoveryAgent.Params params) {
        this.buildingIris = buildingIris;
        this.context = params.makeContext();
        this.params = params;
    }

    public Void call() {
        List<CityObject> cityObjectsList = new ArrayList<>();
        List<String> footpringList = new ArrayList<>();
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        HashMap<String, MultiPolygon> footprints = new HashMap<>();
        for (String buildingIri : buildingIris) {
            //1. find lod0footprintId
            SelectBuilder srsQuery = new SelectBuilder();
            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_FOOTPRINT_ID, srsQuery);
            srsQuery.addVar("?model").addWhere(NodeFactory.createURI(buildingIri), SchemaManagerAdapter.ONTO_FOOTPRINT_ID, "?model");
            JSONArray srsResponse = context.query(srsQuery.buildString());

            if (!srsResponse.isEmpty()){
                String iri = srsResponse.getJSONObject(0).getString("model");
                footpringList.add(iri);
                context.createHollowModel(SurfaceGeometry.class, iri);
                context.getModel(SurfaceGeometry.class, iri).setCityObjectId(URI.create(buildingIri));
                ArrayList<SurfaceGeometry> footprintChilds = new ArrayList<>();
                ModelContext childContext = params.makeContext();
                footprintChilds.addAll(childContext.pullAllWhere(
                        SurfaceGeometry.class,
                        new WhereBuilder().addWhere(
                                ModelContext.getModelVar(),
                                NodeFactory.createURI(SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_ROOT_ID)),
                                NodeFactory.createURI(iri)
                        )
                ));
                context.getModel(SurfaceGeometry.class, iri).setChildGeometries(footprintChilds);
                List<org.locationtech.jts.geom.Polygon> footPolyList = new ArrayList<>();
                for (int i = 0; i<footprintChilds.size(); i++){
                    if (footprintChilds.get(i).getGeometryType() != null)
                    {
                        footPolyList.add(footprintChilds.get(i).getGeometryType().getPolygon());
                    }
                }
                org.locationtech.jts.geom.Polygon[] footPolys = footPolyList.stream().toArray(org.locationtech.jts.geom.Polygon[]::new);
                GeometryFactory factory = new GeometryFactory();
                MultiPolygon polygon = new MultiPolygon(footPolys, factory);
                footprints.put(iri, polygon);
            }
        }
        //2. polygon of footprint

//        for(String footprintId : footpringList){
//            List<String> merges = new ArrayList<>();
//            List<SurfaceGeometry> childFootprint = context.getModel(SurfaceGeometry.class, footprintId).getChildGeometries();
//////            EnvelopeType envelop1 = cityObjectsList.get(i).getEnvelopeType();
//////            org.locationtech.jts.geom.Polygon polygon1 =  envelop1.getPolygon();
////
////            String footPrintId = bldgList.get(i).getLod0FootprintId().getIri();
////            WhereBuilder whereFoot = new WhereBuilder();
////            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_FOOTPRINT_ID, whereFoot);
////            whereFoot.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_ID, NodeFactory.createURI(footPrintId));
////            childFootprint.addAll(context.pullAllWhere(SurfaceGeometry.class, whereFoot));
////
//            org.locationtech.jts.geom.Polygon[] footPolys = null;
////            LinearRingType footLinear = new LinearRingType();
////            List<String> cords =  new ArrayList<>();
//            for (int i = 0; i < childFootprint.size(); i++){
////                Coordinate[] coordinates = geospatial.str2coords(childFootprint.get(k).getGeometryType().toString()).toArray(new Coordinate[0]);
////                cords.add(coordinates.toString());
//                footPolys[i] = childFootprint.get(i).getGeometryType().getPolygon();
//            }
//            GeometryFactory factory = new GeometryFactory();
//            MultiPolygon polygon1 = new MultiPolygon(footPolys, factory);
//
//            for(int j = i+1; j < cityObjectsList.size()-i; j++){
////                EnvelopeType envelop2 = cityObjectsList.get(j).getEnvelopeType();
////                org.locationtech.jts.geom.Polygon polygon2 =  envelop2.getPolygon();
//                org.locationtech.jts.geom.Polygon polygon2 = bldgList.get(j).getLod0FootprintId().getGeometryType().getPolygon();
//                if (!polygon2.disjoint(polygon1)){
//
//                    merges.add(cityObjectsList.get(j).getIri());
//                }
//            }
////            if (merges.size() > 0){
////                merges.add(cityObjectsList.get(i).getIri());
////                mergeBuildingId.add(merges);
////            }
//        }

        //2. check intersection of surface in list mergeBuildingId
        for(int i = 0; i < mergeBuildingId.size(); i++){
            List<String> buildings = mergeBuildingId.get(i);
            for(int j = 0; j < buildings.size(); j++){
                String cityObjectId = buildings.get(j);
                String buildingId = cityObjectId.replace("cityobject", "building");


            }
        }

        return null;
    }
}
