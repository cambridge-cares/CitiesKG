package org.citydb.citygml.importer.database.content;

import org.citydb.config.Config;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class DBAddressTest {

    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <address/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:street  ?;ocgml:houseNumber  ?;" +
                "ocgml:poBox  ?;ocgml:zipCode  ?;ocgml:city  ?;ocgml:country  ?;ocgml:multiPoint  ?;" +
                "ocgml:xalSource  ?;.}}";
        String generated;

        try {
            // Set up the environment
            CityGMLImportManager importer = DBObjectTestHelper.getCityGMLImportManager();
            Config config = DBObjectTestHelper.getConfig();
            Connection batchConn = DBObjectTestHelper.getConnection();

            DBAddress dbAddress = new DBAddress(batchConn, config, importer);

            assertNotNull(dbAddress.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
            Method getsparqlMethod = DBAddress.class.getDeclaredMethod("getSPARQLStatement", String.class);
            getsparqlMethod.setAccessible(true);

            // Prepare gmlIdCodespace for getSPARQLStatement method
            boolean hasGmlIdColumn = importer.getDatabaseAdapter().getConnectionMetaData().getCityDBVersion().compareTo(3, 1, 0) >= 0;
            String gmlIdCodespace = null;
            if (hasGmlIdColumn) {
                gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
                if (gmlIdCodespace != null)
                    gmlIdCodespace = "'" + gmlIdCodespace + "', ";
            }

            generated = (String) getsparqlMethod.invoke(dbAddress, gmlIdCodespace);
            assertEquals(expected, generated);
        } catch (Exception e) {
            fail();
        }
    }

}