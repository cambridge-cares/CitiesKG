package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.twa.cities.models.geo.Building;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;
import uk.ac.cam.cares.twa.cities.models.geo.ThematicSurface;

/**
 * A JPSAgent framework based ThematicSurfaceDiscoveryAgent class used to discover thematic surfaces
 * of city obejcts in the Semantic 3D City Database
 * {@link <a href="https://www.cares.cam.ac.uk/research/cities/">}.
 * The agent:
 * - listens to the GET requests containing IRIs of city objects
 * - checks the type of the city object
 * - attempts to load thematic surfaces of the obejct and
 *      - if present, returns them
 *      - if not present, determines thematic surfaces based on the city obejct type and surface
 *      geometries
 * - saves any newly discovered thematic surfaces back to the CKG
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
@WebServlet(
    urlPatterns = {
        ThematicSurfaceDiscoveryAgent.URI_LISTEN
    })
public class ThematicSurfaceDiscoveryAgent extends JPSAgent {

  public static final String URI_LISTEN = "/discovery/thematicsurface";
  public static final String KEY_COBI = "cityObjectIRI";
  private URI cityObjectURI;

  private KnowledgeBaseClientInterface kgClient; //AccessAgent should be used instead of this
  private static String route;
  private boolean lazyload;

  public ThematicSurfaceDiscoveryAgent() {
    super();
    readConfig();
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    if (validateInput(requestParams)) {
      requestParams = new JSONObject(getThematicSurfacesByCityObjectUri(cityObjectURI));
    }
    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    boolean error = true;
    if (!requestParams.isEmpty() && requestParams.get(CityImportAgent.KEY_REQ_METHOD).equals(HttpMethod.GET)) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_COBI)) {
        try {
          cityObjectURI = new URI(requestParams.getString(KEY_COBI));
          error = false;
        } catch (URISyntaxException e) {
          throw new BadRequestException();
        }
      }
    }
    return error;
  }

  private ArrayList<URI> getThematicSurfacesByCityObjectUri(URI cobu) {
    ArrayList<URI> uris = new ArrayList<>();
    setKGClient(true);

    try {
      CityObject cityObject = new CityObject();
      cityObject.fillScalars(cobu.toString(), kgClient);
      int type = cityObject.getObjectClassId();
      if (type == 26) { //generalise this
        Building building = new Building();
        building.fillThematicSurfaces(cityObject.getId().toString().replace("cityobject", "building"),
            kgClient, false);
        ArrayList<ThematicSurface> thsurf =  building.getThematicSurfaces();
        ArrayList<SurfaceGeometry> surfgeom; //@TODO implement getting surface geometries directly from buildings

        //@TODO discovery logic
      }

    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new JPSRuntimeException(e);
    }

    return uris;
  }

  /**
   * reads variable values relevant for CityInformationAgent class from config.properties file.
   */
  private void readConfig() {
    ResourceBundle config = ResourceBundle.getBundle("config");
    lazyload = Boolean.getBoolean(config.getString("loading.status"));
    route = config.getString("uri.route");
  }

  /**
   * sets KG Client for specific endpoint.
   * @param isQuery boolean
   */
  private void setKGClient(boolean isQuery) {
    this.kgClient = KGRouter.getKnowledgeBaseClient(route, isQuery, !isQuery);
  }

}
