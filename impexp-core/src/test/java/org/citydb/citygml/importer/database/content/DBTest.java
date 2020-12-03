package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.database.connection.DatabaseConnectionPool;

import java.sql.Connection;

public class DBTest {

    CityGMLImportManager importer;
    Config config;
    Connection batchConn;
    DatabaseConnectionPool databaseConnectionPool;

    public DBTest() {
        try {
            importer = DBObjectTestHelper.getCityGMLImportManager();
            config = DBObjectTestHelper.getConfig();
            batchConn = DBObjectTestHelper.getConnection();
            databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
            databaseConnectionPool.connect(config);
        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        }
    }

}
