package uk.ac.cam.cares.twa.cities.tasks;


import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementService;
import org.citydb.config.internal.Internal;
import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.apache.jena.sparql.core.Var;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import uk.ac.cam.cares.twa.cities.model.geo.Transform;

import java.util.*;

/**
 * A task that executes the queries for suitable site selector
 *
 * @author <a href="mailto:shiying.li@sec.ethz.ch">Shiying Li</a>
 */
public class DistanceFilterTask {

    private final String cityObjectIri;
    private final double distanceInKM;
    private final String sparqlEndpoint;
    public String customDataType = "<http://localhost/blazegraph/literals/POLYGON-3-15>";
    public String customField = "X0#Y0#Z0#X1#Y1#Z1#X2#Y2#Z2#X3#Y3#Z3#X4#Y4#Z4";
    public String ocgml = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
    public String geo = "http://www.bigdata.com/rdf/geospatial#";
    public String zo = "http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#";
    public String namespaceURl = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql";
    public String hint = "http://www.bigdata.com/queryHints#";
    private Integer WGS84InDeg = 4326;
    private Integer SGInMeter = 3414;

    private String lowerBounds;
    private String upperBounds;

    public DistanceFilterTask(String cityObjectIri, double searchDistance, String route){
        this.cityObjectIri = cityObjectIri;
        this.distanceInKM = searchDistance / 1000;  // km
        this.sparqlEndpoint = route;
        prepareBounds();
    }

