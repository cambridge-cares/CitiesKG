package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DBBuildingFurnitureTest extends DBTest{

    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <buildingfurniture/> { ? ocgml:id  ?;ocgml:class  ?;ocgml:classCodespace  ?;ocgml:function  ?;" +
                "ocgml:functionCodespace  ?;ocgml:usage  ?;ocgml:usageCodespace  ?;" +
                "ocgml:roomId  ?;ocgml:lod4BrepId  ?;ocgml:lod4OtherGeom  ?;ocgml:lod4ImplicitRepId  ?;" +
                "ocgml:lod4ImplicitRefPoint  ?;ocgml:lod4ImplicitTransformation  ?;ocgml:objectClassId  ?;.}}";
        String generated;

        try {

            // Create an object
            DBBuildingFurniture dbBuildingFurniture = new DBBuildingFurniture(batchConn, config, importer);
            assertNotNull(dbBuildingFurniture.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBBuildingFurniture.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbBuildingFurniture);

            assertEquals(expected, generated);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }
}