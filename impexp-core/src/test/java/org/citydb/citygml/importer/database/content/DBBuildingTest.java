package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.registry.ObjectRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBBuildingTest {
    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <building/> { ? ocgml:id  ?;ocgml:buildingParentId  " +
                "?;ocgml:buildingRootId  ?;ocgml:class  ?;ocgml:classCodespace  " +
                "?;ocgml:function  ?;ocgml:functionCodespace  ?;ocgml:usage  " +
                "?;ocgml:usageCodespace  ?;ocgml:yearOfConstruction  ?;ocgml:yearOfDemolition  " +
                "?;ocgml:roofType  ?;ocgml:roofTypeCodespace  ?;ocgml:measuredHeigh  " +
                "?;ocgml:measuredHeightUnit  ?;ocgml:storeysAboveGround  ?;ocgml:storeysBelowGround  " +
                "?;ocgml:storeyHeightsAboveGround  ?;ocgml:storeyHeightsAgUnit  ?;ocgml:storeyHeightsBelowGround  " +
                "?;ocgml:storeyHeightsBgUnit  ?;ocgml:lod1TerrainIntersection  ?;ocgml:lod2TerrainIntersection  " +
                "?;ocgml:lod3TerrainIntersection  ?;ocgml:lod4TerrainIntersection  ?;ocgml:lod2MultiCurve  " +
                "?;ocgml:lod3MultiCurve  ?;ocgml:lod4MultiCurve  ?;ocgml:lod0FootprintId  ?;ocgml:lod0RoofprintId  " +
                "?;ocgml:lod1MultiSurfaceId  ?;ocgml:lod2MultiSurfaceId  ?;ocgml:lod3MultiSurfaceId  " +
                "?;ocgml:lod4MultiSurfaceId  ?;ocgml:lod1SolidId  ?;ocgml:lod2SolidId  ?;ocgml:lod3SolidId  " +
                "?;ocgml:lod4SolidId  ?;ocgml:objectClassId  ?;.}}";
        String generated = "";  //
        // @todo: implement assigning value generated from DBCityObject class, call the class from outside and get the generated string
        ObjectRegistry objectRegistry = DBObjectTestHelper.getObjectRegistry();

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        DBConnection dbConnection = DBObjectTestHelper.getDBConnection();
        config.getProject().getDatabase().setActiveConnection(dbConnection);
        Connection batchConn = DBObjectTestHelper.getConnection();

        DatabaseConnectionPool databaseConnectionPool = DBObjectTestHelper.getDatabaseConnectionPool();
        databaseConnectionPool.connect(config);

        DBBuilding dbBuilding = new DBBuilding(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbBuilding.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBBuilding.class.getDeclaredMethod("getSPARQLStatement", null);
        getsparqlMethod.setAccessible(true);
        generated = (String) getsparqlMethod.invoke(dbBuilding);
        assertEquals(expected, generated);
    }
}