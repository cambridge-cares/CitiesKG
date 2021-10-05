package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBAppearToSurfaceDataTest extends DBTest{

    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <appeartosurfacedata/> { ? ocgml:surfaceDataId  ?;ocgml:appearanceId  ?;.}}";
        String generated;

        try {

            // create an object
            DBAppearToSurfaceData dbAppearToSurfaceData = new DBAppearToSurfaceData(batchConn, config, importer);
            assertNotNull(dbAppearToSurfaceData.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBAppearToSurfaceData.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbAppearToSurfaceData);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}