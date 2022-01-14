package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;

import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.twa.cities.PrefixUtils;
import uk.ac.cam.cares.twa.cities.models.geo.*;
import uk.ac.cam.cares.twa.cities.tasks.geo.ThematicisationTask;

/**
 * A JPSAgent framework based ThematicSurfaceDiscoveryAgent class used to discover thematic surfaces
 * of city obejcts in the Semantic 3D City Database {@link <a
 * href="https://www.cares.cam.ac.uk/research/cities/">}. The agent: - listens to the GET requests
 * containing IRIs of city objects - checks the type of the city object - attempts to load thematic
 * surfaces of the obejct and - if present, returns them - if not present, determines thematic
 * surfaces based on the city obejct type and surface geometries - saves any newly discovered
 * thematic surfaces back to the CKG
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
@WebServlet(urlPatterns = {ThematicSurfaceDiscoveryAgent.URI_LISTEN})
public class ThematicSurfaceDiscoveryAgent extends JPSAgent {

  private ExecutorService executor = Executors.newSingleThreadExecutor();

  public static final String URI_LISTEN = "/discovery/thematicsurface";
  public static final String KEY_NAMESPACE = "namespace";
  public static final String KEY_COBI = "cityObjectIRI";
  public static final String KEY_LOD = "lod";
  public static final String KEY_THRESHOLD = "thresholdAngle";

  private static final String METRIC_SRS = "EPSG:25833";

  private static final String NO_CRS_ERROR_TEXT = "Namespace has no CRS specified.";
  private static final String MULTIPLE_CRS_ERROR_TEXT = "Namespace has more than one CRS specified.";

  private static final String route = ResourceBundle.getBundle("config").getString("uri.route");
  private final StoreClientInterface kgClient = new RemoteStoreClient(route, route); // AccessAgent should be used instead of this

  private URI buildingIri;
  private URI namespaceIri;
  private boolean[] thematiciseLod;
  private double threshold;

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(CityImportAgent.KEY_REQ_METHOD).equals(HttpMethod.GET)) {
      Set<String> keys = requestParams.keySet();
      try {
        if (keys.contains(KEY_THRESHOLD)) {
          threshold = requestParams.getDouble(KEY_THRESHOLD);
        } else {
          threshold = 15;
        }
        if (keys.contains(KEY_NAMESPACE)) {
          namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE));
          buildingIri = keys.contains(KEY_COBI) ? new URI(requestParams.getString(KEY_COBI)) : null;
          thematiciseLod = new boolean[4];
          for(int i = 0; i < 3; i++) {
            thematiciseLod[i] = keys.contains(KEY_LOD + (i+1)) && requestParams.getInt(KEY_LOD + (i+1)) == 1;
          }
          if(!(thematiciseLod[0] || thematiciseLod[1] || thematiciseLod[2] || thematiciseLod[3])) {
            thematiciseLod[0] = thematiciseLod[1] = thematiciseLod[2] = thematiciseLod[3] = true;
          }
          return true;
        }
      } catch (URISyntaxException e) {
        throw new BadRequestException(e);
      }
    }
    throw new BadRequestException();
  }

  private void importSrs() throws JPSRuntimeException{
    JSONArray srsResponse = new JSONArray(kgClient.execute(PrefixUtils.insertPrefixStatements(
        String.format("SELECT ?srs WHERE {<%s> ocgml:srsname ?srs }", namespaceIri.toString()))));
    if (srsResponse.length() == 0) {
      throw new JPSRuntimeException(NO_CRS_ERROR_TEXT);
    } else if (srsResponse.length() > 1) {
      throw new JPSRuntimeException(MULTIPLE_CRS_ERROR_TEXT);
    } else {
      GeometryType.setSourceCrsName(srsResponse.getJSONObject(0).getString("srs"));
      GeometryType.setMetricCrsName(METRIC_SRS);
    }
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    requestParams.put("acceptHeaders", "application/json");
    importSrs();
    // If building IRI specified, use that; else, fetch all buildings in namespace.
    List<String> buildingIris = new ArrayList<>();
    if (buildingIri != null) {
      buildingIris.add(buildingIri.toString());
    } else {
      JSONArray buildingsResponse = new JSONArray(kgClient.execute(
          PrefixUtils.insertPrefixStatements(String.format(
              "SELECT ?bldg WHERE {?bldg %s \"26\"^^xsd:integer; %s ?val. }",
              SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID,
              SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID))));
      for(int i = 0; i < buildingsResponse.length(); i++)
        buildingIris.add(buildingsResponse.getJSONObject(i).getString("bldg"));
    }
    executor.execute(new ThematicisationTask(buildingIris, thematiciseLod, threshold, kgClient));
    return requestParams;
  }


}
