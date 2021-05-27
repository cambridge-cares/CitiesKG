package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBSurfaceGeometryTest extends DBTest {

    @Test
    public void getSPARQLStatementTest() {

        String expected = "PREFIX ocgml: <http://locahost/ontocitygml/> " +
                "BASE <http://localhost/berlin/> " +
                "INSERT DATA { GRAPH <surfacegeometry/> " +
                "{ ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:parentId  ?;ocgml:rootId  ?;ocgml:isSolid  ?;" +
                "ocgml:isComposite  ?;ocgml:isTriangulated  ?;ocgml:isXlink  ?;ocgml:isReverse  ?;" +
                "ocgml:GeometryType  ?;ocgml:SolidType  ?;ocgml:ImplicitGeometryType  ?;ocgml:cityObjectId  ?;.}}";
        String generated;

        try {

            // create an object
            DBSurfaceGeometry dbSurfaceGeometry = new DBSurfaceGeometry(batchConn, config, importer);
            assertNotNull(dbSurfaceGeometry.getClass().getDeclaredMethod("getSPARQLStatement", StringBuilder.class));
            Method getsparqlMethod = DBSurfaceGeometry.class.getDeclaredMethod("getSPARQLStatement", StringBuilder.class);
            getsparqlMethod.setAccessible(true);
            StringBuilder stmt = new StringBuilder();
            generated = getsparqlMethod.invoke(dbSurfaceGeometry, stmt).toString();

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}