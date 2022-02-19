package uk.ac.cam.cares.twa.cities.agents.geo;

import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.Building;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@WebServlet(urlPatterns = {ThematicSurfaceDiscoveryAgent.URI_LISTEN})
public class UPRNAgent extends JPSAgent {


  private final String targetResourceId;
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_NAMESPACE = "namespace";
  public static final String KEY_BUILDING = "cityObjectIRI";
  @Getter private String buildingIri;
  @Getter private String namespaceIri;

  public UPRNAgent() {
    super();
    targetResourceId = ResourceBundle.getBundle("config").getString("uri.route");
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    List<Building> buildings = new ArrayList<>();
    ModelContext context = new ModelContext(targetResourceId, namespaceIri);
    if(buildingIri != null) {
      buildings.add(context.recursiveLoadPartial(Building.class, buildingIri,
          1, "thematicSurfaces", "objectClassId"));
    } else {
      buildings.addAll(context.recursiveLoadPartialWhere(Building.class, null,
          1, "thematicSurfaces", "objectClassId"));
    }
    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.PUT)) {
      try {
        Set<String> keys = requestParams.keySet();
        if (keys.contains(KEY_NAMESPACE)) {
          namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE)).toString();
          buildingIri = requestParams.has(KEY_BUILDING) ? new URI(requestParams.getString(KEY_BUILDING)).toString() : null;
          return true;
        }
      } catch (URISyntaxException | JSONException e) {
        throw new BadRequestException(e);
      }
    }
    throw new BadRequestException();
  }

}
