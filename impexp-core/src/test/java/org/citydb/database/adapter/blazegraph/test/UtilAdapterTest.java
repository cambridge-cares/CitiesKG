package org.citydb.database.adapter.blazegraph.test;

import junit.framework.TestCase;

import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.config.project.database.DatabaseSrsType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.blazegraph.UtilAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

import static org.mockito.Mockito.*;

public class UtilAdapterTest extends TestCase {

    @Mock
    Connection conn = Mockito.mock(Connection.class, RETURNS_MOCKS);
    Statement statement = Mockito.mock(Statement.class, RETURNS_MOCKS);
    ResultSet rs = Mockito.mock(java.sql.ResultSet.class);

    public UtilAdapter createNewUtilAdapter() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor c = UtilAdapter.class.getDeclaredConstructor(AbstractDatabaseAdapter.class);
        c.setAccessible(true);
        UtilAdapter utilAdapter = (UtilAdapter) c.newInstance(DBObjectTestHelper.createAbstractDatabaseAdapter("Blazegraph"));
        return utilAdapter;
    }

    @Test
    public void testNewUtilAdapter() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        assertNotNull(utilAdapter);
    }

    @Test
    public void testNewUtilAdapterFields() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        assertEquals(0, utilAdapter.getClass().getDeclaredFields().length);
    }

    @Test
    public void testNewUtilAdapterMethods() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        assertEquals(20, utilAdapter.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testProtectedGetSrsInfo() {
        /*
         * This method calls public void getSrsInfo(DatabaseSrs srs) which is tested below
         * Hence this test is intentionally left blank
         */
    }

    @Test //test case when srs is supported
    public void testGetSrsInfoSupported() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        DatabaseSrs srs = DatabaseSrs.createDefaultSrs();

        try (MockedStatic<DriverManager> manager = Mockito.mockStatic(DriverManager.class, RETURNS_MOCKS)) {
            manager.when(() -> DriverManager.getConnection(ArgumentMatchers.anyString())).thenReturn(conn);
            when(conn.createStatement()).thenReturn(statement);
            when(statement.executeQuery(ArgumentMatchers.anyString())).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getString(1)).thenReturn("name");
            when(rs.getString(2)).thenReturn("type");
            when(rs.getString(3)).thenReturn("wktext");

            utilAdapter.getSrsInfo(srs);
            assertEquals("name", srs.getDatabaseSrsName());
            assertEquals(DatabaseSrsType.UNKNOWN, srs.getType());
            assertEquals("wktext", srs.getWkText());
            assertTrue(srs.isSupported());
        }
    }

    @Test //test case when srs is supported but has no well known text
    public void testGetSrsInfoNoWktext() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        DatabaseSrs srs = DatabaseSrs.createDefaultSrs();

        try (MockedStatic<DriverManager> manager = Mockito.mockStatic(DriverManager.class, RETURNS_MOCKS)) {
            manager.when(() -> DriverManager.getConnection(ArgumentMatchers.anyString())).thenReturn(conn);
            when(conn.createStatement()).thenReturn(statement);
            when(statement.executeQuery(ArgumentMatchers.anyString())).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getString(1)).thenReturn(null);
            when(rs.getString(2)).thenReturn(null);
            when(rs.getString(3)).thenReturn("some string");

            utilAdapter.getSrsInfo(srs);
            assertEquals("", srs.getDatabaseSrsName());
            assertEquals(DatabaseSrsType.UNKNOWN, srs.getType());
            assertEquals("", srs.getWkText());
            assertTrue(srs.isSupported());
        }
    }

    @Test //test case when srs is not supported
    public void testGetSrsInfoNotSupported() throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        DatabaseSrs srs = DatabaseSrs.createDefaultSrs();

        try (MockedStatic<DriverManager> manager = Mockito.mockStatic(DriverManager.class, RETURNS_MOCKS)) {
            manager.when(() -> DriverManager.getConnection(ArgumentMatchers.anyString())).thenReturn(conn);
            when(conn.createStatement()).thenReturn(statement);
            when(statement.executeQuery(ArgumentMatchers.anyString())).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            utilAdapter.getSrsInfo(srs);
            assertEquals("n/a", srs.getDatabaseSrsName());
            assertEquals(DatabaseSrsType.UNKNOWN, srs.getType());
            assertNull(srs.getWkText());
            assertFalse(srs.isSupported());
        }
    }

    @Test
    public void testPublicChangeSrs() {
        /*
         * This method calls protected void changeSrs(DatabaseSrs srs, boolean doTransform, String schema, Connection connection)
         * which is tested below
         * Hence this test is intentionally left blank
         */
    }

    @Test //test case when srs is supported
    public void testChangeSrsSupported() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, SQLException {
        UtilAdapter spy = Mockito.spy(createNewUtilAdapter());
        DatabaseSrs srs = DatabaseSrs.createDefaultSrs();
        Boolean doTransform = false;
        String schema = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";

        Mockito.doAnswer((Answer<Void>) invocation -> {
                    DatabaseSrs checkSrs = invocation.getArgument(0);
                    checkSrs.setSupported(true);
                    return null;
                }
        ).when(spy).getSrsInfo(ArgumentMatchers.any(DatabaseSrs.class));
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate(anyString())).thenReturn(0);

        Method changeSrs = UtilAdapter.class.getDeclaredMethod("changeSrs", DatabaseSrs.class, boolean.class, String.class, Connection.class);
        changeSrs.setAccessible(true);
        changeSrs.invoke(spy, srs, doTransform, schema, conn);

        verify(statement).executeUpdate(anyString());
    }

    @Test //test case when srs is not supported
    public void testChangeSrsNotSupported() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, SQLException {
        UtilAdapter spy = Mockito.spy(createNewUtilAdapter());
        DatabaseSrs srs = DatabaseSrs.createDefaultSrs();
        Boolean doTransform = false;
        String schema = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";

        Mockito.doNothing().when(spy).getSrsInfo(ArgumentMatchers.any(DatabaseSrs.class));

        Method changeSrs = UtilAdapter.class.getDeclaredMethod("changeSrs", DatabaseSrs.class, boolean.class, String.class, Connection.class);
        changeSrs.setAccessible(true);

        try {
            changeSrs.invoke(spy, srs, doTransform, schema, conn);
        } catch (InvocationTargetException e) {
            assertEquals(SQLException.class, e.getCause().getClass());
            assertEquals("Graph spatialrefsys does not contain the SRID 0. Insert commands for missing SRIDs can be found at spatialreference.org", e.getCause().getMessage());
        }
    }

    @Test
    public void testGetChangeSrsUpdateStatement() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        String schema = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
        String endpoint = "http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/";
        DatabaseSrs srs = DatabaseSrs.createDefaultSrs();
        srs.setSrid(123);
        srs.setGMLSrsName("test");

        String expected = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>\n" +
                "WITH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/databasesrs/>\n" +
                "DELETE { ?srid ocgml:srid ?currentSrid .\n" +
                "?srsname ocgml:srsname ?currentSrsname }\n" +
                "INSERT { <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/> ocgml:srid 123;\n" +
                "ocgml:srsname \"test\" }\n" +
                "WHERE { OPTIONAL { ?srid ocgml:srid ?currentSrid }\n" +
                "OPTIONAL { ?srsname ocgml:srsname ?currentSrsname } }";

        Method changeSrsUpdateStatement = UtilAdapter.class.getDeclaredMethod("getChangeSrsUpdateStatement", String.class, String.class, DatabaseSrs.class);
        changeSrsUpdateStatement.setAccessible(true);

        assertEquals(expected, changeSrsUpdateStatement.invoke(utilAdapter, schema, endpoint, srs));
    }

    @Test
    public void testGetSrsType() throws
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        UtilAdapter utilAdapter = createNewUtilAdapter();
        Method getSrsType = UtilAdapter.class.getDeclaredMethod("getSrsType", String.class);
        getSrsType.setAccessible(true);

        assertEquals(DatabaseSrsType.PROJECTED, getSrsType.invoke(utilAdapter, "PROJCS"));
        assertEquals(DatabaseSrsType.GEOGRAPHIC2D, getSrsType.invoke(utilAdapter, "GEOGCS"));
        assertEquals(DatabaseSrsType.GEOCENTRIC, getSrsType.invoke(utilAdapter, "GEOCCS"));
        assertEquals(DatabaseSrsType.VERTICAL, getSrsType.invoke(utilAdapter, "VERT_CS"));
        assertEquals(DatabaseSrsType.ENGINEERING, getSrsType.invoke(utilAdapter, "LOCAL_CS"));
        assertEquals(DatabaseSrsType.COMPOUND, getSrsType.invoke(utilAdapter, "COMPD_CS"));
        assertEquals(DatabaseSrsType.GEOGRAPHIC3D, getSrsType.invoke(utilAdapter, "GEOGCS3D"));
        assertEquals(DatabaseSrsType.UNKNOWN, getSrsType.invoke(utilAdapter, "unknown"));
    }
}