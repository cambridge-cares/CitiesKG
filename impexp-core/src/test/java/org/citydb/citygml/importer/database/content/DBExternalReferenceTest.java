package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBExternalReferenceTest extends DBTest {

    @Test
    public void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA " +
                "{ GRAPH <externalreference/> " +
                "{ ? ocgml:id  ?;ocgml:infoSys  ?;" +
                "ocgml:name  ?;ocgml:URI  ?;ocgml:cityObjectId  ?;.}}";
        String generated;

        try {

            // Create an object
            DBExternalReference dbExternalReference = new DBExternalReference(batchConn, config, importer);
            assertNotNull(dbExternalReference.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBExternalReference.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbExternalReference);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}