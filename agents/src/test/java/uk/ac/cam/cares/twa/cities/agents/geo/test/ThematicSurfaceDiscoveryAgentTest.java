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

  public void testImportSrs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    ThematicSurfaceDiscoveryAgent agent = Mockito.spy(new ThematicSurfaceDiscoveryAgent());

    JSONObject requestParams = new JSONObject();
    requestParams.put("method", HttpMethod.GET);
    requestParams.put("namespace", "http://example.org/test");
    agent.validateInput(requestParams);

    Method importSrs = agent.getClass().getDeclaredMethod("importSrs");
    importSrs.setAccessible(true);

    Mockito.doReturn("[{\"srs\": \"EPSG:4326\"}]").when(agent).query(Mockito.anyString(), Mockito.anyString());
    try {
      importSrs.invoke(agent);
      assertEquals("EPSG:4326", GeometryType.getSourceCrsName());
    } catch (Exception ignored) {
      fail();
    }

    Mockito.doReturn("[]").when(agent).query(Mockito.anyString(), Mockito.anyString());
    try {
      importSrs.invoke(agent);
      fail();
    } catch (Exception ignored) {
    }

    Mockito.doReturn("[{\"srs\": \"EPSG:4326\"}, {\"srs\": \"EPSG:27700\"}]").when(agent).query(Mockito.anyString(), Mockito.anyString());
    try {
      importSrs.invoke(agent);
      fail();
    } catch (Exception ignored) {
    }

  }

  public void instantiateTestRoom() {

    String kgId = "http://localhost:48080/local-churchill";
    String namespace = "http://localhost:9999/blazegraph/namespace/churchill/sparql";

    // Copied from TSDA
    SelectBuilder srsQuery = new SelectBuilder();
    SPARQLUtils.addPrefix("ocgml:srsname", srsQuery);
    srsQuery.addVar("?srs").addWhere(NodeFactory.createURI(namespace), "ocgml:srsname", "?srs");
    JSONArray srsResponse = SPARQLUtils.unpackQueryResponse(AccessAgentCaller.query(kgId, srsQuery.buildString()));
    GeometryType.setSourceCrsName(srsResponse.getJSONObject(0).getString("srs"));

    // Read in wall geometry
    SurfaceGeometry wallGeometry = new SurfaceGeometry();
    wallGeometry.setIri("UUID_3dc57a8b-755c-4e29-9878-95df430ddbbc", namespace);
    wallGeometry.pullAll(kgId, 0);
    GeometryFactory factory = new GeometryFactory();
    Polygon poly = wallGeometry.getGeometryType().getPolygon();
    LinearRing exteriorRing = new LinearRing(poly.getExteriorRing().getCoordinateSequence(), factory);
    LinearRing[] interiorRings = new LinearRing[poly.getNumInteriorRing()];
    // Alter wall geometry
    for (int i = 0; i < interiorRings.length - 1; i++)
      interiorRings[i] = new LinearRing(poly.getInteriorRingN(i).getCoordinateSequence(), factory);
    Coordinate[] holeCoords = new Coordinate[]{
        new Coordinate(0.101215924092872222, 52.2120853716647445, 18.5),
        new Coordinate(0.101202045558463378, 52.2120640211777055, 18.5),
        new Coordinate(0.101202045558463378, 52.2120640211777055, 16.5),
        new Coordinate(0.101215924092872222, 52.2120853716647445, 16.5),
        new Coordinate(0.101215924092872222, 52.2120853716647445, 18.5)
    };
    interiorRings[interiorRings.length - 1] = new LinearRing(new CoordinateArraySequence(holeCoords), factory);
    wallGeometry.getGeometryType().setPolygon(factory.createPolygon(exteriorRing, interiorRings));
    wallGeometry.queuePushUpdate(true, true);
    // Create window geometry
    Coordinate[] windowCoords = holeCoords.clone();
    ArrayUtils.reverse(windowCoords);
    SurfaceGeometry windowGeometry = new SurfaceGeometry();
    windowGeometry.setIri(UUID.randomUUID().toString(), namespace);
    windowGeometry.setIsComposite(0);
    windowGeometry.setIsReverse(0);
    windowGeometry.setIsSolid(0);
    windowGeometry.setIsTriangulated(0);
    windowGeometry.setIsXlink(0);
    LinearRing windowExteriorRing = new LinearRing(new CoordinateArraySequence(windowCoords), factory);
    Polygon windowPolygon = new Polygon(windowExteriorRing, new LinearRing[0], factory);
    windowGeometry.setGeometryType(new GeometryType(windowPolygon));
    windowGeometry.queuePushUpdate(true, true);
    // Create window opening object
    Opening window = Opening.newWindow();
    window.setIri(UUID.randomUUID().toString(), namespace);
    window.setLod3MultiSurfaceId(windowGeometry);
    window.setThemSurfaceId(wallGeometry.getCityObjectId());
    window.queuePushUpdate(true, true);
    // Read in parent thematic surface and add openingId property
    ThematicSurface wallThematicSurface = new ThematicSurface();
    wallThematicSurface.setIri(wallGeometry.getCityObjectId());
    wallThematicSurface.pullAll(kgId, 0);
    wallThematicSurface.getOpeningId().add(window);
    wallThematicSurface.queuePushUpdate(true, true);

    System.out.println(Model.peekUpdateQueue());

  }

}
