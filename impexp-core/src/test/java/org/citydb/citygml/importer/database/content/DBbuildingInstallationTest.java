package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DBbuildingInstallationTest extends DBTest{
    @Test
    public void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <buildinginstallation/> { ? ocgml:id  ?;ocgml:objectClassId  ?;" +
                "ocgml:class  ?;ocgml:classCodespace  ?;ocgml:function  ?;ocgml:functionCodespace  ?;" +
                "ocgml:usage  ?;ocgml:usageCodespace  ?;ocgml:buildingId  ?;ocgml:roomId  ?;" +
                "ocgml:lod2BrepId  ?;ocgml:lod3BrepId  ?;ocgml:lod4BrepId  ?;ocgml:lod2OtherGeom  ?;" +
                "ocgml:lod3OtherGeom  ?;ocgml:lod4OtherGeom  ?;ocgml:lod2ImplicitRepId  ?;" +
                "ocgml:lod3ImplicitRepId  ?;ocgml:lod4ImplicitRepId  ?;ocgml:lod2ImplicitRefPoint  ?;" +
                "ocgml:lod3ImplicitRefPoint  ?;ocgml:lod4ImplicitRefPoint  ?;ocgml:lod2ImplicitTransformation  ?;" +
                "ocgml:lod3ImplicitTransformation  ?;ocgml:lod4ImplicitTransformation  ?;.}}";

        String generated;

        try {

            // Create an object
            DBBuildingInstallation dbBuildingInstallation = new DBBuildingInstallation(batchConn, config, importer);
            assertNotNull(dbBuildingInstallation.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBBuildingInstallation.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbBuildingInstallation);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }
}