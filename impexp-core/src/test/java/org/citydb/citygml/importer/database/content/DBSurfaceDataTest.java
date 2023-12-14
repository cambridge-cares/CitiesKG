package org.citydb.citygml.importer.database.content;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DBSurfaceDataTest extends DBTest{

    @Test
    public void getSPARQLStatementTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:isFront  ?;ocgml:objectClassId  ?;";
        String generated;

        try {

            // Create an object
            DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer); // construct cityobject -> output string
            assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getSPARQLStatement", String.class));
            Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
            getsparqlMethod.setAccessible(true);

            String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
            if (gmlIdCodespace != null)
                gmlIdCodespace = "'" + gmlIdCodespace + "', ";
            generated = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }


    @Test
    public void getx3dStmtTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:isFront  ?;ocgml:objectClassId  ?;" +
                "ocgml:x3dShininess  ?;ocgml:x3dTransparency  ?;ocgml:x3dAmbientIntensity  ?;" +
                "ocgml:x3dSpecularColor  ?;ocgml:x3dDiffuseColor  ?;ocgml:x3dEmissiveColor  ?;ocgml:x3dIsSmooth  ?;.}}";
        String generated;

        try {

            // Create an object
            DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer); // construct cityobject -> output string
            assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getx3dStmt", String.class));
            Method getx3dStmtMethod = DBSurfaceData.class.getDeclaredMethod("getx3dStmt", String.class);
            getx3dStmtMethod.setAccessible(true);

            Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
            getsparqlMethod.setAccessible(true);
            String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
            if (gmlIdCodespace != null)
                gmlIdCodespace = "'" + gmlIdCodespace + "', ";
            String stmt = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);

            generated = (String) getx3dStmtMethod.invoke(dbSurfaceData, stmt);
            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }


    @Test
    public void getParaStmtTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:isFront  ?;ocgml:objectClassId  ?;" +
                "ocgml:texTextureType  ?;ocgml:texWrapMode  ?;ocgml:texBorderColor  ?;.}}";
        String generated;  //

        try {

            // Create an object
            DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer); // construct cityobject -> output string
            assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getParaStmt", String.class));
            Method getParaStmtMethod = DBSurfaceData.class.getDeclaredMethod("getParaStmt", String.class);
            getParaStmtMethod.setAccessible(true);

            Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
            getsparqlMethod.setAccessible(true);
            String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
            if (gmlIdCodespace != null)
                gmlIdCodespace = "'" + gmlIdCodespace + "', ";
            String stmt = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);

            generated = (String) getParaStmtMethod.invoke(dbSurfaceData, stmt);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

    @Test
    public void getGeoStmtTest() {
        // SYL: this is actually the preparedStatement of psCityObject
        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl> " +
                "BASE <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> " +
                "INSERT DATA { GRAPH <surfacedata/> { ? ocgml:id  ?;ocgml:gmlId  ?;ocgml:name  ?;" +
                "ocgml:nameCodespace  ?;ocgml:description  ?;ocgml:isFront  ?;ocgml:objectClassId  ?;" +
                "ocgml:texTextureType  ?;ocgml:texWrapMode  ?;ocgml:texBorderColor  ?;" +
                "ocgml:gtPreferWorldFile  ?;ocgml:gtOrientation  ?;ocgml:gtReferencePoint  ?;.}}";
        String generated;  //

        try {

            // Create an object
            DBSurfaceData dbSurfaceData = new DBSurfaceData(batchConn, config, importer);
            assertNotNull(dbSurfaceData.getClass().getDeclaredMethod("getGeoStmt", String.class));
            Method getGeoStmtMethod = DBSurfaceData.class.getDeclaredMethod("getGeoStmt", String.class);
            getGeoStmtMethod.setAccessible(true);

            Method getsparqlMethod = DBSurfaceData.class.getDeclaredMethod("getSPARQLStatement", String.class);
            getsparqlMethod.setAccessible(true);
            String gmlIdCodespace = config.getInternal().getCurrentGmlIdCodespace();
            if (gmlIdCodespace != null)
                gmlIdCodespace = "'" + gmlIdCodespace + "', ";
            String stmt = (String) getsparqlMethod.invoke(dbSurfaceData, gmlIdCodespace);

            generated = (String) getGeoStmtMethod.invoke(dbSurfaceData, stmt);

            assertEquals(expected, generated);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            objectRegistry.cleanup();
        }
    }

}