    /**
     * Query for allowable Landuse and its GFA of a plot (future)
     *
     * @return JSONArray that contains zone, gfa, zoning case
     */
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
        return AccessAgentCaller.queryStore(sparqlEndpoint, sparqlQuery);
    }

    /**
     * Query for existing Landuse and its GFA of a plot (present)
     *
     * @return JSONArray that contains landuseType, gfaValue
     */
    public JSONArray queryPresentLandUseGFA(){
        String sparqlQuery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>\n" +
                "PREFIX selected_plot: <{CITYOBJECTIRI}>\n" +
                "SELECT ?landuseType ?gfaValue \n" +
                "WHERE {\n" +
                "  GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib_andrea_v2> {\n" +
                "    ?subject ocgml:cityObjectId selected_plot: . \n" +
                "    ?subject ocgml:attrName ?landuseType.\n" +
                "    ?subject ocgml:intVal ?gfaValue.\n" +
                "  }\n" +
                "}\n";

        sparqlQuery = sparqlQuery.replace("{CITYOBJECTIRI}", this.cityObjectIri);
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, sparqlQuery);

        // Process the resultData, don't forward non-zero value
        JSONArray landuseGFA = new JSONArray();

        for (int i = 0; i < queryResult.length(); ++i){
            JSONObject obj = queryResult.getJSONObject(i);
            String gfaType = obj.getString("landuseType");
            String gfaValue = obj.getString("gfaValue");
            if (gfaType.contains("GFA") && Double.parseDouble(gfaValue) > 0 ) {
                String[] landuseName = gfaType.split("_");
                JSONObject row = new JSONObject();
                row.put("landuseType", landuseName[1]);
                row.put("gfaValue", gfaValue);
                landuseGFA.put(row);
            }
        }

        return landuseGFA;

    }

    /**
     * Get the envelop of a cityobject and calculate its centroid
     *
     * @return double[] that contains long and lat
     */
    public double[] calcEnvelopCentroid (){
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
        return EnvelopeCentroid.calcCentroid(Objects.requireNonNull(envelop)); //[long, long, lat, lat] --> [long, lat]
    }

    /**
     * Get the corner point of a bounding box based on a given centroid and the displacement in x-axis and y-axis
     *
     * @param longitude - longitude of the given point
     * @param latitude - latitude of the given point
     * @param dx - displacement along longitude with unit "km"
     * @param dy - displacement along latitude with unit "km"
     * @return double[] that contains the corner points of a bounding box in long, lat
     */
    private double[] createBboxCorner(double longitude, double latitude, double dx, double dy){
        double radius_earth = 6378f; // km

        double newLatitude = latitude + (dy / radius_earth) * (180 / Math.PI);
        double newLongitude = longitude + (dx / radius_earth) * (180 / Math.PI) / Math.cos(latitude * Math.PI / 180);

        return new double[]{newLongitude, newLatitude};
    }


    /**
     * Create a string to describe a point in form of envelop by repeating the same xyz with 5 times
     *
     * @param longitude - longitude of the given point
     * @param latitude - latitude of the given point
     * @param height - height of the given point
     * @return String - A string form x0#y0#z0#x0#y0#z0#x0#y0#z0#x0#y0#z0#x0#y0#z0
     */
    private String createCornerString(double longitude, double latitude, double height){
        StringBuilder cornerStr = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            cornerStr.append(latitude).append("#").append(longitude).append("#").append(height).append("#");
        }
        cornerStr.append(latitude).append("#").append(longitude).append("#").append(height);
        return cornerStr.toString();
    }

    /**
     * Get envelop corner from envelop String of blazegraph
     *
     * @param envelopStr - String that describes envelope in following form: x0#y0#z0#x1#y1#z1#x2#y2#z3#x4#y4#z4#x0#y0#z0
     * @return double[] - An array of corner point coordinates [new_ymin, new_ymax, new_xmin, new_xmax]
     */
    public static double[] getEnvelopFromString(String envelopStr){
        String[] pointXYZList = envelopStr.split("#");
        double[][] double3DArray;

        if (pointXYZList.length % 3 == 0) {
            int numOfPoints = pointXYZList.length / 3;
            double3DArray = new double[numOfPoints][3];
            int indexNum = 0;
            // 3d coordinates
            for (int i = 0; i < pointXYZList.length; i = i + 3) {
                double3DArray[indexNum][0] = Double.parseDouble(pointXYZList[i]);
                double3DArray[indexNum][1] = Double.parseDouble(pointXYZList[i+1]);
                double3DArray[indexNum][2] = Double.parseDouble(pointXYZList[i+2]);
                indexNum++;
            }
            return Transform.getEnvelopeFromPoints(double3DArray);
        }else {
            System.out.println("InputString has no valid format");
            return null;
        }
    }

    /**
     * Calculate the area from a geometry string with its geometrytype in blazegraph
     *
     * @param geomStr - geometry string retrieved from blazegraph
     * @param geomType - geometry type that defines how to understand geometry string.
     * @return double - Area coverage from a given geometry retrieved from blazegraph
     */
    public double calcGeometryArea(String geomStr, String geomType, Integer EPSGInMeter){
        Geometry polyon = GeoSpatialProcessor.createGeometry(geomStr, geomType);

        GeoSpatialProcessor geop = new GeoSpatialProcessor();
        Geometry transformed = geop.Transform(polyon, WGS84InDeg, EPSGInMeter);
        return transformed.getArea();
    }


    public JSONObject processGeoSearchResult(JSONArray results){

        JSONObject geosearch = new JSONObject();
        geosearch.put("numOfPlots", results.length());
        JSONArray processedResults = new JSONArray();
        double totalArea = 0;
        String previousZone = results.getJSONObject(0).getString("zone");
        double zoneTotalArea = 0;

        JSONArray zoneAreaArray = new JSONArray();

        ArrayList<String> zoneNameList = new ArrayList<>();

        for (int i = 0; i < results.length(); ++i){
            JSONObject obj = results.getJSONObject(i);
            String geomStr = obj.getString("geoms");
            String geomType = obj.getString("geomType");
            double geomArea = calcGeometryArea(geomStr, geomType, SGInMeter);
            obj.put("area", geomArea);
            totalArea += geomArea;
            zoneNameList.add(obj.getString("zone"));
            processedResults.put(obj);

            if (!previousZone.equals(obj.getString("zone"))){
                JSONObject zoneNameArea = new JSONObject();
                zoneNameArea.put("zoneName", obj.getString("zone"));
                zoneNameArea.put("zoneArea", zoneTotalArea);
                zoneAreaArray.put(zoneNameArea);
                // reset
                previousZone = obj.getString("zone");
                zoneTotalArea = geomArea;
            }else{
                zoneTotalArea += geomArea;
            }
        }

        // Checking
        if (zoneNameList.size() == results.length()){
            Set<String> zoneNameSet = new HashSet<>(zoneNameList);
            geosearch.put("availableZone", zoneNameSet.toArray());
        }else{
            throw new RuntimeException("zoneNameList doesn't match with result size");
        }
        geosearch.put("areaPerZone",zoneAreaArray);
        geosearch.put("totalArea", totalArea);

        return geosearch;
    }

    /**
     * Create a query to retrieve information used for area calculation
     *
     * @return double - the area of a plot
     */
    public double getPlotArea(){

        String sparqlQuery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>\n" +
                "PREFIX selected_plot: <{CITYOBJECTIRI}>\n" +
                "\n" +
                "SELECT ?geoms  (DATATYPE(?geoms) AS ?geomType)\n" +
                "WHERE { \n" +
                "  GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>\n" +
                "        { ?cityObject  ocgml:id  selected_plot: . \n" +
                "          BIND(iri(replace(str(?cityObject), \"cityobject\", \"genericcityobject\")) AS ?gen_obj)\n" +
                "        }\n" +
                "\tGRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/>\n" +
                "        { ?surface  ocgml:cityObjectId  ?gen_obj ;\n" +
                "                    ocgml:GeometryType  ?geoms .} \n" +
                "}\n";

        sparqlQuery = sparqlQuery.replace("{CITYOBJECTIRI}", this.cityObjectIri);
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, sparqlQuery);

        JSONObject obj = queryResult.getJSONObject(0);

        String geomStr = obj.getString("geoms");
        String geomType = obj.getString("geomType");
        return calcGeometryArea(geomStr, geomType, SGInMeter);
    }

    /**
     * Prepare the boundary string for the geospatial search
     * exmaple LowerBounds: 1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0
     * example UpperBounds: 1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000
     */
    private void prepareBounds(){
        // Get Envelop Center
        double[] envelopCentroid = calcEnvelopCentroid();
        // Get LowerBounds String
        double[] lowerCorner = createBboxCorner(envelopCentroid[0], envelopCentroid[1], -this.distanceInKM, -this.distanceInKM);
        lowerBounds = createCornerString(lowerCorner[0], lowerCorner[1], 0.0);
        // Get UpperBounds String
        double[] upperCorner = createBboxCorner(envelopCentroid[0], envelopCentroid[1], this.distanceInKM, this.distanceInKM);
        upperBounds = createCornerString(upperCorner[0], upperCorner[1], 1000);
    }

    /**
     * Query different cityobjects within a distance range (e.g., 500m)
     *
     * @return JSONObject - key/value for different cityobjects: total_NPark, total_CarPark, MRT, BUS_STOP
     */
    public JSONObject queryDistanceFilter(){

        try {
            Query actualquery= getInfoWithinBoundsQuery(cityObjectIri, lowerBounds, upperBounds);
            //Query actualquery = getAllowParksWithinBounds(cityObjectIri, lowerBounds, upperBounds);
            //Query actualquery = getTransportWithinBounds(lowerBounds, upperBounds, "MRT"); // MRT or BUS_STOP
            String queryString = actualquery.toString().replace("PLACEHOLDER", "");
            JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, queryString);

            JSONObject distanceFilterResults = processGeoSearchResult(queryResult);

            Integer allowParks = getAllowParksWithinBounds();
            distanceFilterResults.append("allowParks", allowParks);

            Integer nPark = getAttributesWithinBounds("cityobjectgenericattrib_andrea_v2", "total_NPark");
            distanceFilterResults.append("numOfPresentParks", nPark);

            Integer carPark = getAttributesWithinBounds("cityobjectgenericattrib_andrea_v2", "total_CarPark");
            distanceFilterResults.append("numOfCarPark", carPark);

            Integer mrt = getTransportWithinBounds("MRT");
            distanceFilterResults.append("numOfMrt", mrt);

            Integer busstops = getTransportWithinBounds("BUS_STOP");
            distanceFilterResults.append("numOfBusstop", busstops);

            return distanceFilterResults;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get GraphIRI from cityobjectIRI and graph name
     *
     * @param uriString - cityobjectIRI
     * @param graph - graphName
     * @return JSONObject - key/value for different cityobjects: total_NPark, total_CarPark, MRT, BUS_STOP
     */
    private String getGraph(String uriString, String graph) {
        String namespace = getNamespace(uriString);
        return namespace + graph + "/";
    }

    /**
     * Get the namespace from cityobjectIRI
     *
     * @param uriString - cityobjectIRI
     * @return String - Namespace string in form of /xx/
     */
    private String getNamespace(String uriString) {
        String[] splitUri = uriString.split("/");
        return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2))+"/";
    }

    /**
     * Prepare a geospatial query with the given bounderies without specifying the select variables
     *
     * @param lowerBounds - coordinates string for lowerBounds
     * @param upperBounds - coordinates string for upperBounds
     * @return Query - prepared SPARQL query to retrieve all plots lying within the lowerBounds and upperBounds
     */
    private Query buildQueryWithinBounds(String lowerBounds, String upperBounds) throws ParseException {

        SelectBuilder sb = new SelectBuilder();

        // Part 1: cityobject
        WhereBuilder wb1 = new WhereBuilder()
                .addPrefix("ocgml", ocgml)
                .addWhere("?cityObject", "ocgml:EnvelopeType", "?envelope");

        String cityObjectGraph = namespaceURl + "/cityobject/";
        sb.addGraph(NodeFactory.createURI(cityObjectGraph), wb1);

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

    // graphName: "/cityobjectgenericattrib_andrea_v2/"

    /**
     * Retrieve the count for a given attrName using preparedQuery for geospatial search
     *
     * @param graphName - graphName string
     * @param attrName - interested attrName string
     * @return Integer - result number for the given attribute
     */
    public Integer getAttributesWithinBounds(String graphName, String attrName){
        SelectBuilder sb = new SelectBuilder();
        ElementGroup elementGroup = new ElementGroup();
        try {
            //sb.addVar("COUNT(?cityObject)", "?numOfObjs");
            sb.addVar("?cityObject").addVar("?attrValue");
            // cityobjectgenericattrib
            WhereBuilder wb1 = new WhereBuilder()
                .addPrefix("ocgml", ocgml)
                .addWhere("?attr", "ocgml:cityObjectId", "?cityObject")
                .addWhere("?attr", "ocgml:attrName", attrName)
                .addWhere("?attr", "ocgml:intVal", "?attrValue");

            String cityObjectGraph = namespaceURl + "/" + graphName;    // andrea data with "/"
            sb.addGraph(NodeFactory.createURI(cityObjectGraph), wb1);

            Query preparedQuery = buildQueryWithinBounds(lowerBounds, upperBounds);
            elementGroup = (ElementGroup) preparedQuery.getQueryPattern();
            //elementGroup.addElement();
            elementGroup.getElements().add(sb.build().getQueryPattern());

        } catch (ParseException e) {
            e.printStackTrace();
        }

        Query query = sb.build();
        query.setQueryPattern(elementGroup);
        String queryString = query.toString().replace("PLACEHOLDER", "");
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, queryString);

        int totalAttrValue = 0;

        for (int i = 0; i < queryResult.length(); ++i) {
            JSONObject obj = queryResult.getJSONObject(i);
            String cityObject = obj.getString("cityObject");
            String attrValue = obj.getString("attrValue");
            totalAttrValue += Double.parseDouble(attrValue);
        }
        return totalAttrValue;
    }

    /**
     * Retrieve the count of allowable parks within bounds using preparedQuery for geospatial search
     *
     * @return Integer - the count for allowable parks
     */
    public Integer getAllowParksWithinBounds(){

        SelectBuilder sb = new SelectBuilder();
        ElementGroup elementGroup = new ElementGroup();
        try {
            sb.addVar("COUNT(?cityObject)", "?numOfObjs");
            // ontozone
            WhereBuilder wb1 = new WhereBuilder()
                    .addPrefix("zo", zo)
                    .addWhere("?cityObject", "zo:hasZone", "zo:Park");

            String cityObjectGraph = namespaceURl + "/ontozone/";
            sb.addGraph(NodeFactory.createURI(cityObjectGraph), wb1);

            Query preparedQuery = buildQueryWithinBounds(lowerBounds, upperBounds);
            elementGroup = (ElementGroup) preparedQuery.getQueryPattern();
            //elementGroup.addElement();
            elementGroup.getElements().add(sb.build().getQueryPattern());

        } catch (ParseException e) {
            e.printStackTrace();
        }

        Query query = sb.build();
        query.setQueryPattern(elementGroup);
        String queryString = query.toString().replace("PLACEHOLDER", "");
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, queryString);

        JSONObject obj = queryResult.getJSONObject(0);
        return obj.getInt("numOfObjs");

    }

    /**
     * Retrieve the count of either MRT or BUS_STOP within bounds using preparedQuery for geospatial search
     *
     * @param MRTorBusstop - string either MRT or BUS_STOP
     * @return Integer - the count for existing public transport, either MRT or BUS_STOP
     */
    public Integer getTransportWithinBounds(String MRTorBusstop){

        SelectBuilder sb = new SelectBuilder();
        ElementGroup elementGroup = new ElementGroup();
        try {
            sb.addVar("COUNT(?cityObject)", "?numOfObjs");
            // cityobjectgenericattrib
            WhereBuilder wb1 = new WhereBuilder()
                    .addPrefix("ocgml", ocgml)
                    .addWhere("?attr", "ocgml:cityObjectId", "?cityObject")
                    .addWhere("?attr", "ocgml:attrName", "TYPE")
                    .addWhere("?attr", "ocgml:strVal", MRTorBusstop);

            String cityObjectGraph = namespaceURl + "/cityobjectgenericattrib/";
            sb.addGraph(NodeFactory.createURI(cityObjectGraph), wb1);

            Query preparedQuery = buildQueryWithinBounds(lowerBounds, upperBounds);
            elementGroup = (ElementGroup) preparedQuery.getQueryPattern();
            //elementGroup.addElement();
            elementGroup.getElements().add(sb.build().getQueryPattern());


        } catch (ParseException e) {
            e.printStackTrace();
        }

        Query query = sb.build();
        query.setQueryPattern(elementGroup);
        String queryString = query.toString().replace("PLACEHOLDER", "");
        JSONArray queryResult = AccessAgentCaller.queryStore(sparqlEndpoint, queryString);

        JSONObject obj = queryResult.getJSONObject(0);
        return obj.getInt("numOfObjs");
    }

    /**
     * Build the query to retrieve information within given bounds
     *
     * @param uriString - string either MRT or BUS_STOP
     * @param lowerBounds - lowerBounds string
     * @param upperBounds - upperBounds string
     * @return Query -
     */
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
                .addWhere("?surface", "ocgml:GeometryType", "?geoms")
                .addBind("datatype(?geoms)", "?geomType");

        String surfaceGeometryGraph = namespaceURl + "/surfacegeometry/";
        sb.addVar("?geoms").addVar("?geomType").addGraph(NodeFactory.createURI(surfaceGeometryGraph), wb2);

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
        query.addOrderBy(new SortCondition(Var.alloc("zone"), 1));
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
