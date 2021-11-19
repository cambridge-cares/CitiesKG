package org.citydb.database.adapter.blazegraph;
// implemented by SHIYING LI

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.schema.mapping.MappingConstants;
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

import java.sql.Connection;
import java.util.*;


public class StatementTransformer {
    private static final String QST_MARK = "?";
    private static String IRI_GRAPH_BASE;
    private static String PREFIX_ONTOCITYGML;
    private static String IRI_GRAPH_OBJECT_REL = "cityobject/";
    private static String IRI_GRAPH_OBJECT;

    public String sqlStatement;
    public String sparqlStatement;
    private final AbstractDatabaseAdapter databaseAdapter;

    public StatementTransformer(AbstractDatabaseAdapter databaseAdapter) {
        this.databaseAdapter = databaseAdapter;

        // Note: Read the database connection information from the database GUI setting
        PREFIX_ONTOCITYGML  = databaseAdapter.getConnectionDetails().getSchema();
        IRI_GRAPH_BASE = "http://" + databaseAdapter.getConnectionDetails().getServer() +
                ":" + databaseAdapter.getConnectionDetails().getPort() +
                databaseAdapter.getConnectionDetails().getSid();
        IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
    }

    public static String getIriObjectBase(){
        return IRI_GRAPH_BASE;
    }

    public static String getExtrusionHeight(){
        String sparql = "PREFIX  ocgml: <" + PREFIX_ONTOCITYGML + ">" +
                        "SELECT  ?envelope " +
                        "FROM <" + IRI_GRAPH_BASE + "cityobject/" + ">" +
                        "WHERE { ?s ocgml:EnvelopeType ?envelope ; ocgml:id ? . }";
        return sparql;
    }

    // Input is String, can not use the statementAnalyzer to retrieve the component
    public static String getSPARQLStatement_BuildingParts (String sqlQuery) {
        String sparql = "PREFIX  ocgml: <" + PREFIX_ONTOCITYGML + "> " +
                        "SELECT * " +
                        "FROM  <" + IRI_GRAPH_BASE + "building/" + "> " +
                        "WHERE { " +
                        "?id ocgml:buildingRootId ? .}";

        return sparql;
    }


