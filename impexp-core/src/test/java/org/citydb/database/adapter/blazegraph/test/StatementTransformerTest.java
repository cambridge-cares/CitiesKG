package org.citydb.database.adapter.blazegraph.test;

import org.apache.jena.iri.IRI;
import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.config.project.kmlExporter.DisplayForm;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractSQLAdapter;
import org.citydb.database.adapter.blazegraph.StatementTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.plaf.nimbus.State;
import java.lang.reflect.Field;

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
        assertEquals(22, transformer.getClass().getDeclaredMethods().length);
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
        String expected = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "SELECT  ?envelope FROM <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/cityobject/> " +
                "WHERE { ?s ocgml:EnvelopeType ?envelope ; ocgml:id ? . }";
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

}
