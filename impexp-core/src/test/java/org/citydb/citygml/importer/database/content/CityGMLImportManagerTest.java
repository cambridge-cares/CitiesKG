package org.citydb.citygml.importer.database.content;

import org.citydb.citygml.importer.CityGMLImportException;
import org.hsqldb.jdbc.JDBCConnection;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;


public class CityGMLImportManagerTest {

    @Test
    public void isBlazegraphTest() {
        try {
            CityGMLImportManager cityGMLImportManager = DBObjectTestHelper.getCityGMLImportManager();
            assertTrue(cityGMLImportManager.isBlazegraph());
            cityGMLImportManager = DBObjectTestHelper.getCityGMLImportManagerPostGis();
            assertFalse(cityGMLImportManager.isBlazegraph());
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void getOntoCityGmlPrefixTest() {
        try {
            CityGMLImportManager cityGMLImportManager = DBObjectTestHelper.getCityGMLImportManager();
            assertEquals(cityGMLImportManager.getOntoCityGmlPrefix(),
                    "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void getGraphBaseIriTest() {
        try {
            CityGMLImportManager cityGMLImportManager = DBObjectTestHelper.getCityGMLImportManager();
            assertEquals(cityGMLImportManager.getGraphBaseIri(), "http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql");
        } catch (Exception e) {
            fail();
        }
    }

}
