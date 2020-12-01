package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBSurfaceDataTest {

    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;ocgml:nameCodespace  ?" +
                ";ocgml:description  ?;ocgml:isFront  ?;ocgml:objectClassId  ?;";
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
        DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
        Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
        getsparqlMethod.setAccessible(true);

        String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
        if (gmlIdCodespace != null)
            gmlIdCodespace = "'" + gmlIdCodespace + "', ";
        generated = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);
        assertEquals(expected, generated);

    }


    @Test
    void getx3dStmtTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:isFront  ?;" +
                "ocgml:objectClassId  ?;ocgml:x3dShininess  ?;ocgml:x3dTransparency  ?;" +
                "ocgml:x3dAmbientIntensity  ?;ocgml:x3dSpecularColor  ?;ocgml:x3dDiffuseColor  ?;" +
                "ocgml:x3dEmissiveColor  ?;ocgml:x3dIsSmooth  ?;.}}";
        String generated = "";  //

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
        DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getx3dStmt", String.class));
        Method getx3dStmtMethod = DBSurfaceData.class.getDeclaredMethod("getx3dStmt", String.class);
        getx3dStmtMethod.setAccessible(true);

        Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
        getsparqlMethod.setAccessible(true);
        String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
        if (gmlIdCodespace != null)
            gmlIdCodespace = "'" + gmlIdCodespace + "', ";
        String stmt = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);

        generated = (String) getx3dStmtMethod.invoke(dbSurfaceData, stmt);
        assertEquals(expected, generated);
    }


    @Test
    void getParaStmtTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;ocgml:nameCodespace  ?" +
                ";ocgml:description  ?;ocgml:isFront  ?;ocgml:objectClassId  ?;";
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
        DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
        Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
        getsparqlMethod.setAccessible(true);

        String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
        if (gmlIdCodespace != null)
            gmlIdCodespace = "'" + gmlIdCodespace + "', ";
        generated = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);
        assertEquals(expected, generated);

    }

}