package org.citydb.database.adapter.blazegraph;
// implemented by SHIYING LI

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.sqlbuilder.SQLStatement;
import org.citydb.sqlbuilder.expression.PlaceHolder;
import org.citydb.sqlbuilder.schema.Column;
import org.citydb.sqlbuilder.schema.Table;
import org.citydb.sqlbuilder.select.PredicateToken;
import org.citydb.sqlbuilder.select.ProjectionToken;
import org.citydb.sqlbuilder.select.Select;
import org.citydb.sqlbuilder.select.operator.comparison.BinaryComparisonOperator;
import org.citydb.sqlbuilder.select.operator.comparison.InOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class StatementTransformer {
    private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
    private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
    private static final String IRI_GRAPH_OBJECT_REL = "cityobject/";
    private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;

    public String sqlStatement;
    public String sparqlStatement;

    public static String getSparqlstatement (Query query){
        SelectBuilder sb = new SelectBuilder();
        sb.addPrefix("ocgml", "http://locahost/ontocitygml/");
        sb.addVar("?id ?objectclass_id ?gmlid").from(IRI_GRAPH_OBJECT).addWhere("?gmlid", "<http://locahost/ontocitygml/objectClassId>", "?objectclass_id").addWhere("?gmlid", "<http://locahost/ontocitygml/gmlId>", "?name" ); //"ID_0518100000225439"
        try {
            sb.addFilter("?objectclass_id IN ( 64, 4, 5, 7, 8, 9, 42, 43,44, 45, 14, 46, 85, 21, 23, 26 )");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        sb.setVar(Var.alloc("name"), "ID_0518100000225439");
        Query q = sb.build();
        return q.toString();
    }
    // getBuildingPartAggregateGeometries
    public static String getSPARQLStatement_BuildingPartAggregateGeometries (String sqlquery){
        String sparql = "";
        return sparql;
    }



    // getBuildingPartsFromBuilding() in Building.java
    public static String getSPARQLStatement_BuildingParts (String sqlQuery) {
        String sparql = "PREFIX  ocgml: <http://locahost/ontocitygml/> " +
                "SELECT * WHERE { " +
                "GRAPH <http://localhost/berlin/building/>{ " +
                "?id ocgml:buildingRootId ? . } }";

        return sparql;
    }

    //getBuildingPartQuery() in Building.java
    public static String getSPARQLStatement_BuildingPartQuery (String sqlQuery) {
        String sparql = "PREFIX  ocgml: <http://locahost/ontocitygml/> SELECT ?geomtype WHERE { GRAPH <http://localhost/berlin/thematicsurface/> { ?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ; ocgml:lod2MultiSurfaceId ?lod2MSid .} GRAPH <http://localhost/berlin/surfacegeometry/> {" +
                "?sg_id ocgml:rootId ?lod2MSid ; ocgml:GeometryType ?geomtype . } }" +
                "LIMIT 1000";
        return sparql;
    }





    // Analyze SQL statement and transform it to a SPARQL query
    public static Query queryObject_transformer (SQLStatement sqlStatement) throws ParseException {
        Select select = (Select) sqlStatement;
        List<ProjectionToken> projectionTokens = select.getProjection();
        Set<Table> InvolvedTables = sqlStatement.getInvolvedTables();
        List<PredicateToken> predicateTokens = select.getSelection();

        // Build SPARQL query statement
        SelectBuilder sb = new SelectBuilder();
        sb.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        String varnames = getVarNames(projectionTokens);
        String iri_graph_object = getGraphUri(InvolvedTables);
        sb.addVar(varnames).from(iri_graph_object);

        sb.addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "objectClassId", "?objectclass_id");
        sb.addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "gmlId", "?gmlid");
        List<PlaceHolder<?>> placeHolders = sqlStatement.getInvolvedPlaceHolders();

        applyPredicate(sb, predicateTokens);


        Query q = sb.build();
        return q;
    }

    public static String queryTransform (String sqlquery){
        //Force2D instance = new Force2D();
        return null;
    }

    public static void applyPredicate (SelectBuilder sb, List<PredicateToken> predicateTokens) throws ParseException {

        for (int i = 0; i < predicateTokens.size(); ++i) {
            PredicateToken predicateToken = predicateTokens.get(i);
            StringBuilder conditionStr = new StringBuilder();

            if (predicateToken instanceof InOperator) {
                conditionStr.append("?" + ((Column) ((InOperator) predicateToken).getOperand()).getName());
                conditionStr.append(" IN ");
                conditionStr.append("(" + ((InOperator) predicateToken).getSubQueryExpression() + ")");
                sb.addFilter(conditionStr.toString());
            } else if (predicateToken instanceof BinaryComparisonOperator) {

                conditionStr.append("?" + ((Column) ((BinaryComparisonOperator) predicateTokens.get(i)).getLeftOperand()).getName());
                conditionStr.append(" " + ((BinaryComparisonOperator) predicateTokens.get(i)).getSQLName() + " ");
                List<PlaceHolder<?>> placeHolders = new ArrayList<>();
                predicateTokens.get(i).getInvolvedPlaceHolders(placeHolders);
                conditionStr.append("\"" + (String) placeHolders.get(0).getValue() + "\"");
                sb.addFilter(conditionStr.toString());

            } else {

            }
        }

    }

    public static String getGraphUri (Set<Table> InvolvedTables) {
        String graph_rel = InvolvedTables.iterator().next().getName();
        String iri_graph = IRI_GRAPH_BASE + graph_rel + "/";
        return iri_graph;
    }

    public static String getVarNames (List<ProjectionToken> projectionTokens) {
        ArrayList<String> varNames = new ArrayList<String>();
        StringBuilder varStr = new StringBuilder();
        int total = projectionTokens.size();
        String param = "?";

        for (int i = 0; i < total; i++) {
            String var = ((Column) projectionTokens.get(i)).getName();
            varNames.add(var);
            varStr.append(param).append(var);
            if (i != total-1){
                varStr.append(" ");
            }
        }
        return varStr.toString();
    }
}