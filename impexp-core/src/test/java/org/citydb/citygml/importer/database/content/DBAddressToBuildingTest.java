package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBAddressToBuildingTest {

    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/>" +
                "INSERT DATA { " +
                "GRAPH <addresstobuilding/> { ? ocgml:buildingId  ?;ocgml:addressId  ?;.}}";
        String generated = "";  //

        // Set up the environment
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();  // Note: the ObjectRegistry class has a static and synchronized method

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        Connection batchConn = DBObjectTestHelper.getConnection();

        DatabaseConnectionPool databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
        databaseConnectionPool.connect(config);

        // Create an object
        DBAddressToBuilding dbAddressToBuilding = new DBAddressToBuilding(batchConn, config, importer);
        assertNotNull(dbAddressToBuilding.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBAddressToBuilding.class.getDeclaredMethod("getSPARQLStatement", null);
        getsparqlMethod.setAccessible(true);
        generated = (String) getsparqlMethod.invoke(dbAddressToBuilding);

        assertEquals(expected, generated);

    }


}