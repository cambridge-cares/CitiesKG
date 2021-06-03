package org.citydb.database.adapter.blazegraph;
// implemented by SHIYING LI

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.NodeValue;
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

import java.util.*;


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

    public static void main(String[] args) {
        checkValidTest();
    }

    public static void checkValidTest() {
            String blaze_geometry = "385378.016138475#5819494.72358936#36.5200004577637#385378.143842939#5819493.07912378#36.5200004577637#" +
                    "385378.786499313#5819484.76944437#36.5200004577637#385317.300349907#5819479.76855148#36.5200004577637#" +
                    "385316.14659639#5819479.67454114#36.5200004577637#385316.343807439#5819477.03355724#36.5200004577637#" +
                    "385301.703458502#5819476.19301668#36.5200004577637#385300.950074006#5819486.31327541#36.5200004577637#" +
                    "385307.09120739#5819486.77136495#36.5200004577637#385307.460985192#5819481.81075803#36.5200004577637#" +
                    "385308.693746104#5819481.90326643#36.5200004577637#385308.45066795#5819485.1666353#36.5200004577637#" +
                    "385316.810961469#5819485.78848991#36.5200004577637#385316.490861222#5819489.71917047#36.5200004577637#" +
                    "385324.704941389#5819490.38683337#36.5200004577637#385324.526018414#5819492.5952832#36.5200004577637#" +
                    "385330.77917557#5819493.10311801#36.5200004577637#385330.959341237#5819490.89586548#36.5200004577637#" +
                    "385344.583365563#5819492.00379023#36.5200004577637#385344.403522196#5819494.21195249#36.5200004577637#" +
                    "385350.971202767#5819494.74706581#36.5200004577637#385351.151327801#5819492.53767831#36.5200004577637#" +
                    "385364.669308965#5819493.63754988#36.5200004577637#385364.489178164#5819495.84663344#36.5200004577637#" +
                    "385371.115215387#5819496.38551926#36.5200004577637#385371.294125703#5819494.17645895#36.5200004577637#" +
                    "385378.016138475#5819494.72358936#36.5200004577637";
            String noValid_geometry = "82120.313 454255.538 130.672,82120.672 454254.897 130.672,82120.672 454254.897 -0.526," +
                    "82120.313 454255.538 -0.526,82120.313 454255.538 130.672";
            String valid_geometry = "82106.765 454221.659 -0.526,82107.759 454227.024 -0.526,82108.99 454233.752 -0.526,82109.056 454234.11 -0.526," +
                    "82109.301 454235.449 -0.526,82110.361 454241.194 -0.526,82110.597 454242.533 -0.526,82111.667 454248.272 -0.526,82111.79 454248.937 -0.526," +
                    "82111.586 454248.97 -0.526,82112.523 454254.297 -0.526,82112.779 454254.933 -0.526,82113.133 454255.521 -0.526,82113.576 454256.045 -0.526," +
                    "82114.095 454256.492 -0.526,82114.679 454256.852 -0.526,82115.312 454257.115 -0.526,82115.979 454257.275 -0.526,82116.679 454257.347 -0.526," +
                    "82117.38 454257.306 -0.526,82118.066 454257.153 -0.526,82118.719 454256.891 -0.526,82119.321 454256.528 -0.526,82119.857 454256.073 -0.526," +
                    "82120.313 454255.538 -0.526,82120.672 454254.897 -0.526,82120.41 454254.77 -0.526,82139.425 454221.057 -0.526,82139.823 454220.351 -0.526," +
                    "82140.059 454219.694 -0.526,82140.175 454219.006 -0.526,82140.168 454218.309 -0.526,82140.038 454217.623 -0.526,82139.79 454216.971 -0.526," +
                    "82139.43 454216.373 -0.526,82138.971 454215.848 -0.526,82138.429 454215.447 -0.526,82137.847 454215.151 -0.526,82137.222 454214.961 -0.526," +
                    "82136.574 454214.882 -0.526,82135.922 454214.916 -0.526,82134.585 454215.166 -0.526,82132.371 454215.483 -0.526,82131.031 454215.731 -0.526," +
                    "82130.68 454215.789 -0.526,82125.286 454216.795 -0.526,82123.94 454217.036 -0.526,82118.21 454218.086 -0.526,82116.869 454218.345 -0.526," +
                    "82111.134 454219.407 -0.526,82109.79 454219.651 -0.526,82107.919 454219.98 -0.526,82106.507 454220.236 -0.526,82106.765 454221.659 -0.526";
        String st_geometry1 = "82106.765 454221.659 -0.526,82107.759 454227.024 -0.526,82108.99 454233.752 -0.526,82109.056 454234.11 -0.526," +
                "82109.301 454235.449 -0.526,82110.361 454241.194 -0.526,82110.597 454242.533 -0.526,82111.667 454248.272 -0.526,82111.79 454248.937 -0.526," +
                "82111.586 454248.97 -0.526,82112.523 454254.297 -0.526,82112.779 454254.933 -0.526,82113.133 454255.521 -0.526,82113.576 454256.045 -0.526," +
                "82114.095 454256.492 -0.526,82114.679 454256.852 -0.526,82115.312 454257.115 -0.526,82115.979 454257.275 -0.526,82116.679 454257.347 -0.526," +
                "82117.38 454257.306 -0.526,82118.066 454257.153 -0.526,82118.719 454256.891 -0.526,82119.321 454256.528 -0.526,82119.857 454256.073 -0.526," +
                "82120.313 454255.538 -0.526,82120.672 454254.897 -0.526,82120.41 454254.77 -0.526,82139.425 454221.057 -0.526,82139.823 454220.351 -0.526," +
                "82140.059 454219.694 -0.526,82140.175 454219.006 -0.526,82140.168 454218.309 -0.526,82140.038 454217.623 -0.526,82139.79 454216.971 -0.526," +
                "82139.43 454216.373 -0.526,82138.971 454215.848 -0.526,82138.429 454215.447 -0.526,82137.847 454215.151 -0.526,82137.222 454214.961 -0.526," +
                "82136.574 454214.882 -0.526,82135.922 454214.916 -0.526,82134.585 454215.166 -0.526,82132.371 454215.483 -0.526,82131.031 454215.731 -0.526," +
                "82130.68 454215.789 -0.526,82125.286 454216.795 -0.526,82123.94 454217.036 -0.526,82118.21 454218.086 -0.526,82116.869 454218.345 -0.526," +
                "82111.134 454219.407 -0.526,82109.79 454219.651 -0.526,82107.919 454219.98 -0.526,82106.507 454220.236 -0.526,82106.765 454221.659 -0.526";
        String st_geometry2 = "82107.919 454219.98 130.672,82109.79 454219.651 130.672,82111.134 454219.407 130.672,82116.869 454218.345 130.672,82118.21 454218.086 130.672," +
                "82123.94 454217.036 130.672,82125.286 454216.795 130.672,82130.68 454215.789 130.672,82131.031 454215.731 130.672,82132.371 454215.483 130.672,82134.585 454215.166 130.672," +
                "82135.922 454214.916 130.672,82136.574 454214.882 130.672,82137.222 454214.961 130.672,82137.847 454215.151 130.672,82138.429 454215.447 130.672,82138.971 454215.848 130.672," +
                "82139.43 454216.373 130.672,82139.79 454216.971 130.672,82140.038 454217.623 130.672,82140.168 454218.309 130.672,82140.175 454219.006 130.672,82140.059 454219.694 130.672," +
                "82139.823 454220.351 130.672,82139.425 454221.057 130.672,82120.41 454254.77 130.672,82120.672 454254.897 130.672,82120.313 454255.538 130.672,82119.857 454256.073 130.672," +
                "82119.321 454256.528 130.672,82118.719 454256.891 130.672,82118.066 454257.153 130.672,82117.38 454257.306 130.672,82116.679 454257.347 130.672,82115.979 454257.275 130.672," +
                "82115.312 454257.115 130.672,82114.679 454256.852 130.672,82114.095 454256.492 130.672,82113.576 454256.045 130.672,82113.133 454255.521 130.672,82112.779 454254.933 130.672," +
                "82112.523 454254.297 130.672,82111.586 454248.97 130.672,82111.79 454248.937 130.672,82111.667 454248.272 130.672,82110.597 454242.533 130.672,82110.361 454241.194 130.672," +
                "82109.301 454235.449 130.672,82109.056 454234.11 130.672,82108.99 454233.752 130.672,82107.759 454227.024 130.672,82106.765 454221.659 130.672,82106.507 454220.236 130.672," +
                "82107.919 454219.98 130.672";

        String testdata1 = "743238 2967416 0,743238 2967450 0,743265 2967450 0,743265.625 2967416 0,743238 2967416 0";
        String testpolygon = "-7 4.2 2,-7.1 4.2 3,-7.1 4.3 2,-7 4.2 2";
        String testpoint1 = "5 5 5";
        String testpoint2 = "-2 3 1";
        String testlineString = "5 5 5,10 10 10";
        GeometryFactory fac = new GeometryFactory();
        Coordinate pcor1 = new Coordinate(5,5, 5);
        Coordinate pcor2 = new Coordinate(-2,3, 1);
        Polygon testpoly = fac.createPolygon(str2coords(testpolygon).toArray(new Coordinate[0]));
        Point testp1 = fac.createPoint(pcor1);
        Point testp2 = fac.createPoint(pcor2);
        Geometry testl = fac.createLineString(str2coords(testlineString).toArray(new Coordinate[0]));
        Geometry testll = null;
        try {

            testll = fac.createLineString(str2coords(testlineString).toArray(new Coordinate[0]));

        }catch (Exception ex){
            System.out.println(ex.toString());
        }

        String testpolygon2 = "743238 2967416 0,743238 2967450 0,743265 2967450 0,743265.625 2967416 0,743238 2967416 0";
        Polygon testpoly2 = fac.createPolygon(str2coords(testpolygon2).toArray(new Coordinate[0]));
        testpoly2.setSRID(2249);

            /*
            System.out.println(Arrays.toString(GeoSpatialProcessor.IsValid(str2coords(blaze_geometry))));
            System.out.println(Arrays.toString(GeoSpatialProcessor.IsValid(str2coords(noValid_geometry))));
            System.out.println(Arrays.toString(GeoSpatialProcessor.IsValid(str2coords(valid_geometry))));
            */
            //System.out.println(Arrays.toString(GeoSpatialProcessor.IsValid(str2coords(testdata1))));

            Geometry geom = fac.createPolygon(str2coords(testdata1).toArray(new Coordinate[0]));
            System.out.println(Arrays.toString(GeoSpatialProcessor.IsValid(geom)));
            System.out.println(GeoSpatialProcessor.Force2D(geom));
            System.out.println(GeoSpatialProcessor.transform(geom, 2249, 4326));
            System.out.println(GeoSpatialProcessor.CalculateArea(testpoly2));
            //System.out.println(GeoSpatialProcessor.Force2D(str2coords(blaze_geometry)));

            //System.out.println(GeoSpatialProcessor.CalculateArea(str2coords(st_geometry1)));

            System.out.println(GeoSpatialProcessor.Union(testpoly, testpoly2));

        }


        /*Convert the input String into list of coordinates*/
        public static List<Coordinate> str2coords(String st_geometry) {
            String[] pointXYZList = null;
            List<Coordinate> coords = new LinkedList<Coordinate>();

            if (st_geometry.contains(",")) {
                System.out.println("====================== InputString is from POSTGIS");
                pointXYZList = st_geometry.split(",");

                for (int i = 0; i < pointXYZList.length; ++i) {
                    String[] pointXYZ = pointXYZList[i].split(" ");
                    coords.add(new Coordinate(Double.valueOf(pointXYZ[0]), Double.valueOf(pointXYZ[1]), Double.valueOf(pointXYZ[2])));
                }
            } else if (st_geometry.contains("#")) {
                System.out.println("====================== InputString is from Blazegraph");
                pointXYZList = st_geometry.split("#");
                if (pointXYZList.length % 3 == 0) {
                    // 3d coordinates
                    for (int i = 0; i < pointXYZList.length; i = i + 3) {
                        coords.add(new Coordinate(Double.valueOf(pointXYZList[i]), Double.valueOf(pointXYZList[i + 1]), Double.valueOf(pointXYZList[i + 2])));
                    }
                }
            } else {
                System.out.println("InputString has no valid format");
            }

            return coords;

        }

}
