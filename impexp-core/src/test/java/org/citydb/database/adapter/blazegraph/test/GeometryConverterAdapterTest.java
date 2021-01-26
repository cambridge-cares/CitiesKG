package org.citydb.database.adapter.blazegraph.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.HashSet;
import junit.framework.TestCase;
import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.DatabaseAdapterFactory;
import org.citydb.database.adapter.blazegraph.BlazegraphConfigBuilder;
import org.citydb.database.adapter.blazegraph.GeometryConverterAdapter;
import org.junit.jupiter.api.Test;
import org.apache.jena.graph.Node;

public class GeometryConverterAdapterTest  extends TestCase {
  private final String TEST_DB_ADAPTER_TYPE = "Blazegraph";
  private final String TEST_URI = "http://localhost/blazegraph/literals/POINT-1-3";
  private final String TEST_GEODATATYPE = "{\"config\":{\"fields\":["
      + "{\"serviceMapping\":\"X0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"},"
      + "{\"serviceMapping\":\"Y0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"},"
      + "{\"serviceMapping\":\"Z0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"}],"
      + "\"uri\":\"" + TEST_URI + "\"}}";

  @Test
  public void testNewGeometryConverterAdapter() {
    GeometryConverterAdapter converter;

    try {
      AbstractDatabaseAdapter adapter = DatabaseAdapterFactory.getInstance()
          .createDatabaseAdapter(DatabaseType.fromValue(TEST_DB_ADAPTER_TYPE));
      converter = (GeometryConverterAdapter) adapter.getGeometryConverter();
      assertNotNull(converter);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testNewGeometryConverterAdapterFields() {
    AbstractDatabaseAdapter adapter = DatabaseAdapterFactory.getInstance()
        .createDatabaseAdapter(DatabaseType.fromValue(TEST_DB_ADAPTER_TYPE));
    GeometryConverterAdapter converter = (GeometryConverterAdapter) adapter.getGeometryConverter();

    assertEquals(1, converter.getClass().getDeclaredFields().length);

    try {
      Field BASE_URL_LITERALS = converter.getClass().getDeclaredField("BASE_URL_LITERALS");
      BASE_URL_LITERALS.setAccessible(true);
      assertEquals("http://localhost/blazegraph/literals/", BASE_URL_LITERALS.get(converter));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testNewGeometryConverterAdaptermethods() {
    AbstractDatabaseAdapter adapter = DatabaseAdapterFactory.getInstance()
        .createDatabaseAdapter(DatabaseType.fromValue(TEST_DB_ADAPTER_TYPE));
    GeometryConverterAdapter converter = (GeometryConverterAdapter) adapter.getGeometryConverter();

    assertEquals(12, converter.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testGetDatabaseObject() {
    AbstractDatabaseAdapter adapter = DatabaseAdapterFactory.getInstance()
        .createDatabaseAdapter(DatabaseType.fromValue(TEST_DB_ADAPTER_TYPE));
    GeometryConverterAdapter converter = (GeometryConverterAdapter) adapter.getGeometryConverter();

    try {
      Method getDatabaseObject = converter.getClass().getDeclaredMethod("getDatabaseObject",
          GeometryObject.class, Connection.class);
      GeometryObject geo = GeometryObject.createPoint(new double[]{1.1, 2.2, 3.2}, 3, 1);
      Connection con = DBObjectTestHelper.getConnection();

      Node dbo = (Node) getDatabaseObject.invoke(converter, geo, con);

      assertEquals("1.1#2.2#3.2", dbo.getLiteral().getLexicalForm());
      assertEquals(TEST_URI, dbo.getLiteral().getDatatypeURI());
      BlazegraphConfigBuilder builder = BlazegraphConfigBuilder.getInstance();
      Field geoDataTypes = builder.getClass().getDeclaredField("geoDataTypes");
      Field uriStrings = builder.getClass().getDeclaredField("uriStrings");

      geoDataTypes.setAccessible(true);
      uriStrings.setAccessible(true);

      assertTrue(((HashSet) geoDataTypes.get(builder)).contains(TEST_GEODATATYPE));
      assertTrue(((HashSet) uriStrings.get(builder)).contains(TEST_URI));

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
        | NoSuchFieldException e) {
      fail();
    }

  }


}
