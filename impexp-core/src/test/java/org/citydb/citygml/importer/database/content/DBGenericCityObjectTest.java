package org.citydb.citygml.importer.database.content;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBGenericCityObjectTest extends DBTest{

    @Test
    public void getSPARQLStatementTest(){
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <genericcityobject/> { ? ocgml:id  ?;" +
                "ocgml:class  ?;ocgml:classCodespace  ?;" +
                "ocgml:function  ?;ocgml:functionCodespace  ?;" +
                "ocgml:usage  ?;ocgml:usageCodespace  ?;" +
                "ocgml:lod0TerrainIntersection  ?;ocgml:lod1TerrainIntersection  ?;" +
                "ocgml:lod2TerrainIntersection  ?;ocgml:lod3TerrainIntersection  ?;" +
                "ocgml:lod4TerrainIntersection  ?;" +
                "ocgml:lod0BrepId  ?;ocgml:lod1BrepId  ?;ocgml:lod2BrepId  ?;" +
                "ocgml:lod3BrepId  ?;ocgml:lod4BrepId  ?;ocgml:lod0OtherGeom  ?;" +
                "ocgml:lod1OtherGeom  ?;ocgml:lod2OtherGeom  ?;ocgml:lod3OtherGeom  ?;" +
                "ocgml:lod4OtherGeom  ?;ocgml:lod0ImplicitRepId  ?;ocgml:lod1ImplicitRepId  ?;" +
                "ocgml:lod2ImplicitRepId  ?;ocgml:lod3ImplicitRepId  ?;ocgml:lod4ImplicitRepId  ?;" +
                "ocgml:lod0ImplicitRefPoint  ?;ocgml:lod1ImplicitRefPoint  ?;" +
                "ocgml:lod2ImplicitRefPoint  ?;ocgml:lod3ImplicitRefPoint  ?;" +
                "ocgml:lod4ImplicitRefPoint  ?;ocgml:lod0ImplicitTransformation  ?;" +
                "ocgml:lod1ImplicitTransformation  ?;ocgml:lod2ImplicitTransformation  ?;" +
                "ocgml:lod3ImplicitTransformation  ?;ocgml:lod4ImplicitTransformation  ?;" +
                "ocgml:objectClassId  ?;.}}";

        String generated;

        try {
            DBGenericCityObject dbGenericCityObject = new DBGenericCityObject(batchConn, config, importer);
            assertNotNull(dbGenericCityObject.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBGenericCityObject.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbGenericCityObject);

            assertEquals(expected, generated);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }

    }
}