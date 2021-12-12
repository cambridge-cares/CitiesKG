package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteKnowledgeBaseClient;
import uk.ac.cam.cares.twa.cities.models.geo.Building;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;

/**
 * A JPSAgent framework based ThematicSurfaceDiscoveryAgent class used to discover thematic surfaces
 * of city obejcts in the Semantic 3D City Database {@link <a
 * href="https://www.cares.cam.ac.uk/research/cities/">}. The agent: - listens to the GET requests
 * containing IRIs of city objects - checks the type of the city object - attempts to load thematic
 * surfaces of the obejct and - if present, returns them - if not present, determines thematic
 * surfaces based on the city obejct type and surface geometries - saves any newly discovered
 * thematic surfaces back to the CKG
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
@WebServlet(urlPatterns = {ThematicSurfaceDiscoveryAgent.URI_LISTEN})
public class ThematicSurfaceDiscoveryAgent extends JPSAgent {

  public static final String URI_LISTEN = "/discovery/thematicsurface";
  public static final String KEY_COBI = "cityObjectIRI";
  private URI cityObjectURI;

  private KnowledgeBaseClientInterface kgClient; // AccessAgent should be used instead of this
  private static String route;
  private boolean lazyload;

  public ThematicSurfaceDiscoveryAgent() {
    super();
    readConfig();
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    requestParams.append("lodXMultiSurfaces", getThematicSurfacesByCityObjectUri(cityObjectURI));
    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(CityImportAgent.KEY_REQ_METHOD).equals(HttpMethod.GET)) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_COBI)) {
        try {
          System.out.println(requestParams.getString(KEY_COBI));
          cityObjectURI = new URI(requestParams.getString(KEY_COBI));
          return true;
        } catch (URISyntaxException e) {
          throw new BadRequestException();
        }
      }
    }
    throw new BadRequestException();
  }

  private ArrayList<String> getThematicSurfacesByCityObjectUri(URI cobu) {
    setKGClient(true);
    CityObject cityObject = new CityObject();
    cityObject.populateAll(cobu.toString(), kgClient, 1);
    int type = cityObject.getObjectClassId();
    if (type == 26) { // generalise this
      String buildingIriName = cityObject.getIri().toString().replace("cityobject", "building");
      Building bldg = new Building();
      bldg.populateAll(buildingIriName, kgClient, 99);
      ArrayList<String> geometries = new ArrayList<>();
      ArrayList<SurfaceGeometry> queue = new ArrayList<>();
      if(bldg.getLod1MultiSurfaceId() != null) queue.add(bldg.getLod1MultiSurfaceId());
      if(bldg.getLod2MultiSurfaceId() != null) queue.add(bldg.getLod2MultiSurfaceId());
      while(queue.size() > 0){
        geometries.add(queue.get(0).getIri().toString());
        queue.addAll(queue.get(0).getSurfaceGeometries());
        queue.remove(0);
      }
      return geometries;
    } else {
      return new ArrayList<>(Arrays.asList(cityObject.getGmlId(), Integer.toString(cityObject.getObjectClassId())));
    }
  }

  /** reads variable values relevant for CityInformationAgent class from config.properties file. */
  private void readConfig() {
    ResourceBundle config = ResourceBundle.getBundle("config");
    lazyload = Boolean.getBoolean(config.getString("loading.status"));
    route = config.getString("uri.route");
  }

  /**
   * sets KG Client for specific endpoint.
   *
   * @param isQuery boolean
   */
  private void setKGClient(boolean isQuery) {
    // this.kgClient = KGRouter.getKnowledgeBaseClient(route, isQuery, !isQuery);
    this.kgClient = new RemoteKnowledgeBaseClient(route, route);
  }
}
