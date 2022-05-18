package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBCityFurnitureTest extends DBTest{

    @Test
    public void getSPARQLStatementTest(){
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <cityfurniture/> { ? ocgml:id  ?;ocgml:class  ?;ocgml:classCodespace  ?;" +
                "ocgml:function  ?;ocgml:functionCodespace  ?;ocgml:usage  ?;ocgml:usageCodespace  ?;" +
                "ocgml:lod1TerrainIntersection  ?;ocgml:lod2TerrainIntersection  ?;ocgml:lod3TerrainIntersection  ?;" +
                "ocgml:lod4TerrainIntersection  ?;ocgml:lod1BrepId  ?;ocgml:lod2BrepId  ?;" +
                "ocgml:lod3BrepId  ?;ocgml:lod4BrepId  ?;ocgml:lod1OtherGeom  ?;ocgml:lod2OtherGeom  ?;" +
                "ocgml:lod3OtherGeom  ?;ocgml:lod4OtherGeom  ?;ocgml:lod1ImplicitRepId  ?;" +
                "ocgml:lod2ImplicitRepId  ?;ocgml:lod3ImplicitRepId  ?;ocgml:lod4ImplicitRepId  ?;" +
                "ocgml:lod1ImplicitRefPoint  ?;ocgml:lod2ImplicitRefPoint  ?;ocgml:lod3ImplicitRefPoint  ?;" +
                "ocgml:lod4ImplicitRefPoint  ?;ocgml:lod1ImplicitTransformation  ?;ocgml:lod2ImplicitTransformation  ?;" +
                "ocgml:lod3ImplicitTransformation  ?;ocgml:lod4ImplicitTransformation  ?;ocgml:objectClassId  ?;.}}";

        String generated;

        try {

            // Create an object
            DBCityFurniture dbCityFurniture = new DBCityFurniture(batchConn, config, importer);
            assertNotNull(dbCityFurniture.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBCityFurniture.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbCityFurniture);

            assertEquals(expected, generated);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}
