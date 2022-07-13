package org.citydb.database.adapter.blazegraph.test;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.config.project.kmlExporter.DisplayForm;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractSQLAdapter;
import org.citydb.database.adapter.blazegraph.StatementTransformer;
import org.citydb.sqlbuilder.expression.PlaceHolder;
import org.citydb.sqlbuilder.schema.Column;
import org.citydb.sqlbuilder.schema.Table;
import org.citydb.sqlbuilder.select.ProjectionToken;
import org.citydb.sqlbuilder.select.Select;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class StatementTransformerTest {

    @Test
    public void testNewStatementTransformer() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        assertNotNull(transformer);

        // fields checked in testNewStatementTransformerFields
    }

    @Test
    public void testNewStatementTransformerFields() {
        try {
            AbstractDatabaseAdapter adapter = DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph");
            StatementTransformer transformer = new StatementTransformer(adapter);
            assertEquals(8, transformer.getClass().getDeclaredFields().length);

            Field QST_MARK = transformer.getClass().getDeclaredField("QST_MARK");
            QST_MARK.setAccessible(true);
            assertEquals("?", QST_MARK.get(transformer));
            Field IRI_GRAPH_BASE = transformer.getClass().getDeclaredField("IRI_GRAPH_BASE");
            IRI_GRAPH_BASE.setAccessible(true);
            assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/", IRI_GRAPH_BASE.get(transformer));
            Field PREFIX_ONTOCITYGML = transformer.getClass().getDeclaredField("PREFIX_ONTOCITYGML");
            PREFIX_ONTOCITYGML.setAccessible(true);
            assertEquals("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl", PREFIX_ONTOCITYGML.get(transformer));
            Field IRI_GRAPH_OBJECT_REL = transformer.getClass().getDeclaredField("IRI_GRAPH_OBJECT_REL");
            IRI_GRAPH_OBJECT_REL.setAccessible(true);
            assertEquals("cityobject/", IRI_GRAPH_OBJECT_REL.get(transformer));
            Field IRI_GRAPH_OBJECT = transformer.getClass().getDeclaredField("IRI_GRAPH_OBJECT");
            IRI_GRAPH_OBJECT.setAccessible(true);
            assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/cityobject/", IRI_GRAPH_OBJECT.get(transformer));

            Field sqlStatement = transformer.getClass().getDeclaredField("sqlStatement");
            assertNull(sqlStatement.get(transformer));
            Field sparqlStatement = transformer.getClass().getDeclaredField("sparqlStatement");
            assertNull(sparqlStatement.get(transformer));
            Field databaseAdapter = transformer.getClass().getDeclaredField("databaseAdapter");
            databaseAdapter.setAccessible(true);
            assertEquals(adapter, databaseAdapter.get(transformer));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewStatementTransformerMethods() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        assertEquals(19, transformer.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testGetGenericCityObjectBasisData() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT ?id ?lod1ImplicitRefPoint ?lod1ImplicitTransformation ?cityObjectId \n" +
                "WHERE { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/genericcityobject/> \n" +
                "{ ?id ocgml:lod1ImplicitRefPoint ?lod1ImplicitRefPoint;\n" +
                "ocgml:lod1ImplicitTransformation ?lod1ImplicitTransformation;\n" +
                "ocgml:id ? . } \n" +
                "GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/> \n" +
                " { ?cityObjectId ocgml:cityObjectId ? . } }";
        assertEquals(expected, StatementTransformer.getGenericCityObjectBasisData(1));
    }

    @Test
    public void testGetGenericCityObjectQuery() {
        AbstractDatabaseAdapter spy = Mockito.spy(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        StatementTransformer transformer = new StatementTransformer(spy);

        // test case when displayForm is FOOTPRINT or EXTRUDED
        String expectedFootprintExtruded = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT ?ImplicitGeometryType ?id ?parentId (? AS ?rootId) ?gmlId ?isXlink (datatype(?GeometryType) AS ?datatype)\n" +
                "FROM <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/>" +
                "WHERE {?id ocgml:GeometryType ?GeometryType; ocgml:parentId ?parentId; ocgml:gmlId ?gmlId; ocgml:isXlink ?isXlink; ocgml:rootId ? . } \n" +
                "ORDER BY ?id";
        DisplayForm footprint = new DisplayForm();
        footprint.setForm(1);
        assertEquals(expectedFootprintExtruded, StatementTransformer.getGenericCityObjectQuery(1, footprint, true, true));
        DisplayForm extruded = new DisplayForm();
        extruded.setForm(2);
        assertEquals(expectedFootprintExtruded, StatementTransformer.getGenericCityObjectQuery(1, extruded, true, true));

        // test case when default
        String expectedDefault = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT ?, '5' as ?objectclass_id ";
        DisplayForm def = new DisplayForm();
        def.setForm(4);
        // test case when requiresPseudoTableInSelect returns false
        assertEquals(expectedDefault, StatementTransformer.getGenericCityObjectQuery(1, def, true, true));

        // test case when requiresPseudoTableInSelect returns true
        AbstractSQLAdapter sqlAdapter = Mockito.spy(spy.getSQLAdapter());
        Mockito.doReturn(sqlAdapter).when(spy).getSQLAdapter();
        Mockito.doReturn(true).when(sqlAdapter).requiresPseudoTableInSelect();
        Mockito.doReturn("table").when(sqlAdapter).getPseudoTableName();
        String expectedDefaultTable = expectedDefault + " FROM table";
        assertEquals(expectedDefaultTable, StatementTransformer.getGenericCityObjectQuery(1, def, true, true));
    }

    @Test
    public void testGetSurfaceGeometries() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));

        // test case when isImplicit is true
        String expectedTrue = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT ?ImplicitGeometryType ?id ?parentId (? AS ?rootId) ?gmlId ?isXlink (datatype(?GeometryType) AS ?datatype)\n" +
                "FROM <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/>" +
                "WHERE {?id ocgml:GeometryType ?GeometryType; ocgml:parentId ?parentId; ocgml:gmlId ?gmlId; ocgml:isXlink ?isXlink; ocgml:rootId ? . } \n" +
                "ORDER BY ?id";
        assertEquals(expectedTrue, StatementTransformer.getSurfaceGeometries(true, true));

        // test case when isImplicit is false
        String expectedFalse = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT ?GeometryType ?id ?parentId (? AS ?rootId) ?gmlId ?isXlink (datatype(?GeometryType) AS ?datatype)\n" +
                "FROM <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/>" +
                "WHERE {?id ocgml:GeometryType ?GeometryType; ocgml:parentId ?parentId; ocgml:gmlId ?gmlId; ocgml:isXlink ?isXlink; ocgml:rootId ? . } \n" +
                "ORDER BY ?id";
        assertEquals(expectedFalse, StatementTransformer.getSurfaceGeometries(true, false));
    }

    @Test
    public void testGetIriObjectBase() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/", StatementTransformer.getIriObjectBase());
    }

    @Test
    public void testGetExtrusionHeight() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                "SELECT  ?envelope \n" +
                "FROM <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/cityobject/> \n" +
                "WHERE { ?s ocgml:EnvelopeType ?envelope ; ocgml:id ? . } \n";
        assertEquals(expected, StatementTransformer.getExtrusionHeight());
    }

    @Test
    public void testGetSPARQLStatement_BuildingParts() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT * FROM  <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/building/> " +
                "WHERE { ?id ocgml:buildingRootId ? .}";
        assertEquals(expected, StatementTransformer.getSPARQLStatement_BuildingParts("test"));
    }

    @Test
    public void testGetSPARQLStatement_BuildingPartQuery_part1() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));

        // test case when IRI_GRAPH_BASE does not contain "theworldavatar"
        String expected1 = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT ?geomtype (datatype(?geomtype) AS ?type)" +
                "WHERE {GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/thematicsurface/> {" +
                "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ;ocgml:lod2MultiSurfaceId ?lod2MSid .}" +
                "GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/> {" +
                "?sg_id ocgml:rootId ?lod2MSid; ocgml:GeometryType ?geomtype . FILTER(!isBlank(?geomtype))} }";
        assertEquals(expected1, StatementTransformer.getSPARQLStatement_BuildingPartQuery_part1("test"));

        // test case when IRI_GRAPH_BASE contains "theworldavatar"
        try {
            Field IRI_GRAPH_BASE = transformer.getClass().getDeclaredField("IRI_GRAPH_BASE");
            IRI_GRAPH_BASE.setAccessible(true);
            IRI_GRAPH_BASE.set(transformer, "http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/");

            String expected2 = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                    "SELECT ?fixedlod2MSid " +
                    "WHERE {GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/thematicsurface/> {" +
                    "?ts_id ocgml:objectClassId 35 ; ocgml:buildingId ? ;ocgml:lod2MultiSurfaceId ?lod2MSid .}" +
                    "BIND(IRI(CONCAT(STR(?lod2MSid), '/')) AS ?fixedlod2MSid) }";

            assertEquals(expected2, StatementTransformer.getSPARQLStatement_BuildingPartQuery_part1("test"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testGetSPARQLStatement_BuildingPartQuery_part2() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT ?geomtype (datatype(?geomtype) AS ?type)" +
                "WHERE {GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/> {" +
                "?sg_id ocgml:rootId ?; ocgml:GeometryType ?geomtype . FILTER(!isBlank(?geomtype))} }";
        assertEquals(expected, StatementTransformer.getSPARQLStatement_BuildingPartQuery_part2());
    }

    @Test
    public void testGetTopFeatureId() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));

        // test case when * and SingaporeEPSG4326 namespace
        try {
            Field IRI_GRAPH_BASE = transformer.getClass().getDeclaredField("IRI_GRAPH_BASE");
            IRI_GRAPH_BASE.setAccessible(true);
            IRI_GRAPH_BASE.set(transformer, "http://127.0.0.1:9999/blazegraph/namespace/singaporeEPSG4326/sparql/");

            Select select = Mockito.mock(Select.class, Mockito.RETURNS_MOCKS);
            List<PlaceHolder> list = new ArrayList<>(Collections.singleton(new PlaceHolder("*")));

            Mockito.doReturn(list).when(select).getInvolvedPlaceHolders();

            String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                    "SELECT ?id ?objectclass_id ?gmlid\n" +
                    "\nWHERE { \n" +
                    "GRAPH <http://127.0.0.1:9999/blazegraph/namespace/singaporeEPSG4326/sparql/cityobject/> \n" +
                    "{ ?id ocgml:id ?Id ; ocgml:gmlId ?gmlid ; ocgml:objectClassId  ?objectclass_id . \n" +
                    "FILTER ( ?objectclass_id IN (64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26) )} \n" +
                    "GRAPH <http://127.0.0.1:9999/blazegraph/namespace/singaporeEPSG4326/sparql/cityobjectgenericattrib/> \n" +
                    "{ ?ObjectIdAttr ocgml:cityObjectId ?Id ; ocgml:attrName 'LU_DESC' . } }";
            assertEquals(expected, StatementTransformer.getTopFeatureId(select));
        } catch (NoSuchFieldException | IllegalAccessException | ParseException e) {
            fail();
        }

        // test case where placeHolders.size() == 1 and *
        try {
            Field IRI_GRAPH_BASE = transformer.getClass().getDeclaredField("IRI_GRAPH_BASE");
            IRI_GRAPH_BASE.setAccessible(true);
            IRI_GRAPH_BASE.set(transformer, "http://127.0.0.1:9999/blazegraph/namespace/test/sparql/");

            Select select = Mockito.mock(Select.class, Mockito.RETURNS_MOCKS);
            List<PlaceHolder> list = new ArrayList<>(Collections.singleton(new PlaceHolder("*")));

            Mockito.doReturn(list).when(select).getInvolvedPlaceHolders();

            String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                    "SELECT ?id ?objectclass_id ?gmlid\n" +
                    "FROM <http://127.0.0.1:9999/blazegraph/namespace/test/sparql/cityobject/> \n" +
                    "\nWHERE\n " +
                    "{ ?id ocgml:objectClassId  ?objectclass_id ; \n ocgml:gmlId ?gmlid . \n" +
                    "FILTER ( ?objectclass_id IN (64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26) )}";
            assertEquals(expected, StatementTransformer.getTopFeatureId(select));
        } catch (NoSuchFieldException | IllegalAccessException | ParseException e) {
            fail();
        }

        // test case for single and multiple objects
        try {
            Select select = Mockito.mock(Select.class, Mockito.RETURNS_MOCKS);
            List<PlaceHolder> list = new ArrayList<>(Collections.singleton(new PlaceHolder("BLDG_123a")));

            Mockito.doReturn(list).when(select).getInvolvedPlaceHolders();

            String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> \n" +
                    "SELECT ?id ?objectclass_id (? AS ?gmlid) \n" +
                    "FROM <http://127.0.0.1:9999/blazegraph/namespace/test/sparql/cityobject/> \n" +
                    "\nWHERE\n " +
                    "{ ?id ocgml:objectClassId  ?objectclass_id ;\n ocgml:gmlId ?\n" +
                    "FILTER ( ?objectclass_id IN (64, 4, 5, 7, 8, 9, 42, 43, 44, 45, 14, 46, 85, 21, 23, 26) )\n }";
            assertEquals(expected, StatementTransformer.getTopFeatureId(select));
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void getSPARQLStatement_BuildingPartGeometry() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT distinct ?surf ?geomtype (datatype(?geomtype) as ?datatype) ?surftype " +
                "WHERE { ?surf ocgml:cityObjectId ? ;" +
                "ocgml:GeometryType ?geomtype ." +
                "FILTER (!isBlank(?geomtype)) }";
        assertEquals(expected, StatementTransformer.getSPARQLStatement_BuildingPartGeometry());
    }

    @Test
    public void testGetSPARQLStatement_BuildingPartGeometry_part2() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT distinct ?surf ?geomtype ?surftype (datatype(?geomtype) as ?datatype) " +
                "WHERE { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/thematicsurface/> " +
                "{?themsurf ocgml:buildingId ? ; " +
                "ocgml:objectClassId ?surftype.}" +
                "GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/> " +
                "{?surf ocgml:cityObjectId ?themsurf; " +
                "ocgml:GeometryType ?geomtype . " +
                "FILTER (!isBlank(?geomtype)) }}";
        assertEquals(expected, StatementTransformer.getSPARQLStatement_BuildingPartGeometry_part2());
    }

    @Test
    public void testGetSPARQLStatement_SurfaceGeometry() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT ?geom (DATATYPE(?geom) as ?datatype)" +
                "WHERE { ? ocgml:GeometryType ?geom ." +
                "FILTER (!isBlank(?geom)) }";
        assertEquals(expected, StatementTransformer.getSPARQLStatement_SurfaceGeometry());
    }

    @Test
    public void testApplyPredicate() {
        // this test is deliberately left blank
    }

    @Test
    public void testGetGraphUri() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        Set<Table> involvedTables = new HashSet<>();
        involvedTables.add(new Table("cityobject"));
        assertEquals("http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/cityobject/", StatementTransformer.getGraphUri(involvedTables));
    }

    @Test
    public void testGetVarNames() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        List<ProjectionToken> list = new ArrayList<>();
        Table table = new Table("table");
        list.add(new Column(table, "var1", "name"));
        list.add(new Column(table, "var2", "name"));
        list.add(new Column(table, "var3", "name"));

        assertEquals("?var1 ?var2 ?var3", StatementTransformer.getVarNames(list));
    }

    @Test
    public void testGetSPARQQLAggregateGeometriesForLOD2OrHigher() {
        // this test is deliberately left blank
    }

    @Test
    public void testExecuteQuery() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        Connection conn = Mockito.mock(Connection.class);
        ResultSet rs = Mockito.mock(ResultSet.class);
        try {
            Mockito.when(conn.prepareStatement(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(ps);
            Mockito.when(ps.executeQuery()).thenReturn(rs);
            Mockito.when(rs.next()).thenReturn(true).thenReturn(false);
            Mockito.when(rs.getString(ArgumentMatchers.anyString())).thenReturn("result");

            assertEquals("result", StatementTransformer.executeQuery(conn, "test", "test").get(0));
        } catch (SQLException e) {
            fail();
        }
    }

    @Test
    public void testGetSPARQLqueryStage2() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        String expected = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl>\n" +
                "\n" +
                "SELECT  ?geometry\n" +
                "FROM <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/surfacegeometry/>\n" +
                "WHERE\n" +
                "  { ?id  ocgml:rootId        ?rootId ;\n" +
                "         ocgml:GeometryType  ?geometry\n" +
                "    FILTER ( ! isBlank(?geometry) )\n" +
                "      { SELECT  (?lod2MultiSurfaceId AS ?rootId)\n" +
                "        WHERE\n" +
                "          { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/building/>\n" +
                "              { ?id  ocgml:buildingId      ? ;\n" +
                "                     ocgml:lod2MultiSurfaceId  ?lod2MultiSurfaceId\n" +
                "                FILTER ( ! isBlank(?lod2MultiSurfaceId) )\n" +
                "              }}\n" +
                "      }\n" +
                "    UNION\n" +
                "      { SELECT  (?lod2SolidId AS ?rootId)\n" +
                "        WHERE\n" +
                "          { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/building/>\n" +
                "              { ?id  ocgml:buildingId   ? ;\n" +
                "                     ocgml:lod2SolidId  ?lod2SolidId\n" +
                "                FILTER ( ! isBlank(?lod2SolidId) )\n" +
                "              }}\n" +
                "      }\n" +
                "    UNION\n" +
                "      { SELECT  (?lod2MultiSurfaceId AS ?rootId)\n" +
                "        WHERE\n" +
                "          { GRAPH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/thematicsurface/>\n" +
                "              { ?id  ocgml:buildingId      ? ;\n" +
                "                     ocgml:lod2MultiSurfaceId  ?lod2MultiSurfaceId\n" +
                "                FILTER ( ! isBlank(?lod2MultiSurfaceId) )\n" +
                "              }}\n" +
                "      }\n" +
                "  }\n";

        try {
            assertEquals(expected, StatementTransformer.getSPARQLqueryStage2("test", "2"));
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testFilterResult() {
        StatementTransformer transformer = new StatementTransformer(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));

        // test case when pass if condition
        List<String> extracted = new ArrayList<>(Collections.singleton("0.0#0.0#1.0#0.0#1.0#1.0#0.0#1.0#0.0#0.0"));
        assertEquals("POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))", StatementTransformer.filterResult(extracted, 0.5).toString());

        // test case when fail if condition
        assertEquals("GEOMETRYCOLLECTION EMPTY", StatementTransformer.filterResult(extracted, 2.0).toString());
    }
}