    public static String getSPARQLStatement_BuildingPartQuery (String sqlQuery) {
        StringBuilder sparqlString = new StringBuilder();

        if (IRI_GRAPH_BASE.contains("theworldavatar")){
            sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
                    "SELECT ?fixedlod2MSid " +
                    "WHERE {" +
                    "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> {" +
                    "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ;ocgml:lod2MultiSurfaceId ?lod2MSid .}" +
                    "BIND(IRI(CONCAT(STR(?lod2MSid), '/')) AS ?fixedlod2MSid) }");

        }else {
            sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
                    "SELECT ?geomtype (datatype(?geomtype) AS ?type)" +
                    "WHERE {" +
                    "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> {" +
                    "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ;ocgml:lod2MultiSurfaceId ?lod2MSid .}" +
                    "GRAPH <" + IRI_GRAPH_BASE + "surfacegeometry/> {" +
                    "?sg_id ocgml:rootId ?lod2MSid; ocgml:GeometryType ?geomtype . FILTER(!isBlank(?geomtype))} }");
        }

        return sparqlString.toString();
    }

    // Temporary solution for TWA:
    public static String getSPARQLStatement_BuildingPartQuery_part2 () {
        StringBuilder sparqlString = new StringBuilder();
        sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
                "SELECT ?geomtype (datatype(?geomtype) AS ?type)" +
                "WHERE {" +
                "GRAPH <" + IRI_GRAPH_BASE + "surfacegeometry/> {" +
                "?sg_id ocgml:rootId ?; ocgml:GeometryType ?geomtype . FILTER(!isBlank(?geomtype))} }");
        return sparqlString.toString();
    }

    public static String getSPARQLStatement_BuildingPartQuery_bak (String sqlQuery) {
        String SparqlString = null;
        SelectBuilder sb = new SelectBuilder();
        ExprFactory exprF1 = sb.getExprFactory();
        sb.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        sb.addVar("?geomtype").addVar(exprF1.datatype("?geomtype"),"?type");

        Node graphName1 = NodeFactory.createURI(IRI_GRAPH_BASE + "thematicsurface/");
        WhereBuilder subwhr1 = new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        subwhr1.addWhere("?ts_id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "objectClassId", "35");
        subwhr1.addWhere("?ts_id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", "?");
        subwhr1.addWhere("?ts_id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "lod2MultiSurfaceId", "?lod2MSid");
        WhereBuilder whr1 = new WhereBuilder().addGraph(graphName1, subwhr1);
        sb.addWhere(whr1);

        Node graphName2 = NodeFactory.createURI(IRI_GRAPH_BASE + "surfacegeometry/");
        WhereBuilder subwhr2 = new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        WhereBuilder whr2 = new WhereBuilder().addGraph(graphName2, subwhr2);
        subwhr2.addWhere("?sg_id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "rootId", "?lod2MSid");
        subwhr2.addWhere("?sg_id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "GeometryType", "?geomtype");
        try {
            subwhr2.addFilter("!isBlank(?geomtype)");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        sb.addWhere(whr2);
        SparqlString = sb.build().toString();
        return SparqlString;
    }

    /*
    * PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>
      SELECT  ?id ?objectclass_id (? AS ?gmlid)
      FROM <http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/cityobject/>
      WHERE
        { ?id ocgml:objectClassId  ?objectclass_id .
          ?id ocgml:gmlId ?gmlid
          FILTER ( ?objectclass_id IN (64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26) ).
          FILTER ( ?gmlid =  ?)
         }
      * */
    public static String getTopFeatureId(SQLStatement sqlStatement) throws ParseException {
        StringBuilder sparqlString = new StringBuilder();

        // for single object
        sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> \n" +
            "SELECT ?id ?objectclass_id (" + QST_MARK + " AS ?gmlid) \n" +
            "FROM <" + IRI_GRAPH_BASE + "cityobject/> \n" +
            "\nWHERE\n " +
            "{ ?id ocgml:objectClassId  ?objectclass_id ;\n ocgml:gmlId " + QST_MARK + "\n" +
            "FILTER ( ?objectclass_id IN (64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26) )\n }");

        return sparqlString.toString();
    }

    // Analyze SQL statement and transform it to a SPARQL query (Normal usuage: single gmlid or multiple gmlid or *)
    public static String getTopFeatureId_bak(SQLStatement sqlStatement) throws ParseException {
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
        return q.toString();
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
                continue;
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

    /* Optimized SPARQL query for TWA
     * Purpose: Get AggregateGeometries to create groundsurface for the extraction
     * alternative solution of getSPARQLqueryStage2
     * This part will include the value assignment and execution
     */

    public static ArrayList<ResultSet> getSPARQLAggregateGeometriesForLOD2OrHigher(PreparedStatement psQuery, Connection connection, int lodToExportFrom, String buildingPartId) {

        StringBuilder sparqlString = new StringBuilder();
        ResultSet rs = null;
        ArrayList<String> rootIds = new ArrayList<String>();

        // subquery 1.1
        sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
            "SELECT (?lod2MultiSurfaceId AS ?rootId) " +
            "\nWHERE\n { " +
                "GRAPH <" + IRI_GRAPH_BASE + "building/> { \n" +
            " ?id ocgml:buildingId " +  QST_MARK + " ;\n  ocgml:lod2MultiSurfaceId ?lod2MultiSurfaceId " +
            "FILTER (!isBlank(?lod2MultiSurfaceId)) }}");
        rootIds.addAll(executeQuery(connection, sparqlString.toString(), buildingPartId));

        // subquery 1.2
        sparqlString.setLength(0);
        sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
            "SELECT (?lod2SolidId AS ?rootId) " +
            "\nWHERE\n { " +
            "GRAPH <" + IRI_GRAPH_BASE + "building/> { \n" +
            " ?id ocgml:buildingId " +  QST_MARK + " ;  \n ocgml:lod2SolidId  ?lod2SolidId\n" +
            "FILTER (!isBlank(?lod2SolidId)) }}");
        rootIds.addAll(executeQuery(connection, sparqlString.toString(), buildingPartId));

        // subquery 1.3
        sparqlString.setLength(0);
        sparqlString.append("PREFIX ocgml: <" + PREFIX_ONTOCITYGML + "> " +
            "SELECT (?lod2MultiSurfaceId AS ?rootId) " +
            "\nWHERE\n { " +
            "GRAPH <" + IRI_GRAPH_BASE + "thematicsurface/> { \n" +
            " ?id ocgml:buildingId " +  QST_MARK + " ;  \n ocgml:lod2MultiSurfaceId  ?lod2MultiSurfaceId\n" +
            "FILTER (!isBlank(?lod2MultiSurfaceId)) }}");
        rootIds.addAll(executeQuery(connection, sparqlString.toString(), buildingPartId));

        // query stage 2 for extractig the aggregated geometries
        System.out.println(rootIds.size());
        return null;
    }

    public static ArrayList<String> executeQuery(Connection connection, String querystr, String buildingPartId){

        URL url = null;
        ResultSet rs = null;
        ArrayList<String> results = new ArrayList<String>();
        try {
            url = new URL(buildingPartId);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            PreparedStatement psQuery = connection.prepareStatement(querystr, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            psQuery.setURL(1, url);
            rs = psQuery.executeQuery();

            while (rs.next()) {
                results.add(rs.getString("rootId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //@TODO: to define how to handle
        }
        return results;
    }


    // Get AggregateGeometries to create groundsurface for the extraction
    public static String getSPARQLqueryStage2 (String sqlquery, String LoD) throws ParseException {

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
        Node graphName1 = NodeFactory.createURI(IRI_GRAPH_BASE + "building/");
        subquery1.addVar(exprF1.asExpr("?" + lodXMultiSurfaceId), "rootId");
        WhereBuilder whr1 = new WhereBuilder().addGraph(graphName1,new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", buildingId).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + lodXMultiSurfaceId, "?"+lodXMultiSurfaceId).addFilter("!isBlank(?"+ lodXMultiSurfaceId+")"));
        subquery1.addWhere(whr1);

        // subquery 1.2
        // SELECT b.lod2_solid_id FROM citydb.BUILDING b WHERE b.id = 360 AND b.lod2_solid_id IS NOT NULL
        SelectBuilder subquery2 = new SelectBuilder();
        ExprFactory exprF2 = subquery2.getExprFactory();
        subquery2.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        Node graphName2 = NodeFactory.createURI(IRI_GRAPH_BASE + "building/");
        subquery2.addVar(exprF2.asExpr("?"+lodXSolidId), "rootId");
        WhereBuilder whr2 = new WhereBuilder().addGraph(graphName2,new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", buildingId).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + lodXSolidId, "?"+lodXSolidId).addFilter("!isBlank(?"+lodXSolidId+")"));
        subquery2.addWhere(whr2);

        // subquery 1.3
        // SELECT ts.lod2_multi_surface_id FROM citydb.THEMATIC_SURFACE ts WHERE ts.building_id = 360 AND ts.lod2_multi_surface_id IS NOT NULL
        SelectBuilder subquery3 = new SelectBuilder();
        ExprFactory exprF3 = subquery3.getExprFactory();
        subquery3.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        Node graphName3 = NodeFactory.createURI(IRI_GRAPH_BASE + "thematicsurface/");
        subquery3.addVar(exprF3.asExpr("?"+lodXMultiSurfaceId), "rootId");
        WhereBuilder whr3 = new WhereBuilder().addGraph(graphName3, new WhereBuilder().addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId", buildingId).addWhere("?id", SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + lodXMultiSurfaceId, "?"+lodXMultiSurfaceId).addFilter("!isBlank(?"+ lodXMultiSurfaceId+ ")"));
        subquery3.addWhere(whr3);

        // query 2
        SelectBuilder query2 = new SelectBuilder();
        query2.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, PREFIX_ONTOCITYGML);
        query2.addVar("?geometry").from(IRI_GRAPH_BASE + "surfacegeometry/");
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

    public static Geometry Str2Geometry (String extracted, String datatypeURI){
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();

        if (datatypeURI == null){
            Geometry geomobj = geospatial.createGeometry(extracted);
            return geomobj;

        }else{  // if the polygon contains multiple rings of the dimension X1, X2
            // extract the pattern "POLYGON-X1-X2" which is the last part of the URI
            String datatype = datatypeURI.substring(datatypeURI.lastIndexOf('/') + 1);
            String[] datatype_list = datatype.split("-");
            String geomtype = datatype_list[0];
            int dim = Integer.valueOf(datatype_list[1]);
            int[] dimOfRings = new int[datatype_list.length-2];
            for (int i = 2; i < datatype_list.length; ++i){
                dimOfRings[i-2] = Integer.valueOf(datatype_list[i]);
            }
            // put in createGeopmetry (extracted, geomtype, listOfDim)
            Geometry geomobj = geospatial.createGeometry(extracted, geomtype, dim, dimOfRings);
            return geomobj;
        }
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

    // For testing purpose
    /*
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
        String output = getSPARQLqueryStage2(sqlquery, "2");
        System.out.println(output);
    }*/
}
