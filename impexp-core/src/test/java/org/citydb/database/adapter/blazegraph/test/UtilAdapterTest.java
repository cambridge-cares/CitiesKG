package org.citydb.database.adapter.blazegraph.test;

import junit.framework.TestCase;

import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.config.project.database.DatabaseSrsType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.blazegraph.UtilAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.*;

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
        assertEquals(18, utilAdapter.getClass().getDeclaredMethods().length);
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
        public void testGetSrsType () throws
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