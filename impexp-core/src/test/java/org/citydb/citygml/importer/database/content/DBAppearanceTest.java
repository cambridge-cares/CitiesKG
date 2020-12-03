package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBAppearanceTest {
    @Test
    void getSPARQLStatementTest() throws Exception {

        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA " +
                "{ GRAPH <appearance/> " +
                "{ ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:theme  ?;" +
                "ocgml:cityModelId  ?;ocgml:cityObjectId  ?;.}}";
        String generated = "";

        // Set up the environment
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();  // Note: the ObjectRegistry class has a static and synchronized method

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        Connection batchConn = DBObjectTestHelper.getConnection();

        DatabaseConnectionPool databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
        databaseConnectionPool.connect(config);

        // Create an object
        DBAppearance dbAppearance = new DBAppearance(batchConn, config, importer);
        assertNotNull(dbAppearance.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
        Method getsparqlMethod = DBAppearance.class.getDeclaredMethod("getSPARQLStatement", String.class);
        getsparqlMethod.setAccessible(true);

        String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
        if (gmlIdCodespace != null)
            gmlIdCodespace = "'" + gmlIdCodespace + "', ";

        generated = (String) getsparqlMethod.invoke(dbAppearance, gmlIdCodespace);

        assertEquals(expected, generated);

    }
}