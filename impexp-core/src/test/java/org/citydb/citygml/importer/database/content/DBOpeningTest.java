package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DBOpeningTest extends DBTest{

    @Test
    public void getSPARQLStatementTest() {
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <opening/> { ? ocgml:id  ?;ocgml:objectClassId  ?;" +
                "ocgml:addressId  ?;ocgml:lod3MultiSurfaceId  ?;" +
                "ocgml:lod4MultiSurfaceId  ?;ocgml:lod3ImplicitRepId  ?;ocgml:lod4ImplicitRepId  ?;ocgml:lod3ImplicitRefPoint  ?;" +
                "ocgml:lod4ImplicitRefPoint  ?;ocgml:lod3ImplicitTransformation  ?;ocgml:lod4ImplicitTransformation  ?;.}}";

        String generated;

        try {

            // Create an object
            DBOpening dbOpening = new DBOpening(batchConn, config, importer);
            assertNotNull(dbOpening.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBOpening.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbOpening);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }


}