package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBTexImageTest {
    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <teximage/> { ? ocgml:id  ?;ocgml:texImageURI  ?;ocgml:texMimeType  ?" +
                ";ocgml:texMimeTypeCodespace  ?;.}}";
        String generated = "";

        // Set up the environment
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();  // Note: the ObjectRegistry class has a static and synchronized method

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        Connection batchConn = DBObjectTestHelper.getConnection();

        DatabaseConnectionPool databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
        databaseConnectionPool.connect(config);

        // Create an object
        DBTexImage dbTexImage = new DBTexImage(batchConn, config, importer);
        assertNotNull(dbTexImage.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBTexImage.class.getDeclaredMethod("getSPARQLStatement", null);
        getsparqlMethod.setAccessible(true);
        generated = (String) getsparqlMethod.invoke(dbTexImage);

        assertEquals(expected, generated);

    }
}