package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.twa.cities.agents.geo.UPRNAgent;
import uk.ac.cam.cares.twa.cities.models.osid.UPRN;

import javax.ws.rs.HttpMethod;

public class UPRNAgentTest extends TestCase {

  public void testCrossCrsUprnQuery() {
    UPRNAgent agent = new UPRNAgent();
    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.PUT);
    requestParams.put("namespace", "http://example.org/test/");
    agent.validateInput(requestParams);
    // The [280000,180000,300000,200000] bounding box in converted from EPSG27700 to EPSG4326.
    UPRN[] uprns = agent.queryUprns(-3.7304225, 51.5061517, -3.4480673, 51.6898178, "EPSG:4326");
    for(UPRN uprn: uprns) {
      Coordinate en = uprn.getEastingNorthingCoordinate().coordinate;
      assertTrue(en.x >= 280000);
      assertTrue(en.x <= 300000);
      assertTrue(en.y >= 180000);
      assertTrue(en.y <= 200000);
    }
  }

}
