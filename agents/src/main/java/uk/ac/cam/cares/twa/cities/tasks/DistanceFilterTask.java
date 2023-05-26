package uk.ac.cam.cares.twa.cities.tasks;


import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.vocabulary.VOID;
import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

import java.util.*;

public class DistanceFilterTask {

    private String cityObjectIri;
    private double searchDistance;
    private String sparqlEndpoint;
    public String customDataType = "<http://localhost/blazegraph/literals/POLYGON-3-15>";
    public String customField = "X0#Y0#Z0#X1#Y1#Z1#X2#Y2#Z2#X3#Y3#Z3#X4#Y4#Z4";
    public String ocgml = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
    public String geo = "http://www.bigdata.com/rdf/geospatial#";
    public String zo = "http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#";
    public String obs = "http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#";
    public String namespaceURl = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql";
    public String hint = "http://www.bigdata.com/queryHints#";


    public DistanceFilterTask(String cityObjectIri, double searchDistance, String route){
        this.cityObjectIri = cityObjectIri;
        this.searchDistance = searchDistance / 1000;  // km
        this.sparqlEndpoint = route;
    }

    public JSONArray queryAllowUseAndGFA(){
        String sparqlQuery =
                "PREFIX opr: <http://www.theworldavatar.com/ontology/ontoplanningreg/OntoPlanningReg.owl#>\n" +
                "PREFIX obs: <http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>\n" +
                "PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>\n" +
                "PREFIX oz:<http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>\n" +
                "\n" +
                "PREFIX selected_plot: <{CITYOBJECTIRI}>\n" +
                "\n" +
                "SELECT ?zone (ROUND(?gfa) AS ?gfa_metres) ?zoning_case\n" +
                "WHERE { \n" +
                "  GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/>\n" +
                "\t\t{ selected_plot: oz:hasZone ?zone_uri . \n" +
                "        BIND(REPLACE(STR(?zone_uri), \"http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#\", \"\") AS ?zone ) }\n" +
                "  GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace2/>\n" +
                " \t\t{selected_plot: obs:hasBuildableSpace ?buildable_space .\n" +
                "         ?buildable_space obs:hasAllowedGFA/om:hasValue/om:hasNumericValue ?gfa  .\n" +
                "        OPTIONAL { ?buildable_space obs:forZoningCase ?exception  . } \n" +
                "        BIND(REPLACE(STR(?exception), \"http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#\", \"\") AS ?zoning_case) }\n" +
                "        FILTER(?gfa > 0)  }\n";

        //JSONArray returnedCityObjects =  new JSONArray();
        sparqlQuery = sparqlQuery.replace("{CITYOBJECTIRI}", this.cityObjectIri);
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, sparqlQuery); // string
        return queryResult;
    }

    public double[] calcEnvelopCentroid (String cityObjectIri){
        String sparqlQuery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>\n" +
                "\n" +
                "PREFIX selected_plot: <{CITYOBJECTIRI}>\n" +
                "\n" +
                "SELECT *\n" +
                "WHERE {  \n" +
                "GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/> { \n" +
                "selected_plot: ocgml:id ?Id ;\n" +
                "  \t\t       ocgml:EnvelopeType ?Envelope . \n" +
                "}}";

        sparqlQuery = sparqlQuery.replace("{CITYOBJECTIRI}", this.cityObjectIri);
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, sparqlQuery);
        String envelopStr = queryResult.getJSONObject(0).getString("Envelope");
        // envelope = [Xmin, Xmax, Ymin, Ymax]
        double[] envelop = getEnvelopFromString(envelopStr);
        double[] centroid = EnvelopeCentroid.calcCentroid(envelop); //long, long, lat, lat
        //System.out.print(centroid);
        return centroid;  // long, lat
    }


    private double[] createBboxCorner(double longitude, double latitude, double dx, double dy){
        double radius_earth = 6378f; // km

        double newLatitude = latitude + (dy / radius_earth) * (180 / Math.PI);
        double newLongitude = longitude + (dx / radius_earth) * (180 / Math.PI) / Math.cos(latitude * Math.PI / 180);

        return new double[]{newLongitude, newLatitude};
    }
    // 5points
    private String createCornerString(double longitude, double latitude, double height){
        String cornerStr = "";
        for (int i = 0; i < 4; ++i) {
            cornerStr += Double.toString(latitude) + "#" + Double.toString(longitude) + "#" + Double.toString(height) + "#";
        }
        cornerStr += Double.toString(latitude) + "#" + Double.toString(longitude) + "#" + Double.toString(height);
        return cornerStr;
    }


    public static double[] getEnvelopFromString(String envelopStr){
        String[] pointXYZList = envelopStr.split("#");
        List<Coordinate> points = new LinkedList<>();

        if (pointXYZList.length % 3 == 0) {
            // 3d coordinates
            for (int i = 0; i < pointXYZList.length; i = i + 3) {
                points.add(new Coordinate(Double.parseDouble(pointXYZList[i]), Double.parseDouble(pointXYZList[i + 1]), Double.parseDouble(pointXYZList[i + 2])));
            }
        }else {
            System.out.println("InputString has no valid format");
            return null;
        }

        Coordinate[] coordinates = points.toArray(new Coordinate[0]);

        // [Xmin, Xmax, Ymin, Ymax]
        List<Double> xCoords = new ArrayList<>();
        List<Double> yCoords = new ArrayList<>();
        List<Double> zCoords = new ArrayList<>();

        for (Coordinate coords : points){
            xCoords.add(coords.getX());
            yCoords.add(coords.getY());
            zCoords.add(coords.getY());
        }

        double new_xmin = Collections.min(xCoords);
        double new_xmax = Collections.max(xCoords);
        double new_ymin = Collections.min(yCoords);
        double new_ymax = Collections.max(yCoords);

        double[] envelop = new double[]{new_ymin, new_ymax, new_xmin, new_xmax};

        return envelop;
    }

    public double calcGeomtryArea(String geomStr){
        List<Coordinate> coords = GeoSpatialProcessor.str2coords(geomStr);
        GeometryFactory factory = new GeometryFactory();



        return 0;

    }

    // Process queryDistanceFilter Result
    public JSONArray processGeoSearchResult(JSONArray results){

        JSONObject geosearchResults = new JSONObject();
        geosearchResults.put("numOfPlots", results.length());

        for (int i = 0; i < results.length(); ++i){
            JSONObject obj = results.getJSONObject(i);
            String geomStr = obj.getString("geoms");
            double geomArea = calcGeomtryArea(geomStr);

        }








        return null;
    }

    public JSONArray queryDistanceFilter(){
        // Get Envelop Center
        double[] envelopCentroid = calcEnvelopCentroid(this.cityObjectIri);
        // Get LowerBounds String
        double[] lowerCorner = createBboxCorner(envelopCentroid[0], envelopCentroid[1], -this.searchDistance, -this.searchDistance);
        String lowerBounds = createCornerString(lowerCorner[0], lowerCorner[1], 0.0);
        // Get UpperBounds String
        double[] upperCorner = createBboxCorner(envelopCentroid[0], envelopCentroid[1], this.searchDistance, this.searchDistance);
        String upperBounds = createCornerString(upperCorner[0], upperCorner[1], 1000);

        // LowerBounds: 1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0
        // UpperBounds: 1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000

        try {
            Query actualquery= getInfoWithinBoundsQuery(cityObjectIri, lowerBounds, upperBounds);
            String queryString = actualquery.toString().replace("PLACEHOLDER", "");
            JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, queryString);
            return queryResult;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getGraph(String uriString, String graph) {
        String namespace = getNamespace(uriString);
        return namespace + graph + "/";
    }

    private String getNamespace(String uriString) {
        String[] splitUri = uriString.split("/");
        return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2))+"/";
    }


    // Query for lower part
    private Query getInfoWithinBoundsQuery(String uriString, String lowerBounds, String upperBounds) throws ParseException {
        SelectBuilder sb = new SelectBuilder().addVar("?cityObject");

        // Part 1: cityobject
        WhereBuilder wb1 = new WhereBuilder()
                .addPrefix("ocgml", ocgml)
                .addWhere("?cityObject", "ocgml:EnvelopeType", "?envelope")
                .addBind("IRI(REPLACE(STR(?cityObject), \"cityobject\", \"genericcityobject\"))", "?gen_obj");  // to check

        String cityObjectGraph = namespaceURl + "/cityobject/";
        sb.addGraph(NodeFactory.createURI(cityObjectGraph), wb1); //it won't work only addElement

        // Part 2: surfacegeometry
        WhereBuilder wb2 = new WhereBuilder()
                .addPrefix("ocgml", ocgml)
                .addWhere("?surface", "ocgml:cityObjectId", "?gen_obj")
                .addWhere("?surface", "ocgml:GeometryType", "?geoms");

        String surfaceGeometryGraph = namespaceURl + "/surfacegeometry/";
        sb.addVar("?geoms").addGraph(NodeFactory.createURI(surfaceGeometryGraph), wb2);

        // Part 3: ontozone
        WhereBuilder wb3 = new WhereBuilder()
                .addPrefix("zo", zo)
                .addWhere("?cityObject", "zo:hasZone", "?zone_uri")
                .addBind("REPLACE(STR(?zone_uri), \"http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#\", \"\")","?zone");

        String ontozoneGraph = namespaceURl + "/ontozone/";
        sb.addVar("?zone").addGraph(NodeFactory.createURI(ontozoneGraph), wb3);

        // Checksum: 113672 before geosearch
        // SERVICE geo:search
        // where clause for geospatial search
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ocgml)
                .addPrefix("geo", geo)
                .addPrefix("hint", hint)
                .addWhere("?cityObject", "geo:predicate", "ocgml:EnvelopeType")
                .addWhere("?cityObject", "geo:searchDatatype", customDataType)
                .addWhere("?cityObject", "geo:customFields", customField)
                // PLACEHOLDER because lowerBounds and upperBounds would be otherwise added as doubles, not strings
                .addWhere("?cityObject", "geo:customFieldsLowerBounds", "PLACEHOLDER"+lowerBounds)
                .addWhere("?cityObject", "geo:customFieldsUpperBounds", "PLACEHOLDER"+upperBounds)
                .addWhere("hint:Prior", "hint:runFirst", "true");

        Query query = sb.build();
        // add geospatial service
        ElementGroup body = new ElementGroup();
        body.addElement(new ElementService(geo + "search", wb.build().getQueryPattern()));
        body.addElement(sb.build().getQueryPattern());  // addElement with sb can include the graph. can avoid using WhereHandler to extend the query / add the graph afterwards (see testing purpose example)
        query.setQueryPattern(body);

        return query;
    }

    // Just testing purpose for the development
    private Query getEnvelopsWithinBoundsQuery(String uriString, String lowerBounds, String upperBounds) throws ParseException {
        String ocgmlUri = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
        String geoUri = "http://www.bigdata.com/rdf/geospatial#";

        // where clause for geospatial search
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addPrefix("geo", geoUri)
                .addWhere("?cityObject", "geo:predicate", "ocgml:EnvelopeType")
                .addWhere("?cityObject", "geo:searchDatatype", customDataType)
                .addWhere("?cityObject", "geo:customFields", customField)
                // PLACEHOLDER because lowerBounds and upperBounds would be otherwise added as doubles, not strings
                .addWhere("?cityObject", "geo:customFieldsLowerBounds", "PLACEHOLDER"+lowerBounds)
                .addWhere("?cityObject", "geo:customFieldsUpperBounds", "PLACEHOLDER"+upperBounds);

        SelectBuilder sb = new SelectBuilder()
                .addVar("?cityObject");

        WhereBuilder wb2 = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addWhere("?cityObject", "ocgml:EnvelopeType", "?envelop");

        Query query = sb.build();
        // add geospatial service
        ElementGroup body = new ElementGroup();
        body.addElement(new ElementService(geoUri + "search", wb.build().getQueryPattern()));
        body.addElement(wb2.build().getQueryPattern());
        query.setQueryPattern(body);

        WhereHandler wh = new WhereHandler(query.cloneQuery());

        // add city object graph
        WhereHandler wh2 = new WhereHandler(sb.build());
        wh2.addGraph(NodeFactory.createURI(getGraph(uriString,"cityobject")), wh);
        return wh2.getQuery();
    }


}
