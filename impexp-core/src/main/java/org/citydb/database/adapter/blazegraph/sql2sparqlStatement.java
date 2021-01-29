package org.citydb.database.adapter.blazegraph;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;


public class sql2sparqlStatement {
    public String sqlStatement;
    public String sparqlStatement;

    public String sql2sparqlStatement (String sqlStatement){
        SelectBuilder sb = new SelectBuilder();
        sb.addVar("*").addWhere("?s", "?p", "?o");
        Query q = sb.build();
        return q.toString();
    }
}
