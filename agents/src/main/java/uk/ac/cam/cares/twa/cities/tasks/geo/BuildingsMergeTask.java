package uk.ac.cam.cares.twa.cities.tasks.geo;

import com.google.common.collect.Iterables;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.json.JSONArray;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opengis.feature.simple.SimpleFeatureType;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.net.URI;
import java.time.OffsetDateTime;
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
                    Collection<Geometry> footPolyList = new ArrayList<>();
                    GeometryFactory factory = new GeometryFactory();
                    for (int i = 0; i<footprintChilds.size(); i++){
                        if (footprintChilds.get(i).getGeometryType() != null)
                        {
                            Polygon foot = footprintChilds.get(i).getGeometryType().getPolygon();
                            Coordinate[] coordinates = foot.getCoordinates();
                            footPolyList.add(inflate(factory.createPolygon(coordinates)));
                        }
                    }
                    if(footPolyList.size()==1){
                        footprints.put(buildingIri, Iterables.get(footPolyList,0));
                    }else if(footPolyList.size() > 1){
                        GeometryCollection geometryCollection1 = (GeometryCollection) factory.buildGeometry(footPolyList);
                        Geometry pMerge1 = geometryCollection1.union();
                        footprints.put(buildingIri, deflate(pMerge1));
                    }

                }
            }
        //2. check intersection of surface in list mergeBuildingId
            Map<String, org.locationtech.jts.geom.Geometry> footpritsCopy = footprints;
            Iterator<Map.Entry<String, org.locationtech.jts.geom.Geometry>> iter1 = footprints.entrySet().iterator();

            while (iter1.hasNext()) {
                Map.Entry<String, org.locationtech.jts.geom.Geometry> next1 = iter1.next();
                org.locationtech.jts.geom.Geometry polygon1 = next1.getValue();
                List<String> merges = new ArrayList<>();
                Iterator<Map.Entry<String, org.locationtech.jts.geom.Geometry>> iter2 = footpritsCopy.entrySet().iterator();
                if(isContains(next1.getKey())) continue;
                while (iter2.hasNext()) {
                    Map.Entry<String, org.locationtech.jts.geom.Geometry> next2 = iter2.next();
                    if(isContains(next2.getKey())) continue;
                    org.locationtech.jts.geom.Geometry polygon2 =next2.getValue();
                    if(next1.getKey() != next2.getKey()){
                        boolean intersect = polygon1.getEnvelope().intersects(polygon2.getEnvelope());
                        boolean overlaps = polygon1.getEnvelope().overlaps(polygon2.getEnvelope());
//                        boolean touches = polygon1.getEnvelope().touches(polygon2.getEnvelope());
                        boolean contains = polygon1.getEnvelope().contains(polygon2.getEnvelope());
                        if (intersect||overlaps||contains){
                            merges.add(next2.getKey());
                        }
                    }
                }
                if (merges.size() > 0){
                    merges.add(next1.getKey());
                    mergeBuildingId.add(merges);
                }
            }



        for(int i = 0; i < mergeBuildingId.size(); i++) {
            List<String> buildings = mergeBuildingId.get(i);
            int maxNumSurface = 0;
            String baseBuildingIri = null;
            String baseRootSurfaceIri = null;
            for (int j = 0; j < buildings.size(); j++) {
                String buildingIri = buildings.get(j);
                Building buildingModel = context.loadAll(Building.class, buildingIri);

                String rootURI = null;
                if (buildingModel.getLod1MultiSurfaceId() != null)
                    rootURI = buildingModel.getLod1MultiSurfaceId().getIri();
                else if (buildingModel.getLod2MultiSurfaceId() != null)
                    rootURI = buildingModel.getLod2MultiSurfaceId().getIri();
                else if (buildingModel.getLod3MultiSurfaceId() != null)
                    rootURI = buildingModel.getLod3MultiSurfaceId().getIri();
                SurfaceGeometry surfaceModel = context.getModel(SurfaceGeometry.class, rootURI);
                if (surfaceModel.getChildGeometries().size() == 0) {
                    ArrayList<SurfaceGeometry> childrenSurface = new ArrayList<>();
                    ModelContext childContext = params.makeContext();
                    childrenSurface.addAll(childContext.pullAllWhere(
                            SurfaceGeometry.class,
                            new WhereBuilder().addWhere(
                                    ModelContext.getModelVar(),
                                    NodeFactory.createURI(SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_ROOT_ID)),
                                    NodeFactory.createURI(rootURI)
                            )
                    ));
                    context.getModel(SurfaceGeometry.class, rootURI).setChildGeometries(childrenSurface);
                }

                int numSurface = surfaceModel.getChildGeometries().size();
                if (numSurface > maxNumSurface) {
                    maxNumSurface = numSurface;
                    baseBuildingIri = buildingIri;
                    baseRootSurfaceIri = rootURI;
                }
            }
            //Begin to change data: 1. merge childrenSurface to base building; 2. set new envelope; 4. delete old data (building, cityobject and genericattribute)
                for (int j = 0; j < buildings.size(); j++) {
                    String buildingIri = buildings.get(j);
                    if (buildingIri == baseBuildingIri) continue;
                    Building buildingModel = context.getModel(Building.class, buildingIri);
                    String rootURI = null;
                    if (buildingModel.getLod1MultiSurfaceId() != null)
                        rootURI = buildingModel.getLod1MultiSurfaceId().getIri();
                    else if (buildingModel.getLod2MultiSurfaceId() != null)
                        rootURI = buildingModel.getLod2MultiSurfaceId().getIri();
                    else if (buildingModel.getLod3MultiSurfaceId() != null)
                        rootURI = buildingModel.getLod3MultiSurfaceId().getIri();
                    SurfaceGeometry surfaceModel = context.getModel(SurfaceGeometry.class, rootURI);
                    SurfaceGeometry baseSurfaceModel = context.getModel(SurfaceGeometry.class, baseRootSurfaceIri);
                    ArrayList<SurfaceGeometry> baseChildren = baseSurfaceModel.getChildGeometries();
                    ArrayList<SurfaceGeometry> children = surfaceModel.getChildGeometries();
                    surfaceModel.setCityObjectId(URI.create(baseBuildingIri));
                    for(SurfaceGeometry child : children){
                        child.setCityObjectId(URI.create(baseBuildingIri));
                        child.setParentId(baseSurfaceModel);
                        child.setRootId(URI.create(baseRootSurfaceIri));
                        context.pushChanges(child);
                    }

                    String baseCityobjectId = baseBuildingIri.replace("building", "cityobject");
                    CityObject cityObjectModel = context.loadAll(CityObject.class, baseCityobjectId);
                    ArrayList<SurfaceGeometry> envelopSurface = new ArrayList<>();
                    for (SurfaceGeometry surface : baseChildren){
                        if (surface.getGeometryType() != null){
                            envelopSurface.add(surface);
                        }
                    }
                    cityObjectModel.setEnvelopeType(new EnvelopeType(envelopSurface));
                    cityObjectModel.setLastModificationDate(OffsetDateTime.now().toString());
                    cityObjectModel.setUpdatingPerson("ThematicSurfaceDiscoveryAgent");

                    String deleteCityobjectId = buildingIri.replace("building", "cityobject");
                    CityObject deleteCityObjectModel = context.loadAll(CityObject.class, deleteCityobjectId);
                    ArrayList<GenericAttribute> genAttribModel = deleteCityObjectModel.getGenericAttributes();
                    for (GenericAttribute oldGenericModel : genAttribModel) {
                        context.delete(oldGenericModel,true);
                    }
                    context.delete(surfaceModel, true);
                    context.delete(buildingModel,true);
                    context.delete(deleteCityObjectModel,true);
                    context.pushAllChanges();

                }

        }

        }catch (Exception e){
            System.err.println(e);
        }
        return null;
    }

    private Geometry deflate(Geometry geom) {
        BufferParameters bufferParameters = new BufferParameters();
        bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
        bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
        Geometry buffered = BufferOp.bufferOp(geom, -.0001, bufferParameters);
        buffered.setUserData(geom.getUserData());
        return buffered;
    }

    private Geometry inflate(Geometry geom) {
        BufferParameters bufferParameters = new BufferParameters();
        bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
        bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
        Geometry buffered = BufferOp.bufferOp(geom, .0001, bufferParameters);
        buffered.setUserData(geom.getUserData());
        return buffered;
    }

    private  boolean isContains (String key){
        for (List<String> idsMerge : mergeBuildingId){
            if (idsMerge.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
