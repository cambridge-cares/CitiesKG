package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBCityObjectTest extends DBTest{

    @Test
    public void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <cityobject/> { ? ocgml:id  ?;ocgml:objectClassId  ?;ocgml:gmlId  ?;" +
                "ocgml:name  ?;ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:EnvelopeType  ?;" +
                "ocgml:creationDate  ?;ocgml:terminationDate  ?;ocgml:relativeToTerrain  ?;" +
                "ocgml:relativeToWater  ?;ocgml:lastModificationDate  ?;ocgml:updatingPerson  ?;" +
                "ocgml:reasonForUpdate  ?;ocgml:lineage  ?;.}}";
        String generated;

        try {

            // Create an object
            DBCityObject dbCityObject = new DBCityObject(batchConn, config, importer);
            assertNotNull(dbCityObject.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBCityObject.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbCityObject);

            assertEquals(expected, generated);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}