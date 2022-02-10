package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Executors;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;

import lombok.Getter;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.*;
import uk.ac.cam.cares.twa.cities.tasks.geo.ThematicSurfaceDiscoveryTask;


/**
 * A JPSAgent framework-based ThematicSurfaceDiscoveryAgent class used to discover thematic surfaces
 * of city objects in the Semantic 3D City Database {@link <a
 * href="https://www.cares.cam.ac.uk/research/cities/">}. The agent listens to GET requests containing either the IRI
 * of a building or the IRI of a namespace, and queries the building or buildings in the namespace to discover
 * lodXMultiSurface geometries, determine their themes from building geometry, and update the CKG database with a
 * revised hierarchy structured into thematic surfaces. This agent can only be used for buildings with no interior
 * geometry and strictly top-down topography.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@WebServlet(urlPatterns = {ThematicSurfaceDiscoveryAgent.URI_LISTEN})
public class ThematicSurfaceDiscoveryAgent extends JPSAgent {

  public enum Mode {
    RESTRUCTURE,
    COMMENT
  }

  public static class Params {
    public final String targetResourceId;
    public final String namespace;
    public final boolean[] lods;
    public final double threshold;
    public final Mode mode;
    public Params(String targetResourceId, String namespace, boolean[] lods, double threshold, Mode mode) {
      this.targetResourceId = targetResourceId;
      this.namespace = namespace;
      this.lods = lods;
      this.threshold = threshold;
      this.mode = mode;
    }
    public ModelContext makeContext() {
      return new ModelContext(targetResourceId, namespace);
    }
  }

  // Agent endpoint and parameter keys
  public static final String URI_LISTEN = "/discovery/thematicsurface";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_NAMESPACE = "namespace";
  public static final String KEY_COBI = "cityObjectIRI";
  public static final String KEY_LOD = "lod";
  public static final String KEY_THRESHOLD = "thresholdAngle";
  public static final String KEY_COMMENT = "comment";

  // Exception and error text
  private static final String NO_CRS_EXCEPTION_TEXT = "Namespace has no CRS specified.";
  private static final String MULTIPLE_CRS_EXCEPTION_TEXT = "Namespace has more than one CRS specified.";

  // Query labels
  private static final String QM = "?";
  private static final String SRS = "srs";
  private static final String BUILDING = "bldg";
  private static final String BUILDING_PARENT = "bldgParent";

  // Default task parameters
  private static final double DEFAULT_THRESHOLD = 15;

  private final String targetResourceId;
  @Getter private String buildingIri;
  @Getter private String namespaceIri;
  @Getter private Params taskParams;

  public ThematicSurfaceDiscoveryAgent() {
    super();
    targetResourceId = ResourceBundle.getBundle("config").getString("uri.route");
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    requestParams.put("acceptHeaders", "application/json");
    importSrs();
    // If building IRI specified, use that; else, fetch all buildings in namespace.
    List<String> buildingIris = new ArrayList<>();
    if (buildingIri != null) {
      buildingIris.add(buildingIri);
    } else {
      Node buildingObjectClass = NodeFactory.createLiteral(String.valueOf(26), XSDDatatype.XSDinteger);
      SelectBuilder buildingsQuery = new SelectBuilder();
      SPARQLUtils.addPrefix(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, buildingsQuery);
      buildingsQuery.addVar(QM + BUILDING)
          .addWhere(QM + BUILDING, SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, buildingObjectClass)
          .addWhere(QM + BUILDING, SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID, QM + BUILDING_PARENT);
      JSONArray buildingsResponse = taskParams.makeContext().query(buildingsQuery.buildString());
      for (int i = 0; i < buildingsResponse.length(); i++)
        buildingIris.add(buildingsResponse.getJSONObject(i).getString(BUILDING));
    }
    Executors.newSingleThreadExecutor().execute(new ThematicSurfaceDiscoveryTask(buildingIris, taskParams));
    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.PUT)) {
      try {
        Set<String> keys = requestParams.keySet();
        Mode mode = keys.contains(KEY_COMMENT) ? Mode.COMMENT : Mode.RESTRUCTURE;
        if (keys.contains(KEY_NAMESPACE)) {
          namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE)).toString();
          buildingIri = keys.contains(KEY_COBI) ? new URI(requestParams.getString(KEY_COBI)).toString() : null;
          double threshold = keys.contains(KEY_THRESHOLD) ? requestParams.getDouble(KEY_THRESHOLD) : DEFAULT_THRESHOLD;
          boolean[] lods = new boolean[4];
          for (int i = 0; i < 4; i++)
            lods[i] = keys.contains(KEY_LOD + (i + 1)) && requestParams.getBoolean(KEY_LOD + (i + 1));
          if (!(lods[0] || lods[1] || lods[2] || lods[3])) Arrays.fill(lods, true);
          taskParams = new Params(targetResourceId, namespaceIri, lods, threshold, mode);
          return true;
        }
      } catch (URISyntaxException | JSONException e) {
        throw new BadRequestException(e);
      }
    }
    throw new BadRequestException();
  }

  /**
   * Queries the database for the coordinate reference system to use and sets it as the {@link GeometryType} source crs.
   */
  private void importSrs() throws JPSRuntimeException {
    // TODO: convert ocgml:srsname to a SchemaManagerAdapter constant when it exists.
    SelectBuilder srsQuery = new SelectBuilder();
    SPARQLUtils.addPrefix("ocgml:srsname", srsQuery);
    srsQuery.addVar(QM + SRS).addWhere(NodeFactory.createURI(namespaceIri), "ocgml:srsname", QM + SRS);
    JSONArray srsResponse = taskParams.makeContext().query(srsQuery.buildString());
    if (srsResponse.length() == 0) {
      throw new JPSRuntimeException(NO_CRS_EXCEPTION_TEXT);
    } else if (srsResponse.length() > 1) {
      throw new JPSRuntimeException(MULTIPLE_CRS_EXCEPTION_TEXT);
    } else {
      GeometryType.setSourceCrsName(srsResponse.getJSONObject(0).getString(SRS));
    }
  }

}
