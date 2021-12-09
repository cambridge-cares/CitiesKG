package org.citydb.database.adapter.blazegraph;
// implemented by SHIYING LI

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.config.project.kmlExporter.DisplayForm;
import org.citydb.config.project.kmlExporter.KmlExporter;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
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
    private static String IRI_GRAPH_BASE;
    private static String PREFIX_ONTOCITYGML;
    private static String IRI_GRAPH_OBJECT_REL = "cityobject/";
    private static String IRI_GRAPH_OBJECT;

    public String sqlStatement;
    public String sparqlStatement;
    private static AbstractDatabaseAdapter databaseAdapter;

    public StatementTransformer(AbstractDatabaseAdapter databaseAdapter) {
        this.databaseAdapter = databaseAdapter;

        // Note: Read the database connection information from the database GUI setting
        PREFIX_ONTOCITYGML  = databaseAdapter.getConnectionDetails().getSchema();
        IRI_GRAPH_BASE = "http://" + databaseAdapter.getConnectionDetails().getServer() +
                ":" + databaseAdapter.getConnectionDetails().getPort() +
                databaseAdapter.getConnectionDetails().getSid();
        IRI_GRAPH_OBJECT = IRI_GRAPH_BASE + IRI_GRAPH_OBJECT_REL;
    }

    /* GenericCityObject */
    public static String getGenericCityObjectBasisData(int lodToExportFrom){
        String sparql = "PREFIX  ocgml: <" + PREFIX_ONTOCITYGML + "> \n" +
                "SELECT ?id ?lod1ImplicitRefPoint ?lod1ImplicitTransformation ?lod1BrepId \n" +
                "FROM <" + IRI_GRAPH_BASE + "genericcityobject/" + "> \n" +
                "WHERE { ?id ocgml:lod1ImplicitRefPoint ?lod1ImplicitRefPoint;\n" +
                "ocgml:lod1ImplicitTransformation ?lod1ImplicitTransformation;\n" +
                "ocgml:lod1BrepId ?lod1BrepId;\n" +
                "ocgml:id ? . }";
        return sparql;
    }

    public static String getGenericCityObjectQuery(int lodToExportFrom, DisplayForm displayForm, boolean isImplicit, boolean exportAppearance){
        String query = null;

        switch (displayForm.getForm()) {
            case DisplayForm.FOOTPRINT:
            case DisplayForm.EXTRUDED:
                query = getSurfaceGeometries(exportAppearance, isImplicit);
                break;
            default:
                StringBuilder tmp = new StringBuilder().append("PREFIX  ocgml: <" + PREFIX_ONTOCITYGML + "> \n")
                        .append("SELECT ?, '5' as ?objectclass_id "); // dummy
                if (databaseAdapter.getSQLAdapter().requiresPseudoTableInSelect())
                    tmp.append(" FROM ")
                            .append(databaseAdapter.getSQLAdapter().getPseudoTableName());

                query = tmp.toString();
        }

        return query;
    }

    public static String getSurfaceGeometries(boolean exportAppearance, boolean isImplicit){
        //TODO: translate the SQL query to SPARQL
        StringBuilder query = new StringBuilder()
                .append("select ")
                .append(isImplicit ? "sg.implicit_geometry" : "sg.geometry")
                .append(", sg.id, sg.parent_id, sg.root_id, sg.gmlid, sg.is_xlink ");
        /*
        if (exportAppearance) {
            query.append(", sd.x3d_shininess, sd.x3d_transparency, sd.x3d_ambient_intensity, ")
                    .append("sd.x3d_specular_color, sd.x3d_diffuse_color, sd.x3d_emissive_color, sd.x3d_is_smooth, ")
                    .append("sd.tex_image_id, ti.tex_image_uri, tp.texture_coordinates, coalesce(a.theme, '<unknown>') theme ");
        }
        */
        //query.append("FROM ").append(schema).append(".surface_geometry sg ");
        /*
        if (exportAppearance) {
            query.append("LEFT JOIN ").append(schema).append(".textureparam tp ON tp.surface_geometry_id = sg.id ")
                    .append("LEFT JOIN ").append(schema).append(".surface_data sd ON sd.id = tp.surface_data_id ")
                    .append("LEFT JOIN ").append(schema).append(".tex_image ti ON ti.id = sd.tex_image_id ")
                    .append("LEFT JOIN ").append(schema).append(".appear_to_surface_data a2sd ON a2sd.surface_data_id = sd.id ")
                    .append("LEFT JOIN ").append(schema).append(".appearance a ON a2sd.appearance_id = a.id ");
        }
        */
        query.append("WHERE sg.root_id = ? ")
                .append("ORDER BY sg.id");

        return query.toString();

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


    // Analyze SQL statement and transform it to a SPARQL query (Normal usuage: single gmlid or multiple gmlid or *)
    public static String getTopFeatureId (SQLStatement sqlStatement) throws ParseException {
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
    }
}
