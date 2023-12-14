package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DBOpeningToThemSurfaceTest extends DBTest{
    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql>" +
                "INSERT DATA { GRAPH <openingtothemsurface/> { ? ocgml:openingID  ?;ocgml:themSurfaceID  ?;.}}";
        String generated;

        try {

            // Create an object
            DBOpeningToThemSurface dbOpeningToThemSurface = new DBOpeningToThemSurface(batchConn, config, importer);
            assertNotNull(dbOpeningToThemSurface.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBOpeningToThemSurface.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbOpeningToThemSurface);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }

}