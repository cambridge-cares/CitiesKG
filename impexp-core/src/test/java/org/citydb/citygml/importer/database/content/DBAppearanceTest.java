package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBAppearanceTest extends DBTest {


    @Test
    public void getSPARQLStatementTest() throws Exception {

        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA " +
                "{ GRAPH <appearance/> " +
                "{ ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:theme  ?;" +
                "ocgml:cityModelId  ?;ocgml:cityObjectId  ?;.}}";
        String generated = "";
        try {

            // Create an object
            DBAppearance dbAppearance = new DBAppearance(batchConn, config, importer);
            assertNotNull(dbAppearance.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
            Method getsparqlMethod = DBAppearance.class.getDeclaredMethod("getSPARQLStatement", String.class);
            getsparqlMethod.setAccessible(true);

            String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
            if (gmlIdCodespace != null)
                gmlIdCodespace = "'" + gmlIdCodespace + "', ";

            generated = (String) getsparqlMethod.invoke(dbAppearance, gmlIdCodespace);

            assertEquals(expected, generated);
        } catch(Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }
}