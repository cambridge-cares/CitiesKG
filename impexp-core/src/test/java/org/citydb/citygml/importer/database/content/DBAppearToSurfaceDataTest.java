package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBAppearToSurfaceDataTest {
    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <appeartosurfacedata/> { " +
                "? ocgml:surfaceDataId  ?;ocgml:appearanceId  ?;.}}";
        String generated = "";  //
        // @todo: implement assigning value generated from DBCityObject class, call the class from outside and get the generated string

        //TODO: try to create ObjectRegistry that can be accessed by another class
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();  // Note: the ObjectRegistry class has a static and synchronized method

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        DBConnection dbConnection = DBObjectTestHelper.getDBConnection();
        config.getProject().getDatabase().setActiveConnection(dbConnection);
        Connection batchConn = DBObjectTestHelper.getConnection();

        DatabaseConnectionPool databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
        databaseConnectionPool.connect(config);

        // create an object
        DBAppearToSurfaceData dbAppearToSurfaceData = new DBAppearToSurfaceData(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbAppearToSurfaceData.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBAppearToSurfaceData.class.getDeclaredMethod("getSPARQLStatement", null);
        getsparqlMethod.setAccessible(true);
        generated = (String) getsparqlMethod.invoke(dbAppearToSurfaceData);
        assertEquals(expected, generated);

    }

}