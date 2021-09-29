package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DBRoomTest extends DBTest{

    @Test
    public void testGetSPARQLStatement() {
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <room/> { ? ocgml:id  ?;ocgml:class  ?;ocgml:classCodespace  ?;ocgml:function  ?;" +
                "ocgml:functionCodespace  ?;ocgml:usage  ?;ocgml:usageCodespace  ?;ocgml:buildingId  ?;" +
                "ocgml:lod4MultiSurfaceId  ?;ocgml:lod4SolidId  ?;ocgml:objectClassId  ?;.}}";

        String generated;

        try {

            // Create an object
            DBRoom dbRoom = new DBRoom(batchConn, config, importer);
            assertNotNull(dbRoom.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBRoom.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbRoom);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}