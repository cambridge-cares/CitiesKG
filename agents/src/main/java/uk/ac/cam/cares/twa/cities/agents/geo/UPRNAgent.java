package uk.ac.cam.cares.twa.cities.agents.geo;

import lombok.Getter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.ogm.models.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.model.geo.*;
import uk.ac.cam.cares.twa.cities.models.osid.UPRN;
import uk.ac.cam.cares.twa.cities.tasks.geo.UPRNTask;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;

@WebServlet(urlPatterns = {UPRNAgent.URI_LISTEN})
public class UPRNAgent extends JPSAgent {

  public static final String URI_LISTEN = "/uprn";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_NAMESPACE = "namespace";
  public static final String KEY_BUILDING = "cityObjectIRI";

  // for SRS query
  private static final String NO_CRS_EXCEPTION_TEXT = "Namespace has no CRS specified.";
  private static final String MULTIPLE_CRS_EXCEPTION_TEXT = "Namespace has more than one CRS specified.";
  private static final String QM = "?";
  private static final String SRS = "srs";

  private final String targetResourceId = ResourceBundle.getBundle("config").getString("uri.route");

  @Getter private String buildingIri;
  @Getter private String namespaceIri;
  private ModelContext context;

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    importSrs(context);
    Executors.newSingleThreadExecutor().execute(new UPRNTask(context, buildingIri, namespaceIri));
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
          buildingIri = keys.contains(KEY_BUILDING) ? new URI(requestParams.getString(KEY_BUILDING)).toString() : null;
          context = new ModelContext(targetResourceId, namespaceIri);
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
  private void importSrs(ModelContext context) throws JPSRuntimeException {
    // TODO: convert ocgml:srsname to a SchemaManagerAdapter constant when it exists.
    SelectBuilder srsQuery = new SelectBuilder();
    SPARQLUtils.addPrefix("ocgml:srsname", srsQuery);
    srsQuery.addVar(QM + SRS).addWhere(NodeFactory.createURI(namespaceIri), "ocgml:srsname", QM + SRS);
    JSONArray srsResponse = context.query(srsQuery.buildString());
    if (srsResponse.length() == 0) {
      throw new JPSRuntimeException(NO_CRS_EXCEPTION_TEXT);
    } else if (srsResponse.length() > 1) {
      throw new JPSRuntimeException(MULTIPLE_CRS_EXCEPTION_TEXT);
    } else {
      GeometryType.setSourceCrsName(srsResponse.getJSONObject(0).getString(SRS));
    }
  }

}
