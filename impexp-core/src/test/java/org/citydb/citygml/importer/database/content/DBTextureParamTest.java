package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBTextureParamTest {

    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <textureparam/> { ? ocgml:surfaceGeometryId  ?;ocgml:isTextureParametrization  ?;" +
                "ocgml:worldToTexture  ?;ocgml:textureCoordinates  ?;ocgml:surfaceDataId  ?;.}}";
        String generated = "";

        // Set up the environment
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();  // Note: the ObjectRegistry class has a static and synchronized method

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        Connection batchConn = DBObjectTestHelper.getConnection();

        DatabaseConnectionPool databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
        databaseConnectionPool.connect(config);

        // Create an object
        DBTextureParam dbTextureParam = new DBTextureParam(batchConn, config, importer);
        assertNotNull(dbTextureParam.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBTextureParam.class.getDeclaredMethod("getSPARQLStatement", null);
        getsparqlMethod.setAccessible(true);
        generated = (String) getsparqlMethod.invoke(dbTextureParam);

        assertEquals(expected, generated);

    }

}