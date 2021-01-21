package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBAddressToBuildingTest extends DBTest {

    @Test
    public void getSPARQLStatementTest() {
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/>" +
                "INSERT DATA { " +
                "GRAPH <addresstobuilding/> { ? ocgml:buildingId  ?;ocgml:addressId  ?;.}}";
        String generated;

        try {
            // Create an object
            DBAddressToBuilding dbAddressToBuilding = new DBAddressToBuilding(batchConn, config, importer);
            assertNotNull(dbAddressToBuilding.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBAddressToBuilding.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbAddressToBuilding);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }


}