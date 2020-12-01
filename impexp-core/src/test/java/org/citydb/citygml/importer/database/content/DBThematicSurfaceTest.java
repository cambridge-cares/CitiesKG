package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBThematicSurfaceTest {

    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <thematicsurface/> { ? ocgml:id  ?;ocgml:objectClassId  ?;" +
                "ocgml:buildingId  ?;ocgml:roomId  ?;ocgml:buildingInstallationId  ?;" +
                "ocgml:lod2MultiSurfaceId  ?;ocgml:lod3MultiSurfaceId  ?;" +
                "ocgml:lod4MultiSurfaceId  ?;.}}";
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
        DBThematicSurface dbThematicSurface = new DBThematicSurface(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbThematicSurface.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBThematicSurface.class.getDeclaredMethod("getSPARQLStatement", null);
        getsparqlMethod.setAccessible(true);
        generated = (String) getsparqlMethod.invoke(dbThematicSurface);
        assertEquals(expected, generated);

    }
}