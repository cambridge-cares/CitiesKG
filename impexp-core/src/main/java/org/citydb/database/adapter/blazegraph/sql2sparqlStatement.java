package org.citydb.database.adapter.blazegraph;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;


public class sql2sparqlStatement {
    private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
    private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
    private static final String IRI_GRAPH_OBJECT_REL = "cityobject/";
    private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;

    public String sqlStatement;
    public String sparqlStatement;

    public static String transformer1 (String sqlStatement){
        SelectBuilder sb = new SelectBuilder();
        sb.addPrefix("ocgml", "http://locahost/ontocitygml/");
        sb.addVar("*").addWhere("?s", "?p", "?o").setLimit(10);
        Query q = sb.build();
        return q.toString();
    }
    public static String transformer2 (String sqlStatement){
        String param = "  ?;";
        String stmt = "PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
                "BASE <" + IRI_GRAPH_BASE + "> " +
                "SELECT *" +
                "WHERE {?s ?p ?o }" +
                "LIMIT 10";

        return stmt;
    }
}
