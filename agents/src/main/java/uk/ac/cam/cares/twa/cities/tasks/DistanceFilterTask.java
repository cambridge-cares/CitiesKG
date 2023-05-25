package uk.ac.cam.cares.twa.cities.tasks;


import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;

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
                "  GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql//buildablespace2/>\n" +
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

}
