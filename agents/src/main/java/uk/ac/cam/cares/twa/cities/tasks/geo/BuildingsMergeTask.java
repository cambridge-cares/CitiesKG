package uk.ac.cam.cares.twa.cities.tasks.geo;

import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.config.geometry.Polygon;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.twa.cities.models.geo.EnvelopeType;
import uk.ac.cam.cares.twa.cities.models.geo.ThematicSurface;

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

        for (String buildingIri : buildingIris) {
            WhereBuilder where = new WhereBuilder();
            SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_ENVELOPE_TYPE, where);
            String cityObjectIri = buildingIri.replace("building", "cityobject");
            where.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_ID, NodeFactory.createURI(cityObjectIri));
            cityObjectsList.addAll(context.pullAllWhere(CityObject.class, where));
        }

        for(int i = 0; i < cityObjectsList.size(); i++){
            List<String> merges = new ArrayList<>();
            EnvelopeType envelop1 = cityObjectsList.get(i).getEnvelopeType();
            org.locationtech.jts.geom.Polygon polygon1 =  envelop1.getPolygon();
            for(int j = i+1; j < cityObjectsList.size()-i; j++){
                EnvelopeType envelop2 = cityObjectsList.get(j).getEnvelopeType();
                org.locationtech.jts.geom.Polygon polygon2 =  envelop2.getPolygon();
                if (!polygon2.disjoint(polygon1)){
                    merges.add(cityObjectsList.get(j).getIri());
                }
            }
            if (merges != null){
                merges.add(cityObjectsList.get(i).getIri());
                mergeBuildingId.add(merges);
            }
        }
        return null;
    }
}
