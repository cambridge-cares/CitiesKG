package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBThematicSurfaceTest extends DBTest{

    @Test
    public void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <thematicsurface/> { ? ocgml:id  ?;ocgml:objectClassId  ?;" +
                "ocgml:buildingId  ?;ocgml:roomId  ?;ocgml:buildingInstallationId  ?;" +
                "ocgml:lod2MultiSurfaceId  ?;ocgml:lod3MultiSurfaceId  ?;ocgml:lod4MultiSurfaceId  ?;.}}";
        String generated;

        try {

            // Create an object
            DBThematicSurface dbThematicSurface = new DBThematicSurface(batchConn, config, importer);
            assertNotNull(dbThematicSurface.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBThematicSurface.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbThematicSurface);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }
}