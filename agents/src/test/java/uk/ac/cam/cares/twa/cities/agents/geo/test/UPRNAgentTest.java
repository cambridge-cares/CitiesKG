package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.agents.geo.UPRNAgent;

import javax.ws.rs.HttpMethod;

public class UPRNAgentTest extends TestCase {

  public void testValidateInput() {
    UPRNAgent agent = new UPRNAgent();
    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.PUT);
    requestParams.put("namespace", "http://example.org/test/");
    agent.validateInput(requestParams);
    assertNull(agent.getBuildingIri());
    assertEquals("http://example.org/test/", agent.getNamespaceIri());

    agent = new UPRNAgent();
    requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.PUT);
    requestParams.put("namespace", "http://example.org/test/");
    requestParams.put("cityObjectIRI", "http://example.org/tests/");
    agent.validateInput(requestParams);
    assertNull(agent.getBuildingIri());
    assertEquals("http://example.org/test/", agent.getNamespaceIri());
    assertEquals("http://example.org/tests/", agent.getBuildingIri());

    agent = new UPRNAgent();
    requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.PUT);
    try {
      agent.validateInput(requestParams);
      fail();
    } catch (Exception ignored) {

    }
  }

}
