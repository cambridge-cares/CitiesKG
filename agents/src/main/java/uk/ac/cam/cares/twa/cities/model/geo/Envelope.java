package uk.ac.cam.cares.twa.cities.model.geo;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.geotools.geometry.jts.GeometryBuilder;
import org.json.JSONArray;
import org.locationtech.jts.geom.*;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;


/**
 * The class transforms Envelope string into a list of points that defines envelope boundary and from it computes envelope centroid.
 */
public class Envelope {

    private int numberOfPoints = 5;
    private int numberOfDimensions = 3;
    private Polygon boundary;
    private Point centroid;
    private GeometryBuilder factory = new GeometryBuilder();
    private String crs;

    //private static final String ONTOLOGY_URI = "http://theworldavatar.com/ontology/ontocitygml/citieskg/";
    //private static final String ENVELOPE_GRAPH_URI = "http://www.theworldavatar.com/citieskg/singapore/cityobject/";
    //private static final String ROUTE = "http://kb/citieskg-singapore";
    private static final String ONTOLOGY_URI = "http://locahost/ontocitygml/";
    private static final String ENVELOPE_GRAPH_URI = "http://localhost/berlin/cityobject/";
    private static final String ROUTE = "http://kb/singapore-local";

    private KnowledgeBaseClientInterface kgClient;

    public Envelope(String crs) {
        this.crs = crs;
    }


   /** builds a SPARQL query for a specific URI to find envelope.
    */
    private Query getEnvelopeQuery(String uriString) {

        SelectBuilder sb = new SelectBuilder()
                .addPrefix( "ocgml",  ONTOLOGY_URI )
                .addVar( "?Envelope" )
                .addGraph(NodeFactory.createURI(ENVELOPE_GRAPH_URI), "?s", "ocgml:EnvelopeType", "?Envelope");
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));
        Query q = sb.build();

        return q;
    }


    /** get KGClient via KGrouter and execute query to get envelope string.
     */
    public String getEnvelopeString(String uriString) {
        setKGClient(ROUTE);

        String envelopeString = new String();
        Query q = getEnvelopeQuery(uriString);

        String queryResultString = kgClient.execute(q.toString());

        JSONArray queryResult = new JSONArray(queryResultString);
        envelopeString = queryResult.getJSONObject(0).get("Envelope").toString();

        return envelopeString;
    }


    /** sets KG Client for created envelope query.
     */
    private void setKGClient(String route){
        this.kgClient = KGRouter.getKnowledgeBaseClient(route,
                true,
                false);
    }


    /** Transforms envelopeString into 5 points representing envelope boundary.
     */
   public void extractEnvelopePoints(String envelopeString) {
       if (envelopeString.equals("")) {
           throw new IllegalArgumentException("empty String"); }
       else if (!envelopeString.contains("#")){
           throw new IllegalArgumentException("Does not contain #"); }

      String[] pointsAsString = (envelopeString.split("#"));

      if (pointsAsString.length % 3 == 0){
           numberOfDimensions = 3; }
      else if (pointsAsString.length % 2 == 0){
           numberOfDimensions = 2; }
      else {
          throw new IllegalArgumentException("Number of points is not divisible by 3 or 2"); }

      numberOfPoints = pointsAsString.length/numberOfDimensions;
       if (numberOfPoints < 4) {
          throw new IllegalArgumentException("Polygon has less than 4 points"); }
      double[] points = new double[pointsAsString.length];
      for (int index = 0; index < pointsAsString.length; index++){
          points[index]= Double.parseDouble(pointsAsString[index]);
      }

      double centroidZ = 0;

      if (numberOfDimensions == 3){
          boundary = factory.polygonZ(points);
          for (int z = 2; z < points.length-3; z +=3 ){
              centroidZ += points[z]; }
          centroidZ = centroidZ/(numberOfPoints-1); }
      else {
          boundary = factory.polygon(points); }

      centroid = boundary.getCentroid();
      centroid.getCoordinateSequence().setOrdinate(0, 2, centroidZ);
      centroid.geometryChanged();
   }


    /** Method gets centroid as Point.
     */
  public Point getCentroid() {
      return centroid;
  }


    /** Method gets envelope CRS.
     */
  public String getCRS(){
      return crs;
  }
}