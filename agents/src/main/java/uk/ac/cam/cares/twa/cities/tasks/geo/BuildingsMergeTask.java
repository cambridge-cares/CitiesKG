package uk.ac.cam.cares.twa.cities.tasks.geo;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.locationtech.jts.geom.*;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;

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
        Map<String, org.locationtech.jts.geom.Geometry> footprints = new HashMap<>();
        try{
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
                Collection<org.locationtech.jts.geom.Geometry> footPolyList = new ArrayList<>();
                GeometryFactory factory = new GeometryFactory();
                for (int i = 0; i<footprintChilds.size(); i++){
                    if (footprintChilds.get(i).getGeometryType() != null)
                    {
                        Polygon foot = footprintChilds.get(i).getGeometryType().getPolygon();
                        footPolyList.add(foot.getBoundary());
                    }
                }
                GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry( footPolyList );
                org.locationtech.jts.geom.Geometry pMerge = geometryCollection.union();
//                System.out.println(pMerge.isValid());
                footprints.put(iri, pMerge);
            }
        }
        //2. check intersection of surface in list mergeBuildingId
        Iterator<Map.Entry<String, org.locationtech.jts.geom.Geometry>> iter1 = footprints.entrySet().iterator();

            while (iter1.hasNext()) {
                Map.Entry<String, org.locationtech.jts.geom.Geometry> next1 = iter1.next();
                org.locationtech.jts.geom.Geometry polygon1 = next1.getValue();
                List<String> merges = new ArrayList<>();
                Iterator<Map.Entry<String, org.locationtech.jts.geom.Geometry>> iter2 = iter1;
                int count1 = 0;
                int count2 = 0;
                while (iter2.hasNext()) {
                    Map.Entry<String, org.locationtech.jts.geom.Geometry> next2 = iter2.next();
                    org.locationtech.jts.geom.Geometry polygon2 =next2.getValue();
                    if(next1.getKey() != next2.getKey()){
                        if (polygon1.intersects(polygon2)||polygon1.touches(polygon2)||polygon1.overlaps(polygon2)){
                            count1++;
                            System.out.println("intersection:" + count1);
                            merges.add(next2.getKey());
                        }else{
                            count2++;
                            System.out.println("no relation:" + count2);}

                    }
                }
                if (merges.size() > 0){
                    merges.add(next1.getKey());
                    mergeBuildingId.add(merges);
                }
            }
        }catch (Exception e){
            System.err.println(e);
        }



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
