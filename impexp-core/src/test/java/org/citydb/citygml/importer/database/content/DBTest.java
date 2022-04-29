package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractUtilAdapter;
import org.citydb.database.adapter.DatabaseAdapterFactory;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.connection.DatabaseMetaData;
import org.citydb.registry.ObjectRegistry;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.fail;

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

            // set up spies to stub getDatabaseMetadata method
            AbstractDatabaseAdapter databaseAdapter = Mockito.spy(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
            AbstractUtilAdapter utilAdapter = Mockito.spy(databaseAdapter.getUtil());
            DatabaseMetaData databaseMetaData = databaseAdapter.getConnectionMetaData();
            try (MockedStatic<DatabaseAdapterFactory> factory = Mockito.mockStatic(DatabaseAdapterFactory.class, Mockito.RETURNS_DEEP_STUBS)) {
                factory.when(() -> DatabaseAdapterFactory.getInstance().createDatabaseAdapter(ArgumentMatchers.any(DatabaseType.class)))
                        .thenReturn(databaseAdapter);
                Mockito.doReturn(utilAdapter).when(databaseAdapter).getUtil();
                Mockito.doReturn(databaseMetaData).when(utilAdapter).getDatabaseInfo(ArgumentMatchers.anyString());
                databaseConnectionPool.connect(config);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
