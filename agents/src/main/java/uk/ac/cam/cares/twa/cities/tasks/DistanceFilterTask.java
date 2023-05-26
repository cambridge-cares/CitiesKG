package uk.ac.cam.cares.twa.cities.tasks;


import org.apache.jena.vocabulary.VOID;
import org.json.JSONArray;
import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DistanceFilterTask {

    private String cityObjectIri;
    private Integer searchDistance;
    private String sparqlEndpoint;


    public DistanceFilterTask(String cityObjectIri, Integer searchDistance, String route){
        this.cityObjectIri = cityObjectIri;
        this.searchDistance = searchDistance;
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

    public void calcEnvelopCentroid (String cityObjectIri){
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
        double[] centroid = EnvelopeCentroid.calcCentroid(envelop);

        System.out.print(centroid);
    }

    private double[] createBboxCorner(double longitude, double latitude, double dx, double dy){
        double radius_earth = 6378; // km

        double newLatitude = latitude + (dy / radius_earth) * (180 / Math.PI);
        double newLongitude = longitude + (dx / radius_earth) * (180 / Math.PI) / Math.cos(latitude * Math.PI / 180);

        return new double[]{newLongitude, newLatitude};
    }

    private String createCornerString(double longitude, double latitude, double height){
        String cornerStr = "";
        for (int i = 0; i < 3; ++i) {
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


    public JSONArray queryDistanceFilter(String cityObjectIri, Integer searchDistance){

        // Get Envelop Center
        // Get LowerBounds String
        // Get UpperBounds String

        String sparqlQuery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>\n" +
                "PREFIX geo: <http://www.bigdata.com/rdf/geospatial#>\n" +
                "PREFIX zo: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#>\n" +
                "PREFIX om: <http://www.ontology-of-units-of-measure.org/resource/om-2/>\n" +
                "PREFIX obs:<http://www.theworldavatar.com/ontology/ontobuildablespace/OntoBuildableSpace.owl#>\n" +
                "\n" +
                "SELECT ?obj (SAMPLE(?geoms) AS ?geom) (SAMPLE(?zones) AS ?zone) (MAX(?gfas) AS ?gfa)\n" +
                "WHERE {\n" +
                "GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/>\n" +
                "{ ?obj ocgml:EnvelopeType ?envelope .\n" +
                "  BIND(IRI(REPLACE(STR(?obj), \"cityobject\", \"genericcityobject\")) AS ?gen_obj) }\n" +
                "SERVICE geo:search {\n" +
                " ?obj geo:predicate ocgml:EnvelopeType .\n" +
                " ?obj geo:searchDatatype <http://localhost/blazegraph/literals/POLYGON-3-15> .\n" +
                " ?obj geo:customFields \"X0#Y0#Z0#X1#Y1#Z1#X2#Y2#Z2#X3#Y3#Z3#X4#Y4#Z4\" .\n" +
                " ?obj geo:customFieldsLowerBounds \"1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0#1.253371#103.774452#0\" .\n" +
                " ?obj geo:customFieldsUpperBounds \"1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000#1.335576#103.868351#1000\" .\n" +
                " hint:Prior hint:runFirst \"true\" . } \n" +
                " GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/surfacegeometry/> {\n" +
                "    ?surface ocgml:cityObjectId ?gen_obj ;\n" +
                "             ocgml:GeometryType ?geoms . }   \n" +
                "GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/> {\n" +
                "  ?obj zo:hasZone ?zone_uri . \n" +
                "  BIND(IRI(REPLACE(STR(?zone_uri), \"http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#\", \"\")) AS ?zones)} } \n" +
                "GROUP BY ?obj\n";

        return null;


    }

}
