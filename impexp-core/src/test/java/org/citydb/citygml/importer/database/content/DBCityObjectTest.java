package org.citydb.citygml.importer.database.content;

import org.citydb.citygml.importer.CityGMLImportException;
import org.citydb.config.Config;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DBCityObjectTest {

    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <cityobject/> " +
                "{ ? ocgml:id  ?;ocgml:objectClassId  ?;ocgml:gmlId  ?;ocgml:name  ?;ocgml:nameCodespace  " +
                "?;ocgml:description  ?;ocgml:EnvelopeType  ?;ocgml:creationDate  ?;ocgml:terminationDate  " +
                "?;ocgml:relativeToTerrain  ?;ocgml:relativeToWater  ?;ocgml:lastModificationDate  " +
                "?;ocgml:updatingPerson  ?;ocgml:reasonForUpdate  ?;ocgml:lineage  ?;.}}";
        String generated = "";  //
        // @todo: implement assigning value generated from DBCityObject class, call the class from outside and get the generated string

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        Connection batchConn = DBObjectTestHelper.getConnection();

        DBCityObject dbCityObject = new DBCityObject(batchConn, config, importer); // construct cityobject -> output string
        assertNotNull(dbCityObject.getClass().getDeclaredMethod("getSPARQLStatement", null));
        Method getsparqlMethod = DBCityObject.class.getDeclaredMethod("getSPARQLStatement", null);

        generated = (String) getsparqlMethod.invoke(dbCityObject);
        assertEquals(expected, generated);

    }

}