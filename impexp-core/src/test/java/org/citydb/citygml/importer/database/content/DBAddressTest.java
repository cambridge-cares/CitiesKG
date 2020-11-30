package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.citydb.database.version.DatabaseVersion;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DBAddressTest {
    @Test
    void getSPARQLStatementTest() throws Exception {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <address/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:street  ?;ocgml:houseNumber  " +
                "?;ocgml:poBox  ?;ocgml:zipCode  ?;ocgml:city  ?;ocgml:country  ?;ocgml:multiPoint  ?;ocgml:xalSource  ?;.}}";
        String generated = "";  //
        // @todo: implement assigning value generated from DBCityObject class, call the class from outside and get the generated string

        CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
        Config config = DBObjectTestHelper.getConfig();
        Connection batchConn = DBObjectTestHelper.getConnection();

        DBAddress dbAddress = new DBAddress(batchConn, config, importer); // construct cityobject -> output string

        assertNotNull(dbAddress.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
        Method getsparqlMethod = DBAddress.class.getDeclaredMethod("getSPARQLStatement", String.class);
        getsparqlMethod.setAccessible(true);

        boolean hasGmlIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(3, 1, 0) >= 0;
        String gmlIdCodespace = null;

        generated = (String) getsparqlMethod.invoke(dbAddress, gmlIdCodespace);
        assertEquals(expected, generated);
    }
}