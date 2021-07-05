package org.citydb.database.adapter.blazegraph;
// implemented by SHIYING LI

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_IsBlank;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.lang.arq.ARQParser;
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
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;

import org.citydb.database.adapter.blazegraph.GeoSpatialProcessor;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class StatementTransformer {
    private static final String IRI_GRAPH_BASE = "http://localhost/berlin/";
    private static final String PREFIX_ONTOCITYGML = "http://locahost/ontocitygml/";
    private static final String IRI_GRAPH_OBJECT_REL = "cityobject/";
    private static final String IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;

    public String sqlStatement;
    public String sparqlStatement;

    // create the query based on number of predicates and * for all
    public static String getTopFeatureId(SQLStatement sqlStatement){
        Select select = (Select) sqlStatement;
        List<PredicateToken> predicateTokens = select.getSelection();
        List<PlaceHolder<?>> placeHolders = new ArrayList<>();
        predicateTokens.get(0).getInvolvedPlaceHolders(placeHolders);
        if (placeHolders.get(0).getValue() == "*"){
            // input is * => retrieve all gmlId from database
        }else{

        }
        //queryObject_transformer
        return null;
    }

    // try test with multiple gmlId
    public static String getObjectId (){
        String sparql = "PREFIX  ocgml: <http://locahost/ontocitygml/>\n" +
                "SELECT  ?id ?objectclass_id ?gmlid\n" +
                "FROM <http://localhost/berlin/cityobject/>\n" +
                "WHERE  { ?id ocgml:objectClassId  ?objectclass_id .    \n" +
                "        ?id ocgml:gmlId ?gmlid    \n" +
                "        FILTER ( ?objectclass_id IN (64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26) ).    \n" +
                "        FILTER ( ?gmlid IN ( ? , ? ))  }";
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
                "?sg_id ocgml:rootId ?lod2MSid ; ocgml:GeometryType ?geomtype . FILTER(!isBlank(?geomtype))} }" +
                "LIMIT 1000";
        return sparql;
    }


    // Analyze SQL statement and transform it to a SPARQL query (Normal usuage: single gmlid or multiple gmlid)
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

        applyPredicate(sb, predicateTokens, placeHolders);


        Query q = sb.build();
        return q;
    }

    public static void applyPredicate (SelectBuilder sb, List<PredicateToken> predicateTokens, List<PlaceHolder<?>> placeHolders) throws ParseException {
        ExprFactory exprF = sb.getExprFactory();

        for (int i = 0; i < predicateTokens.size(); ++i) {
            PredicateToken predicateToken = predicateTokens.get(i);
            StringBuilder conditionStr = new StringBuilder();

            if (predicateToken instanceof InOperator) {
                conditionStr.append("?" + ((Column) ((InOperator) predicateToken).getOperand()).getName());
                conditionStr.append(" IN ");
                conditionStr.append("( ");
                if(((InOperator) predicateToken).getSubQueryExpression().toString().contains("?")){
                    int count = 0;
                    String searchStr = ((InOperator) predicateToken).getSubQueryExpression().toString();
                    for (int j = 0; j < searchStr.length(); j++) {
                        if (searchStr.charAt(j) == '?') {
                            count++;
                        }
                    }
                    if (placeHolders.size() == count) {
                        for (int k = 0; k < count; ++k) {
                            conditionStr.append("\"");
                            conditionStr.append(placeHolders.get(k).getValue().toString());
                            conditionStr.append("\"");
                            if (k < count-1){
                                conditionStr.append(",");
                            }
                        }
                    }else {
                        System.out.println("Number of ? does not match number of placeholder");
                    }

                }else {
                    conditionStr.append(((InOperator) predicateToken).getSubQueryExpression());
                }
                conditionStr.append(" )");
                sb.addFilter(conditionStr.toString());
                // if there is only 1 gmlid or * given as input
            } else if (predicateToken instanceof BinaryComparisonOperator) {
                List<PlaceHolder<?>> placeHolders1 = new ArrayList<>();
                predicateTokens.get(i).getInvolvedPlaceHolders(placeHolders1);
                if (placeHolders1.get(0).getValue().equals("*")) {
                    continue;
                }else{
                    conditionStr.append("?" + ((Column) ((BinaryComparisonOperator) predicateTokens.get(i)).getLeftOperand()).getName());
                    conditionStr.append(" " + ((BinaryComparisonOperator) predicateTokens.get(i)).getSQLName() + " ");
                    conditionStr.append("\"" + (String) placeHolders1.get(0).getValue() + "\"");
                    sb.addFilter(conditionStr.toString());
                }
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
    public static void main(String[] args) throws ParseException {
        String sqlquery =  "SELECT ST_Union(get_valid_area.simple_geom) " +
                "FROM (SELECT * FROM (SELECT * FROM (SELECT ST_Force2D(sg.geometry) AS simple_geom " +
                "FROM citydb.SURFACE_GEOMETRY sg WHERE sg.root_id IN( " +
                "SELECT b.lod2_multi_surface_id FROM citydb.BUILDING b WHERE b.id = ? AND b.lod2_multi_surface_id IS NOT NULL " +
                "UNION SELECT b.lod2_solid_id FROM citydb.BUILDING b WHERE b.id = ? AND b.lod2_solid_id IS NOT NULL " +
                "UNION SELECT ts.lod2_multi_surface_id FROM citydb.THEMATIC_SURFACE ts WHERE ts.building_id = ? " +
                "AND ts.lod2_multi_surface_id IS NOT NULL ) AND sg.geometry IS NOT NULL) AS get_geoms " +
                "WHERE ST_IsValid(get_geoms.simple_geom) = 'TRUE') AS get_valid_geoms " +
                "WHERE ST_Area(ST_Transform(get_valid_geoms.simple_geom,4326)::geography, true) > 0.001) AS get_valid_area";
        String output = getSPARQLqueryStage2(sqlquery, String.valueOf(2));

    }

    public static String getSPARQLqueryStage2 (String inputquery, String LoD) throws ParseException {

        //String buildingId = "<http://localhost/berlin/building/ID_0518100000225439/>";
        String buildingId = "?";
        String lodXMultiSurfaceId = "lod<LoD>MultiSurfaceId";
        String lodXSolidId = "lod<LoD>SolidId";
        lodXMultiSurfaceId = lodXMultiSurfaceId.replace("<LoD>", LoD);
        lodXSolidId = lodXSolidId.replace("<LoD>", LoD);

        // subquery 1.1
        // SELECT b.lod2_multi_surface_id FROM citydb.BUILDING b WHERE b.id = 360 AND b.lod2_multi_surface_id IS NOT NULL
        SelectBuilder subquery1 = new SelectBuilder();
        ExprFactory exprF1 = subquery1.getExprFactory();
        subquery1.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        Node graphName1 = NodeFactory.createURI("http://localhost/berlin/building/");
        subquery1.addVar(exprF1.asExpr("?" + lodXMultiSurfaceId), "rootId");
        WhereBuilder whr1 = new WhereBuilder().addGraph(graphName1,new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", buildingId).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + lodXMultiSurfaceId, "?"+lodXMultiSurfaceId).addFilter("!isBlank(?"+ lodXMultiSurfaceId+")"));
        subquery1.addWhere(whr1);

        // subquery 1.2
        // SELECT b.lod2_solid_id FROM citydb.BUILDING b WHERE b.id = 360 AND b.lod2_solid_id IS NOT NULL
        SelectBuilder subquery2 = new SelectBuilder();
        ExprFactory exprF2 = subquery2.getExprFactory();
        subquery2.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        Node graphName2 = NodeFactory.createURI("http://localhost/berlin/building/");
        subquery2.addVar(exprF2.asExpr("?"+lodXSolidId), "rootId");
        WhereBuilder whr2 = new WhereBuilder().addGraph(graphName2,new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", buildingId).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + lodXSolidId, "?"+lodXSolidId).addFilter("!isBlank(?"+lodXSolidId+")"));
        subquery2.addWhere(whr2);

        // subquery 1.3
        // SELECT ts.lod2_multi_surface_id FROM citydb.THEMATIC_SURFACE ts WHERE ts.building_id = 360 AND ts.lod2_multi_surface_id IS NOT NULL
        SelectBuilder subquery3 = new SelectBuilder();
        ExprFactory exprF3 = subquery3.getExprFactory();
        subquery3.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        Node graphName3 = NodeFactory.createURI("http://localhost/berlin/thematicsurface/");
        subquery3.addVar(exprF3.asExpr("?"+lodXMultiSurfaceId), "rootId");
        WhereBuilder whr3 = new WhereBuilder().addGraph(graphName3, new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", buildingId).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + lodXMultiSurfaceId, "?"+lodXMultiSurfaceId).addFilter("!isBlank(?"+ lodXMultiSurfaceId+ ")"));
        subquery3.addWhere(whr3);

        // query 2
        SelectBuilder query2 = new SelectBuilder();
        query2.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        query2.addVar("?geometry").from("http://localhost/berlin/surfacegeometry/");
        query2.addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "rootId", "?rootId");
        query2.addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "GeometryType", "?geometry");

        try {
            query2.addFilter("!isBlank(?geometry)");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        WhereBuilder sb = new WhereBuilder();
        sb.addUnion(subquery1);
        sb.addUnion(subquery2);
        sb.addUnion(subquery3);
        query2.addWhere(sb);

        return query2.toString();
    }

    public static Geometry Str2Geometry (String extracted){
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        Geometry geomobj = geospatial.createGeometry(extracted);
        return geomobj;
    }

    public static Geometry filterResult(List<String> extracted, double tolerance) {

        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        List<Geometry> geom2union = new ArrayList<>();

        for (int i = 0; i < extracted.size(); ++i){
            Geometry geomobj = geospatial.createGeometry(extracted.get(i));
            if (geospatial.IsValid(geomobj) && geospatial.CalculateArea(geospatial.Transform(geomobj, 4326, 4326)) > tolerance){
                geom2union.add(geomobj);
            }
        }

        Geometry union = geospatial.UnaryUnion(geom2union);

        return union;
    }

}
