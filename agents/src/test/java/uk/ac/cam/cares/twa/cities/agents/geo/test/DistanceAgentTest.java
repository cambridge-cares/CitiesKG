package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateRequest;
import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.geom.Point;
import org.mockito.*;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.jps.base.query.StoreRouter;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.models.geo.EnvelopeType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class DistanceAgentTest extends TestCase {

  public void testGetDistanceQuery()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    DistanceAgent distanceAgent = new DistanceAgent();
    String uri1 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
    String uri2 = "http://localhost/berlin/cityobject/UUID_6cbfb096-5116-4962-9162-48b736768cd4/";


    assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class));
    Method getDistanceQuery = distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class);
    getDistanceQuery.setAccessible(true);

    Query q = (Query) getDistanceQuery.invoke(distanceAgent, uri1, uri2);
    assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/>"));
    assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_6cbfb096-5116-4962-9162-48b736768cd4/>"));

  }

  public void testGetNamespace()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    DistanceAgent distanceAgent = new DistanceAgent();
    Method getNamespace = distanceAgent.getClass().getDeclaredMethod("getNamespace", String.class);
    getNamespace.setAccessible(true);

    //test whether Uri is split  and assembled into a namespace correctly.
    String uri = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
    assertEquals("http://localhost/berlin", getNamespace.invoke(distanceAgent, uri));

    //test whether Uri is split  and assembled into a namespace correctly.
    String uri2 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3";
    assertEquals("http://localhost/berlin", getNamespace.invoke(distanceAgent, uri2));
  }

  public void testGetObjectSRSQuery()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    DistanceAgent distanceAgent = new DistanceAgent();
    String uri1 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";

    assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSRSQuery", String.class, boolean.class));
    Method getObjectSRSQuery = distanceAgent.getClass().getDeclaredMethod("getObjectSRSQuery", String.class, boolean.class);
    getObjectSRSQuery.setAccessible(true);

    //test with mocked kgClient and kgRouter when it returns source srs.
    Query q = (Query) getObjectSRSQuery.invoke(distanceAgent, uri1, true);
    assertTrue(q.toString().contains("<http://localhost/berlin/sparql>"));
    assertTrue(q.toString().contains("ocgml:srsname"));

    //test with mocked kgClient and kgRouter when it returns a target srs.
    Query q2 = (Query) getObjectSRSQuery.invoke(distanceAgent, uri1, false);
    assertTrue(q2.toString().contains("<http://localhost/berlin/sparql>"));
    assertTrue(q2.toString().contains("ocgml:metricSrsName"));
  }

  public void testGetObjectSrs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

    DistanceAgent distanceAgent = new DistanceAgent();

    // Get getObjectSrs method
    assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class));
    Method getObjectSrs = distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class);
    getObjectSrs.setAccessible(true);

    String uri = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";

    try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {

      //test with mocked kgClient and kgRouter when method is called to return appropriate metric srs.
      accessAgentCallerMock.when(() -> AccessAgentCaller.query(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn("{ \"result\": \"[{'srsName': 'EPSG:3414'}]\" }");
      assertEquals("EPSG:3414", getObjectSrs.invoke(distanceAgent, uri, true));

      //test with mocked kgClient and kgRouter when there is no string to return.
      accessAgentCallerMock.when(() -> AccessAgentCaller.query(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn("{ \"result\": \"[]\" }");
      assertEquals("EPSG:4236", getObjectSrs.invoke(distanceAgent, uri, true));

    }

  }

  public void testGetDistance() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

    DistanceAgent distanceAgent = new DistanceAgent();

    // Get getDistance method
    assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class));
    Method getDistance = distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class);
    getDistance.setAccessible(true);

    try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {

      //test with mocked AccessAgentCaller when it returns a string.
      accessAgentCallerMock.when(() -> AccessAgentCaller.query(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn("{ \"result\": \"[{'distance': 10.0}]\" }");
      assertEquals(10.0, getDistance.invoke(distanceAgent, "http://localhost/berlin/cityobject/UUID_1/", "http://localhost/berlin/cityobject/UUID_2/"));

      //test with mocked AccessAgentCaller when there is no string to return.
      accessAgentCallerMock.when(() -> AccessAgentCaller.query(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn("{ \"result\": \"[]\" }");
      assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class));
      getDistance.setAccessible(true);
      assertEquals(-1.0, getDistance.invoke(distanceAgent, "http://localhost/berlin/cityobject/UUID_1/", "http://localhost/berlin/cityobject/UUID_2/"));

    }

  }

  public void testComputeDistance() {

    DistanceAgent distanceAgent = new DistanceAgent();

    EnvelopeType.setSourceCrsName("EPSG:24500");

    // test distance calculation without CRS conversion.
    String envelopeString1 = "1#1#0#1#2#0#2#2#0#2#1#0#1#1#0";
    EnvelopeType envelope1 = new EnvelopeType(envelopeString1, "POLYGON-3-15");

    String envelopeString2 = "1#2#1#1#3#1#2#3#1#2#2#1#1#2#1";
    EnvelopeType envelope2 = new EnvelopeType(envelopeString2, "POLYGON-3-15");

    assertEquals(0.999897609154347, distanceAgent.computeDistance(envelope1, envelope2));

    // test distance calculation with CRS conversion.
    EnvelopeType.setSourceCrsName("EPSG:3414");
    String envelopeString3 = "2.85#-1.85#0#2.85#0.15#0#4.85#0.15#0#4.85#-1.85#0#2.85#-1.85#0";
    EnvelopeType envelope3 = new EnvelopeType(envelopeString3, "POLYGON-3-15");

    assertEquals(1.009065402591304, distanceAgent.computeDistance(envelope1, envelope3));
  }

  public void testGetSetDistanceQuery() {

    DistanceAgent distanceAgent = new DistanceAgent();

    String uri1 = "http:cityobjectxample/cityobject2";
    String uri2 = "http:cityobjectxample/cityobject3";
    String distanceUriMock = "123e4567-e89b-12d3-a456-556642440000";
    String valueUriMock = "123e4567-e89b-12d3-a456-556642441111";
    double distance = 10.0;

    try (MockedStatic<UUID> randomUUID = Mockito.mockStatic(UUID.class, RETURNS_DEEP_STUBS)) {

      randomUUID.when(() -> UUID.randomUUID().toString()).thenReturn(distanceUriMock, valueUriMock);

      assertNotNull(distanceAgent.getClass().getDeclaredMethod("getSetDistanceQuery", String.class, String.class, double.class));
      Method getSetDistanceQuery = distanceAgent.getClass().getDeclaredMethod("getSetDistanceQuery", String.class, String.class, double.class);
      getSetDistanceQuery.setAccessible(true);

      UpdateRequest ur = (UpdateRequest) getSetDistanceQuery.invoke(distanceAgent, uri1, uri2, distance);

      assertTrue(ur.toString().contains("<http:cityobjectxample/cityobject2>"));
      assertTrue(ur.toString().contains("<http:cityobjectxample/cityobject3>"));
      assertTrue(ur.toString().contains("10.0"));
      assertTrue(ur.toString().contains("123e4567-e89b-12d3-a456-556642440000"));
      assertTrue(ur.toString().contains("123e4567-e89b-12d3-a456-556642441111"));
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      fail();
    }
  }

  public void testSetDistance() throws NoSuchMethodException {

    DistanceAgent distanceAgent = new DistanceAgent();

    try(MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {
      assertNotNull(distanceAgent.getClass().getDeclaredMethod("setDistance", String.class, String.class, double.class));
      Method setDistance = distanceAgent.getClass().getDeclaredMethod("setDistance", String.class, String.class, double.class);
      setDistance.setAccessible(true);
      try {
        setDistance.invoke(distanceAgent, "http://localhost/berlin/cityobject/UUID_1/", "http://localhost/berlin/cityobject/UUID_2", 10.0);
      } catch (Exception e) {
        fail();
      }
    }

  }
}