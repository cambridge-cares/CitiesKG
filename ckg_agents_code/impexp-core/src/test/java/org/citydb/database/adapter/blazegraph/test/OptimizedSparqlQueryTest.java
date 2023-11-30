package org.citydb.database.adapter.blazegraph.test;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.blazegraph.OptimizedSparqlQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizedSparqlQueryTest {

    @Test
    public void testNewOptimizedSparqlQuery() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        assertNotNull(osq);

        // fields checked in testNewOptimizedSparqlQueryFields
    }

    @Test
    public void testNewOptimizedSparqlQueryFields() {
        try {
            AbstractDatabaseAdapter adapter = DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph");
            OptimizedSparqlQuery osq = new OptimizedSparqlQuery(adapter);
            assertEquals(10, osq.getClass().getDeclaredFields().length);

            Field QST_MARK = osq.getClass().getDeclaredField("QST_MARK");
            QST_MARK.setAccessible(true);
            assertEquals("?", QST_MARK.get(osq));
            Field IRI_GRAPH_BASE = osq.getClass().getDeclaredField("IRI_GRAPH_BASE");
            IRI_GRAPH_BASE.setAccessible(true);
            assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/", IRI_GRAPH_BASE.get(osq));
            Field PREFIX_ONTOCITYGML = osq.getClass().getDeclaredField("PREFIX_ONTOCITYGML");
            PREFIX_ONTOCITYGML.setAccessible(true);
            assertEquals("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl", PREFIX_ONTOCITYGML.get(osq));
            Field IRI_GRAPH_OBJECT_REL = osq.getClass().getDeclaredField("IRI_GRAPH_OBJECT_REL");
            IRI_GRAPH_OBJECT_REL.setAccessible(true);
            assertEquals("cityobject/", IRI_GRAPH_OBJECT_REL.get(osq));
            Field IRI_GRAPH_OBJECT = osq.getClass().getDeclaredField("IRI_GRAPH_OBJECT");
            IRI_GRAPH_OBJECT.setAccessible(true);
            assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/cityobject/", IRI_GRAPH_OBJECT.get(osq));
            Field FIXED_LOD2MISID = osq.getClass().getDeclaredField("FIXED_LOD2MSID");
            FIXED_LOD2MISID.setAccessible(true);
            assertEquals("?fixedlod2MSid", FIXED_LOD2MISID.get(osq));
            Field LODXMSID = osq.getClass().getDeclaredField("LODXMSID");
            LODXMSID.setAccessible(true);
            assertEquals("?lodXMSid", LODXMSID.get(osq));

            Field sqlStatement = osq.getClass().getDeclaredField("sqlStatement");
            assertNull(sqlStatement.get(osq));
            Field sparqlStatement = osq.getClass().getDeclaredField("sparqlStatement");
            assertNull(sparqlStatement.get(osq));
            Field databaseAdapter = osq.getClass().getDeclaredField("databaseAdapter");
            databaseAdapter.setAccessible(true);
            assertEquals(adapter, databaseAdapter.get(osq));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewOptimizedSparqlQueryMethods() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        assertEquals(6, osq.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testGetBuildingPartQuery_part1() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl>\n" +
                "SELECT ?lodXMSid\n" +
                " WHERE {\nGRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/thematicsurface/> {\n" +
                "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ;\n" +
                "ocgml:lod2MultiSurfaceId ?lodXMSid . } }";
        assertEquals(expected, OptimizedSparqlQuery.getBuildingPartQuery_part1(2));
    }

    @Test
    public void testGetBuildingPartQuery_part2() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl>\n" +
                "SELECT ?geomtype (datatype(?geomtype) AS ?datatype)\n" +
                "WHERE {\n" +
                "GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/> {\n" +
                "?sg_id ocgml:rootId ? ; \n" +
                "ocgml:GeometryType ?geomtype . \n" +
                "FILTER(!isBlank(?geomtype))} }";
        assertEquals(expected, OptimizedSparqlQuery.getBuildingPartQuery_part2());
    }

    @Test
    public void testGetSparqlBuildingPart() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        Connection conn = Mockito.mock(Connection.class);
        ResultSet rs1 = Mockito.mock(ResultSet.class);
        try {
            Mockito.when(conn.prepareStatement(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(ps);
            Mockito.when(ps.executeQuery()).thenReturn(rs1);
            Mockito.when(rs1.next()).thenReturn(true).thenReturn(true).thenReturn(false);
            Mockito.when(rs1.getString(ArgumentMatchers.anyInt())).thenReturn("http://127.0.0.1:9999/123a");
            assertEquals(rs1, OptimizedSparqlQuery.getSPARQLBuildingPart(conn, "query", 2, "http://127.0.0.1:9999/building").get(0));
        } catch (SQLException e) {
            fail();
        }
    }

    @Test
    public void testGetSparqlAggregateGeometriesForLOD2OrHigher() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        Connection conn = Mockito.mock(Connection.class);
        ResultSet rs1 = Mockito.mock(ResultSet.class);
        ResultSet rs2 = Mockito.mock(ResultSet.class);
        ResultSet rs3 = Mockito.mock(ResultSet.class);
        ResultSet finalRs = Mockito.mock(ResultSet.class);
        String subquery1_1 = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT (?lodId AS ?rootId) " +
                "WHERE { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/building/> {" +
                " ?id ocgml:id ? ;  ocgml:lod2MultiSurfaceId ?lodId.\n" +
                "FILTER (!isBlank(?lodId)) }}";
        String subquery1_2 = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT (?lodId AS ?rootId) " +
                "\nWHERE\n { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/building/> { \n" +
                " ?id ocgml:buildingId ? ;  \n ocgml:lod2SolidId ?lodId\n" +
                "FILTER (!isBlank(?lodId)) }}";
        String subquery1_3 = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT (?lodId AS ?rootId) " +
                "\nWHERE\n { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/thematicsurface/> { \n" +
                " ?id ocgml:buildingId ? ;  \n ocgml:lod2MultiSurfaceId ?lodId; \n" +
                "FILTER (!isBlank(?lodId)) }}";
        String query = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT ?geometry (datatype(?geometry) AS ?datatype)" +
                "\nWHERE\n { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/> { \n" +
                " ?id ocgml:rootId ? ;  \n ocgml:GeometryType    ?geometry\n" +
                "FILTER (!isBlank(?geometry)) }}";

        try (MockedStatic<OptimizedSparqlQuery> mock = Mockito.mockStatic(OptimizedSparqlQuery.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(() -> OptimizedSparqlQuery.executeQuery(ArgumentMatchers.any(), ArgumentMatchers.contains(subquery1_1), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(rs1);
            mock.when(() -> OptimizedSparqlQuery.executeQuery(ArgumentMatchers.any(), ArgumentMatchers.contains(subquery1_2), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(rs2);
            mock.when(() -> OptimizedSparqlQuery.executeQuery(ArgumentMatchers.any(), ArgumentMatchers.contains(subquery1_3), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(rs3);
            mock.when(() -> OptimizedSparqlQuery.executeQuery(ArgumentMatchers.any(), ArgumentMatchers.contains(query), ArgumentMatchers.contains("rootidvalue"), ArgumentMatchers.anyString()))
                    .thenReturn(finalRs);
            Mockito.when(rs1.next()).thenReturn(true).thenReturn(true).thenReturn(false);
            Mockito.when(rs2.next()).thenReturn(false);
            Mockito.when(rs3.next()).thenReturn(false);
            Mockito.when(rs1.getString(ArgumentMatchers.anyInt())).thenReturn("rootidvalue");
            Mockito.when(finalRs.next()).thenReturn(true);
            assertEquals(finalRs, OptimizedSparqlQuery.getSPARQLAggregateGeometriesForLOD2OrHigher(conn, 2, "http://127.0.0.1:9999/test").get(0));
        } catch (SQLException e) {
            fail();
        }
    }

    @Test
    public void testGetSPARQLAggregateGeometriesForCityFurniture() {
        OptimizedSparqlQuery osq = new OptimizedSparqlQuery(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        Connection conn = Mockito.mock(Connection.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        SelectBuilder query = new SelectBuilder();
        try {
            query.addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl")
                    .setBase("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry")
                    .addVar("?geometry (datatype(?geometry) AS ?datatype)")
                    .addWhere("?s", "ocgml:GeometryType", "?geometry")
                    .addWhere("?s", "ocgml:cityObjectId", "?")
                    .addFilter("!isBlank(?geometry)");
        } catch (ParseException e) {
            fail();
        }

        try (MockedStatic<OptimizedSparqlQuery> mock = Mockito.mockStatic(OptimizedSparqlQuery.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(() -> OptimizedSparqlQuery.executeQuery(ArgumentMatchers.any(), ArgumentMatchers.contains(query.toString()), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(rs);
            Mockito.when(rs.next()).thenReturn(true);
            assertEquals(rs, OptimizedSparqlQuery.getSPARQLAggregateGeometriesForCityFurniture(conn, "http://127.0.0.1:9999/test").get(0));
        } catch (SQLException | ParseException e) {
            fail();
        }
    }

    @Test
    public void testExecuteQuery() {
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        Connection conn = Mockito.mock(Connection.class);
        ResultSet rs = Mockito.mock(ResultSet.class);
        try {
            Mockito.when(conn.prepareStatement(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(ps);
            Mockito.when(ps.executeQuery()).thenReturn(rs);
            Mockito.when(rs.next()).thenReturn(true).thenReturn(false);
            assertEquals(rs, OptimizedSparqlQuery.executeQuery(conn, "test", "test", "token"));
        } catch (SQLException e) {
            fail();
        }
    }

}
