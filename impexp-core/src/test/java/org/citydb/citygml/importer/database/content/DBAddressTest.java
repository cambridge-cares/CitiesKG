package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBAddressTest extends DBTest {

    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> "+
                "INSERT DATA { GRAPH <address/> " + "" +
                "{ ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:street  ?;ocgml:houseNumber  " +
                "?;ocgml:poBox  ?;ocgml:zipCode  ?;ocgml:city  ?;ocgml:country  ?;" +
                "ocgml:multiPoint  ?;ocgml:xalSource  ?;.}}";
        String generated;

        try {

            DBAddress dbAddress = new DBAddress(batchConn, config, importer);
            assertNotNull(dbAddress.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBAddress.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);

            generated = (String) getsparqlMethod.invoke(dbAddress);
            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }
}