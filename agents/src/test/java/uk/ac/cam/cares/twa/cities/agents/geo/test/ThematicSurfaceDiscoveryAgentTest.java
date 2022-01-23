package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;
import uk.ac.cam.cares.twa.cities.models.geo.Opening;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;
import uk.ac.cam.cares.twa.cities.models.geo.ThematicSurface;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;

public class ThematicSurfaceDiscoveryAgentTest extends TestCase {

  public void testValidateInput() throws NoSuchFieldException {
    // Empty params
    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.GET);
    try {
      assertFalse(new ThematicSurfaceDiscoveryAgent().validateInput(requestParams));
      fail();
    } catch (BadRequestException ignored) {
    }
    // Namespace only
    requestParams.put("namespace", "http://example.org/test");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertEquals("http://example.org/test", agent.getNamespaceIri());
      assertNull("http://example.org/test", agent.getBuildingIri());
      assertArrayEquals(new boolean[]{true, true, true, true}, agent.getLods());
      assertEquals(15.0, agent.getThreshold());
    } catch (BadRequestException ignored) {
      fail();
    }
    // Namespace and building
    requestParams.put("cityObjectIRI", "http://example.org/test/building");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertEquals("http://example.org/test", agent.getNamespaceIri());
      assertEquals("http://example.org/test/building", agent.getBuildingIri());
    } catch (BadRequestException ignored) {
      fail();
    }
    // Invalid lod argument
    requestParams.put("lod1", "invalidboolean");
    try {
      assertFalse(new ThematicSurfaceDiscoveryAgent().validateInput(requestParams));
      fail();
    } catch (BadRequestException ignored) {
    }
    // Valid lod argument
    requestParams.put("lod1", "true");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertArrayEquals(new boolean[]{true, false, false, false}, agent.getLods());
    } catch (BadRequestException ignored) {
      fail();
    }
    // Multiple lod arguments
    requestParams.put("lod4", "true");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertArrayEquals(new boolean[]{true, false, false, true}, agent.getLods());
    } catch (BadRequestException ignored) {
      fail();
    }
    // Invalid threshold argument
    requestParams.put("thresholdAngle", "invalidthreshold");
    try {
      assertFalse(new ThematicSurfaceDiscoveryAgent().validateInput(requestParams));
      fail();
    } catch (BadRequestException ignored) {
    }
    // Valid threshold argument
    requestParams.put("thresholdAngle", "13");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertEquals(13.0, agent.getThreshold());
    } catch (BadRequestException ignored) {
      fail();
    }
  }

  public void testImportSrs() throws NoSuchMethodException {

    ThematicSurfaceDiscoveryAgent agent = Mockito.spy(new ThematicSurfaceDiscoveryAgent());

    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.GET);
    requestParams.put("namespace", "http://example.org/test");
    agent.validateInput(requestParams);

    Method importSrs = agent.getClass().getDeclaredMethod("importSrs");
    importSrs.setAccessible(true);

    Mockito.doReturn("{ \"result\": \"[{'srs': 'EPSG:4326'}]\" }").when(agent).query(Mockito.anyString(), Mockito.anyString());
    try {
      importSrs.invoke(agent);
      assertEquals("EPSG:4326", GeometryType.getSourceCrsName());
    } catch (Exception ignored) {
      fail();
    }

    Mockito.doReturn("{ \"result\": \"[]\" }").when(agent).query(Mockito.anyString(), Mockito.anyString());
    try {
      importSrs.invoke(agent);
      fail();
    } catch (Exception ignored) {
    }

    Mockito.doReturn("{ \"result\": \"[{'srs': 'EPSG:4326'}, {'srs': 'EPSG:27700'}]\" }").when(agent).query(Mockito.anyString(), Mockito.anyString());
    try {
      importSrs.invoke(agent);
      fail();
    } catch (Exception ignored) {
    }

  }

}
