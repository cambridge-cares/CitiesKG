package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBCityObjectGenericAttribTest {

    @Test
    void getSPARQLStatement1Test() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> BASE <http://localhost/berlin/> INSERT DATA { GRAPH <cityobjectgenericattrib/> { ? ocgml:id  ?;ocgml:parentGenattribId  ?;ocgml:rootGenattribId  ?;ocgml:attrName  ?;ocgml:dataType  ?;ocgml:genattribsetCodespace  ?;ocgml:cityObjectId  ?;.}}";
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
        DBCityObjectGenericAttrib dbCityObjectGenericAttrib = new DBCityObjectGenericAttrib(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbCityObjectGenericAttrib.getClass().getDeclaredMethod("getSPARQLStatement1", StringBuilder.class));
        Method getsparqlMethod = DBCityObjectGenericAttrib.class.getDeclaredMethod("getSPARQLStatement1", StringBuilder.class);
        getsparqlMethod.setAccessible(true);

        StringBuilder stmt = new StringBuilder();
        generated = getsparqlMethod.invoke(dbCityObjectGenericAttrib, stmt).toString();
        assertEquals(expected, generated);

    }

    @Test
    void getSPARQLStatement2Test() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> BASE <http://localhost/berlin/> INSERT DATA { GRAPH <cityobjectgenericattrib/> { ? ocgml:id 1;ocgml:attrName  ?;ocgml:dataType  ?;ocgml:strVal  ?;ocgml:intVal  ?;ocgml:realVal  ?;ocgml:uriVal  ?;ocgml:dateVal  ?;ocgml:unit  ?;ocgml:cityObjectId  ?;";
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
        DBCityObjectGenericAttrib dbCityObjectGenericAttrib = new DBCityObjectGenericAttrib(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbCityObjectGenericAttrib.getClass().getDeclaredMethod("getSPARQLStatement2", StringBuilder.class, CityGMLImportManager.class ));
        Method getsparqlMethod = DBCityObjectGenericAttrib.class.getDeclaredMethod("getSPARQLStatement2", StringBuilder.class, CityGMLImportManager.class);
        getsparqlMethod.setAccessible(true);
        StringBuilder stmt = new StringBuilder();
        generated = getsparqlMethod.invoke(dbCityObjectGenericAttrib, stmt, importer).toString();
        assertEquals(expected, generated);
    }
}