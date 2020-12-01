package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBSurfaceGeometryTest {

    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> BASE <http://localhost/berlin/> INSERT DATA { GRAPH <surfacegeometry/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:parentId  ?;ocgml:rootId  ?;ocgml:isSolid  ?;ocgml:isComposite  ?;ocgml:isTriangulated  ?;ocgml:isXlink  ?;ocgml:isReverse  ?;ocgml:GeometryType  ?;ocgml:SolidType  ?;ocgml:ImplicitGeometryType  ?;ocgml:cityObjectId  ?;.}}";
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
        DBSurfaceGeometry dbSurfaceGeometry = new DBSurfaceGeometry(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbSurfaceGeometry.getClass().getDeclaredMethod("getSPARQLStatement", StringBuilder.class));
        Method getsparqlMethod = DBSurfaceGeometry.class.getDeclaredMethod("getSPARQLStatement", StringBuilder.class);
        getsparqlMethod.setAccessible(true);
        StringBuilder stmt = new StringBuilder();
        generated = getsparqlMethod.invoke(dbSurfaceGeometry, stmt).toString();
        assertEquals(expected, generated);

    }

}