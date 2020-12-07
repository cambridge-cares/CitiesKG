package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBTextureParamTest extends DBTest {

    @Test
    public void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { " +
                "GRAPH <textureparam/> { ? ocgml:surfaceGeometryId  ?;ocgml:isTextureParametrization  ?;" +
                "ocgml:worldToTexture  ?;ocgml:textureCoordinates  ?;ocgml:surfaceDataId  ?;.}}";
        String generated;

        try {

            // Create an object
            DBTextureParam dbTextureParam = new DBTextureParam(batchConn, config, importer);
            assertNotNull(dbTextureParam.getClass().getDeclaredMethod("getSPARQLStatement"));
            Method getsparqlMethod = DBTextureParam.class.getDeclaredMethod("getSPARQLStatement");
            getsparqlMethod.setAccessible(true);
            generated = (String) getsparqlMethod.invoke(dbTextureParam);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}