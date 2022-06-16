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

import java.util.ArrayList;
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
        List<Building> bldgList = new ArrayList<>();
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();

        for (String buildingIri : buildingIris) {
//            WhereBuilder where = new WhereBuilder();
//            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_ENVELOPE_TYPE, where);
//            String cityObjectIri = buildingIri.replace("building", "cityobject");
//            where.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_ID, NodeFactory.createURI(cityObjectIri));
//            cityObjectsList.addAll(context.pullAllWhere(CityObject.class, where));

            SelectBuilder srsQuery = new SelectBuilder();
            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_FOOTPRINT_ID, srsQuery);
            srsQuery.addVar("?model").addWhere(NodeFactory.createURI(buildingIri), SchemaManagerAdapter.ONTO_FOOTPRINT_ID, "?model");
            JSONArray srsResponse = context.query(srsQuery.buildString());

            if (!srsResponse.isEmpty()){

            }

//            WhereBuilder whereBldg = new WhereBuilder();
//            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_FOOTPRINT_ID, whereBldg);
//            whereBldg.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_ID, NodeFactory.createURI(buildingIri));
//            bldgList.addAll(context.pullAllWhere(Building.class, whereBldg));
        }
        //1. find intersect envelops
        for(int i = 0; i < bldgList.size(); i++){
            List<String> merges = new ArrayList<>();
            List<SurfaceGeometry> childFootprint = new ArrayList<>();
//            EnvelopeType envelop1 = cityObjectsList.get(i).getEnvelopeType();
//            org.locationtech.jts.geom.Polygon polygon1 =  envelop1.getPolygon();

            String footPrintId = bldgList.get(i).getLod0FootprintId().getIri();
            WhereBuilder whereFoot = new WhereBuilder();
            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_FOOTPRINT_ID, whereFoot);
            whereFoot.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_ID, NodeFactory.createURI(footPrintId));
            childFootprint.addAll(context.pullAllWhere(SurfaceGeometry.class, whereFoot));

            org.locationtech.jts.geom.Polygon[] footPolys = null;
            LinearRingType footLinear = new LinearRingType();
            List<String> cords =  new ArrayList<>();
            for (int k = 0; k < childFootprint.size(); k++){
//                Coordinate[] coordinates = geospatial.str2coords(childFootprint.get(k).getGeometryType().toString()).toArray(new Coordinate[0]);
//                cords.add(coordinates.toString());
                footPolys[k] = childFootprint.get(k).getGeometryType().getPolygon();
            }
            GeometryFactory factory = new GeometryFactory();
            MultiPolygon polygon1 = new MultiPolygon(footPolys, factory);

            for(int j = i+1; j < cityObjectsList.size()-i; j++){
//                EnvelopeType envelop2 = cityObjectsList.get(j).getEnvelopeType();
//                org.locationtech.jts.geom.Polygon polygon2 =  envelop2.getPolygon();
                org.locationtech.jts.geom.Polygon polygon2 = bldgList.get(j).getLod0FootprintId().getGeometryType().getPolygon();
                if (!polygon2.disjoint(polygon1)){

                    merges.add(cityObjectsList.get(j).getIri());
                }
            }
            if (merges.size() > 0){
                merges.add(cityObjectsList.get(i).getIri());
                mergeBuildingId.add(merges);
            }
        }

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
