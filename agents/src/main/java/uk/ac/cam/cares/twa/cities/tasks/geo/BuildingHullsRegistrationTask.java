package uk.ac.cam.cares.twa.cities.tasks.geo;

import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.ogm.models.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.ogm.models.geo.Building;
import uk.ac.cam.cares.ogm.models.geo.ThematicSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queries a building for its lodXMultiSurfaces and creates and queues a {@link MultiSurfaceThematicisationTask} for
 * each multi-surface of level of detail requested in the <code>lods</code> argument.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public class BuildingHullsRegistrationTask implements Callable<Void> {

  private final String buildingIri;
  private final ModelContext context;
  private final ThematicSurfaceDiscoveryAgent.Params params;
  private final ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> outputQueue;

  public BuildingHullsRegistrationTask(String buildingIri, ThematicSurfaceDiscoveryAgent.Params params, ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> outputQueue) {
    this.buildingIri = buildingIri;
    this.context = params.makeContext();
    this.params = params;
    this.outputQueue = outputQueue;
  }

  public Void call() {
    // Pull thematic surfaces belonging to the target building
    WhereBuilder where = new WhereBuilder();
    SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_BUILDING_ID, where);
    where.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_BUILDING_ID, NodeFactory.createURI(buildingIri));
    List<ThematicSurface> thematicSurfaces = context.pullPartialWhere(ThematicSurface.class, where,
        "lod2MultiSurfaceId", "lod3MultiSurfaceId", "lod4MultiSurfaceId");

    if (params.mode == ThematicSurfaceDiscoveryAgent.Mode.VALIDATE ||
            (params.mode == ThematicSurfaceDiscoveryAgent.Mode.FOOTPRINT && thematicSurfaces.size() > 0)) {
      // One root list for each level of detail from 2-4
      List<List<String>> roots = new ArrayList<>();
      for(int i = 0; i < 3; i++) roots.add(new ArrayList<>());
      // Populate virtual parents with thematic surface multisurfaces
      for (ThematicSurface themSurf: thematicSurfaces) {
        if(params.lods[1] && themSurf.getLod2MultiSurfaceId() != null)
          roots.get(0).add(themSurf.getLod2MultiSurfaceId().getIri());
        if(params.lods[2] && themSurf.getLod3MultiSurfaceId() != null)
          roots.get(1).add(themSurf.getLod3MultiSurfaceId().getIri());
        if(params.lods[3] && themSurf.getLod4MultiSurfaceId() != null)
          roots.get(2).add(themSurf.getLod4MultiSurfaceId().getIri());
      }
      // Push tasks for each non-empty virtual parent
      for(int i = 0; i < roots.size(); i++)
        if(roots.get(i).size() > 0)
          outputQueue.add(new MultiSurfaceThematicisationTask(i+1, params, roots.get(i).toArray(new String[0])));
    } else {
      Building building = context.loadPartial(Building.class, buildingIri,
          "lod1MultiSurfaceId", "lod2MultiSurfaceId", "lod3MultiSurfaceId", "lod4MultiSurfaceId");
      if(params.lods[0] && building.getLod1MultiSurfaceId() != null)
        outputQueue.add(new MultiSurfaceThematicisationTask(1, params, building.getLod1MultiSurfaceId().getIri()));
      if(params.lods[1] && building.getLod2MultiSurfaceId() != null)
        outputQueue.add(new MultiSurfaceThematicisationTask(1, params, building.getLod2MultiSurfaceId().getIri()));
      if(params.lods[2] && building.getLod3MultiSurfaceId() != null)
        outputQueue.add(new MultiSurfaceThematicisationTask(1, params, building.getLod3MultiSurfaceId().getIri()));
      if(params.lods[3] && building.getLod4MultiSurfaceId() != null)
        outputQueue.add(new MultiSurfaceThematicisationTask(1, params, building.getLod4MultiSurfaceId().getIri()));
    }
    return null;
  }

}
