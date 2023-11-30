package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBTexImageTest extends DBTest {

    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <teximage/> { ? ocgml:id  ?;ocgml:texImageURI  ?;ocgml:texMimeType  ?;" +
                "ocgml:texMimeTypeCodespace  ?;.}}";
        String generated;

        try {

            // Create an object
            DBTexImage dbTexImage = new DBTexImage(batchConn, config, importer);
            assertNotNull(dbTexImage.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBTexImage.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbTexImage);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }
}