package org.citydb.database.adapter.blazegraph.test;

import org.citydb.citygml.importer.database.content.CityGMLImportManager;
import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;

public class SchemaManagerAdapterTest {

    @Test
    public void existsSchemaTest()  {
        Connection con = DBObjectTestHelper.getConnection();
        String validSchema = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl";
        String invalidSchema = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/owl.owl";
        try {
            CityGMLImportManager cityGMLImportManager = DBObjectTestHelper.getCityGMLImportManager();
            assertTrue(cityGMLImportManager.getDatabaseAdapter().getSchemaManager().existsSchema(con, validSchema));
            assertFalse(cityGMLImportManager.getDatabaseAdapter().getSchemaManager().existsSchema(con, invalidSchema));
        } catch (Exception e) {
            fail();
        }
    }
}
