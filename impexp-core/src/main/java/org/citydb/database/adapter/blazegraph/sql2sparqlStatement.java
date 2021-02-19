package org.citydb.database.adapter.blazegraph;
// implemented by SHIYING LI
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
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
        sb.addVar("?id ?objectclass_id ?gmlid").from(IRI_GRAPH_OBJECT).addWhere("?gmlid", "<http://locahost/ontocitygml/objectClassId>", "?objectclass_id").addWhere("?gmlid", "<http://locahost/ontocitygml/gmlId>", "?name" ); //"ID_0518100000225439"
        try {
            sb.addFilter("?objectclass_id IN ( 64, 4, 5, 7, 8, 9, 42, 43,44, 45, 14, 46, 85, 21, 23, 26 )");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        sb.setVar(Var.alloc("?name"), "ID_0518100000225439");
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
