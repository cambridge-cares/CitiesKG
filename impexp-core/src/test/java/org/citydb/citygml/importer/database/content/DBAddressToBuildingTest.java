package org.citydb.citygml.importer.database.content;

import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DBAddressToBuildingTest extends DBTest {

    @Test
    void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/>" +
                "INSERT DATA { " +
                "GRAPH <addresstobuilding/> { ? ocgml:buildingId  ?;ocgml:addressId  ?;.}}";
        String generated;  //
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();

        try {
            // Create an object
            DBAddressToBuilding dbAddressToBuilding = new DBAddressToBuilding(batchConn, config, importer);
            assertNotNull(dbAddressToBuilding.getClass().getDeclaredMethod("getSPARQLStatement", null));
            Method getsparqlMethod = DBAddressToBuilding.class.getDeclaredMethod("getSPARQLStatement", null);
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbAddressToBuilding);

            assertEquals(expected, generated);
        } catch (Exception e) {
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }


}