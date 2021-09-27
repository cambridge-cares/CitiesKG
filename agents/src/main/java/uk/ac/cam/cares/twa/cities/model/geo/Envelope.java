package uk.ac.cam.cares.twa.cities.model.geo;

import java.util.Arrays;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.geotools.geometry.jts.GeometryBuilder;
import org.json.JSONArray;
import org.locationtech.jts.geom.*;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import java.util.ResourceBundle;

/**
 *  The class retrieves envelope string of a specific URI from the KG.
 *  It further transforms envelope string into an envelope with a boundary(list of five points), centroid and crs attributes.
 */
public class Envelope {

    private Point centroid;
    private final GeometryBuilder factory = new GeometryBuilder();
    private final String crs;

    private static String route;
    private static String ontologyUri;
    private String cityobjectURI;
    private static final String ENVELOPE_OBJECT = "Envelope";
    private static final String OCGML_PREFIX = "ocgml";
    private static final String QST_MARK = "?";
    private static final String COLON = ":";

    private KnowledgeBaseClientInterface kgClient;

    public Envelope(String crs) {
        this.crs = crs;
        readConfig();
    }

   /**
   * reads variable values relevant for DistanceAgent class from config.properties file.
   */
    private void readConfig() {
        ResourceBundle config = ResourceBundle.getBundle("config");

        ontologyUri = config.getString("uri.ontology.ontocitygml");
        route = config.getString("uri.route");
        cityobjectURI = config.getString("uri.ckg.cityobject");
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve its envelope.
     * @param uriString object id
     * @return returns a query
     */
    private Query getEnvelopeQuery(String uriString) {

        String envelopeGraphUri = getEnvelopeGraphUri(uriString);

        SelectBuilder sb = new SelectBuilder()
                .addPrefix(OCGML_PREFIX, ontologyUri)
                .addVar( QST_MARK + ENVELOPE_OBJECT)
                .addGraph(NodeFactory.createURI(envelopeGraphUri), QST_MARK +"s", OCGML_PREFIX + COLON + "EnvelopeType", QST_MARK + ENVELOPE_OBJECT);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        return sb.build();
    }

    /**
     * sets KG Client for specific endpoint.
     */
    private void setKGClient(){

        this.kgClient = KGRouter.getKnowledgeBaseClient(Envelope.route,
                true,
                false);
    }

    /**
    * returns the graph Uri of the city object.
    * @param uriString city object id
    * @return graph uri of the city object.
    */
    private String getEnvelopeGraphUri(String uriString) {
      String[] splitUri = uriString.split("/");
      String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
      return namespace + cityobjectURI;
    }

    /**
     * executes query on SPARQL endpoint and retrieves envelope string for specific URI.
     * @param uriString city object id
     * @return envelope string
     */
    public String getEnvelopeString(String uriString) {

        setKGClient();

        String envelopeString;
        Query q = getEnvelopeQuery(uriString);

        String queryResultString = kgClient.execute(q.toString());

        JSONArray queryResult = new JSONArray(queryResultString);
        envelopeString = queryResult.getJSONObject(0).get(ENVELOPE_OBJECT).toString();

        return envelopeString;
    }

    /**
     * transforms envelopeString into 5 points representing envelope boundary, computes its centroid and sets envelope attributes.
     * @param envelopeString envelope string from KG.
     */
    public void extractEnvelopePoints(String envelopeString) {

        if (envelopeString.equals("")) {
           throw new IllegalArgumentException("empty String");
       }
       else if (!envelopeString.contains("#")){
           throw new IllegalArgumentException("Does not contain #");
       }

       String[] pointsAsString = (envelopeString.split("#"));

       int numberOfDimensions;
       if (pointsAsString.length % 3 == 0){
           numberOfDimensions = 3;
       }
       else if (pointsAsString.length % 2 == 0){
           numberOfDimensions = 2;
       }
       else {
          throw new IllegalArgumentException("Number of points is not divisible by 3 or 2");
       }

       int numberOfPoints = pointsAsString.length / numberOfDimensions;
       if (numberOfPoints < 4) {
          throw new IllegalArgumentException("Polygon has less than 4 points");
       }

      double[] points = new double[pointsAsString.length];
      for (int index = 0; index < pointsAsString.length; index++) {
          points[index]= Double.parseDouble(pointsAsString[index]);
      }
      double centroidZ = 0;
        Polygon boundary;
        if (numberOfDimensions == 3){
          boundary = factory.polygonZ(points);

          for (int z = 2; z < points.length-3; z +=3 ){
              centroidZ += points[z];
          }
          centroidZ = centroidZ/(numberOfPoints -1);
      }
      else {
          boundary = factory.polygon(points);
      }
      centroid = boundary.getCentroid();
      centroid.getCoordinateSequence().setOrdinate(0, 2, centroidZ);
      centroid.geometryChanged();
   }

    /**
     * gets centroid as Point.
     * @return centroid
     */
    public Point getCentroid() {
      return centroid;
  }

    /**
     * gets envelope CRS.
     * @return crs
     */
    public String getCRS(){
        return crs;
  }
}