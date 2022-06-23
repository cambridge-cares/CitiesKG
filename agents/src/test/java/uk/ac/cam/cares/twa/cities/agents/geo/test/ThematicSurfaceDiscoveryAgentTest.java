package uk.ac.cam.cares.twa.cities.agents.geo.test;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;


public class ThematicSurfaceDiscoveryAgentTest{

  @Test
  public void testValidateInput() {
    // Empty params
    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.PUT);
    try {
      assertFalse(new ThematicSurfaceDiscoveryAgent().validateInput(requestParams));
      fail();
    } catch (BadRequestException ignored) {
    }
    // Namespace only
    requestParams.put("namespace", "http://example.org/test/");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertEquals("http://example.org/test/", agent.getNamespaceIri());
      assertNull(agent.getBuildingIri());
      assertArrayEquals(new boolean[]{true, true, true, true}, agent.getTaskParams().lods);
      assertEquals(5.0, agent.getTaskParams().threshold);
      assertEquals(ThematicSurfaceDiscoveryAgent.Mode.RESTRUCTURE, agent.getTaskParams().mode);
      assertEquals("http://example.org/test/", agent.getTaskParams().namespace);
    } catch (BadRequestException ignored) {
      fail();
    }
    // Namespace and building
    requestParams.put("cityObjectIRI", "http://example.org/test/building");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertEquals("http://example.org/test/", agent.getNamespaceIri());
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
      assertArrayEquals(new boolean[]{true, false, false, false}, agent.getTaskParams().lods);
    } catch (BadRequestException ignored) {
      fail();
    }
    // Multiple lod arguments
    requestParams.put("lod4", "true");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertArrayEquals(new boolean[]{true, false, false, true}, agent.getTaskParams().lods);
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
      assertEquals(13.0, agent.getTaskParams().threshold);
    } catch (BadRequestException ignored) {
      fail();
    }
    // Comment argument
    requestParams.put("footprint", "1");
    try {
      ThematicSurfaceDiscoveryAgent agent = new ThematicSurfaceDiscoveryAgent();
      assertTrue(agent.validateInput(requestParams));
      assertEquals(ThematicSurfaceDiscoveryAgent.Mode.RESTRUCTURE, agent.getTaskParams().mode);
    } catch (BadRequestException ignored) {
      fail();
    }
  }

  @Test
  public void testImportSrs() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

    ModelContext mockContext = Mockito.spy(new ModelContext("", ""));

    ThematicSurfaceDiscoveryAgent agent = Mockito.spy(new ThematicSurfaceDiscoveryAgent());;

    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.PUT);
    requestParams.put("namespace", "http://example.org/test/");
    agent.validateInput(requestParams);

    Field taskParamsField = agent.getClass().getDeclaredField("taskParams");
    taskParamsField.setAccessible(true);
    ThematicSurfaceDiscoveryAgent.Params mockParams = Mockito.spy((ThematicSurfaceDiscoveryAgent.Params)taskParamsField.get(agent));
    Mockito.doReturn(mockContext).when(mockParams).makeContext();
    taskParamsField.set(agent, mockParams);

    Method importSrs = agent.getClass().getDeclaredMethod("importSrs");
    importSrs.setAccessible(true);

    Mockito.doReturn(new JSONArray("[{'srs': 'EPSG:4326'}]")).when(mockContext).query(Mockito.anyString());
    try {
      importSrs.invoke(agent);
      assertEquals("EPSG:4326", GeometryType.getSourceCrsName());
    } catch (Exception ignored) {
      fail();
    }

    Mockito.doReturn(new JSONArray()).when(mockContext).query(Mockito.anyString());
    try {
      importSrs.invoke(agent);
      fail();
    } catch (Exception ignored) {
    }

    Mockito.doReturn(new JSONArray("[{'srs': 'EPSG:4326'}, {'srs': 'EPSG:27700'}]")).when(mockContext).query(Mockito.anyString());
    try {
      importSrs.invoke(agent);
      fail();
    } catch (Exception ignored) {
    }

  }

}
