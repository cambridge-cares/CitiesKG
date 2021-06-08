package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBCityObjectGenericAttribTest extends DBTest{

    @Test
    public void getSPARQLStatement1Test() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <cityobjectgenericattrib/> { ? ocgml:id  ?;ocgml:parentGenattribId  ?;" +
                "ocgml:rootGenattribId  ?;ocgml:attrName  ?;ocgml:dataType  ?;ocgml:genattribsetCodespace  ?;" +
                "ocgml:cityObjectId  ?;.}}";
        String generated;  //

        try {
            // Create an object
            DBCityObjectGenericAttrib dbCityObjectGenericAttrib = new DBCityObjectGenericAttrib(batchConn, config, importer);

            assertNotNull(dbCityObjectGenericAttrib.getClass().getDeclaredMethod("getSPARQLStatement1", StringBuilder.class));
            Method getsparqlMethod = DBCityObjectGenericAttrib.class.getDeclaredMethod("getSPARQLStatement1", StringBuilder.class);
            getsparqlMethod.setAccessible(true);

            StringBuilder stmt = new StringBuilder();
            generated = getsparqlMethod.invoke(dbCityObjectGenericAttrib, stmt).toString();

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }

    @Test
    public void getSPARQLStatement2Test() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <cityobjectgenericattrib/> { ? ocgml:id  ?;ocgml:attrName  ?;ocgml:dataType  ?;" +
                "ocgml:strVal  ?;ocgml:intVal  ?;ocgml:realVal  ?;ocgml:uriVal  ?;ocgml:dateVal  ?;ocgml:unit  ?;" +
                "ocgml:cityObjectId  ?;";
        String generated;

        try {
            // Create an object
            DBCityObjectGenericAttrib dbCityObjectGenericAttrib = new DBCityObjectGenericAttrib(batchConn, config, importer);
            assertNotNull(dbCityObjectGenericAttrib.getClass().getDeclaredMethod("getSPARQLStatement2", StringBuilder.class, CityGMLImportManager.class));
            Method getsparqlMethod = DBCityObjectGenericAttrib.class.getDeclaredMethod("getSPARQLStatement2", StringBuilder.class, CityGMLImportManager.class);
            getsparqlMethod.setAccessible(true);

            StringBuilder stmt = new StringBuilder();
            generated = getsparqlMethod.invoke(dbCityObjectGenericAttrib, stmt, importer).toString();

            assertEquals(expected, generated);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }
}