package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;

import java.sql.Connection;

public class DBTest {
    /*This class is used for setting up the environment */
    CityGMLImportManager importer;
    Config config;
    Connection batchConn;
    DatabaseConnectionPool databaseConnectionPool;
    ObjectRegistry objectRegistry;

    public DBTest() {

        try {
            objectRegistry = DBObjectTestHelper.getObjectRegistry();        // Note: objectRegistry need to be here. Otherwise even the UNIT Test will still work while there is Exception
            importer = DBObjectTestHelper.getCityGMLImportManager();
            config = DBObjectTestHelper.getConfig();
            batchConn = DBObjectTestHelper.getConnection();
            databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
            databaseConnectionPool.connect(config);
        } catch (Exception e) {
            //System.out.print(e.getLocalizedMessage());
            //System.out.print(e.toString());
            e.printStackTrace();
        }
    }

}
