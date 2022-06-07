package org.citydb.database.adapter.blazegraph.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.BlazegraphGeoDatatype;
import org.junit.jupiter.api.Test;

public class BlazegraphGeoDatatypeTest {
  private final String TEST_URI = "http://localhost/blazegraph/literals/POINT-1-3";
  private final String TEST_GEOLITERAL = "1#2#3";
  private final String TEST_GEODATATYPE = "{\"config\":{\"fields\":["
          + "{\"serviceMapping\":\"X0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"},"
          + "{\"serviceMapping\":\"Y0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"},"
          + "{\"serviceMapping\":\"Z0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"}],"
          + "\"uri\":\"" + TEST_URI + "\"}}";

  @Test
  public void testNewBlazegraphGeoDatatype() {
    BlazegraphGeoDatatype dt;
    try {
      URI datatypeURI = new URI(TEST_URI);
      RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
      Node dbObject = NodeFactory.createLiteral(TEST_GEOLITERAL, geoDatatype);
      dt = new BlazegraphGeoDatatype(dbObject);
      assertNotNull(dt);
    } catch (Exception e){
      fail();
    }
  }

  @Test
  public void testNewBlazegraphGeoDatatypeFields() {
    try {
      URI datatypeURI = new URI(TEST_URI);
      RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
      Node dbObject = NodeFactory.createLiteral(TEST_GEOLITERAL, geoDatatype);
      BlazegraphGeoDatatype dt = new BlazegraphGeoDatatype(dbObject);
      assertEquals(2, dt.getClass().getDeclaredFields().length);

      Field KEY_MAIN = dt.getClass().getDeclaredField("KEY_MAIN");
      Field geodatatype = dt.getClass().getDeclaredField("geodatatype");
      geodatatype.setAccessible(true);

      assertEquals("com.bigdata.rdf.store.AbstractTripleStore.geoSpatialDatatypeConfig.",
              KEY_MAIN.get(dt));
      assertEquals(TEST_GEODATATYPE, geodatatype.get(dt));

    } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  @Test
  public void testNewBlazegraphGeoDatatypeMethods() {
    try {
      URI datatypeURI = new URI(TEST_URI);
      RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
      Node dbObject = NodeFactory.createLiteral(TEST_GEOLITERAL, geoDatatype);
      BlazegraphGeoDatatype dt = new BlazegraphGeoDatatype(dbObject);
      assertEquals(1, dt.getClass().getDeclaredMethods().length);
    } catch (URISyntaxException e) {
      fail();
    }
  }

  @Test
  public void testGetGeodatatype() {
    try {
      URI datatypeURI = new URI(TEST_URI);
      RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
      Node dbObject = NodeFactory.createLiteral(TEST_GEOLITERAL, geoDatatype);
      BlazegraphGeoDatatype dt = new BlazegraphGeoDatatype(dbObject);
      Method getGeodatatype = dt.getClass().getDeclaredMethod("getGeodatatype");
      assertEquals(TEST_GEODATATYPE, getGeodatatype.invoke(dt));
    } catch (URISyntaxException | NoSuchMethodException | IllegalAccessException
            | InvocationTargetException e) {
      fail();
    }
  }

}
