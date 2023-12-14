package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.twa.cities.model.geo.CityObject;
import uk.ac.cam.cares.twa.cities.model.geo.GeometryType;
import uk.ac.cam.cares.twa.cities.models.osid.UPRN;
import uk.ac.cam.cares.twa.cities.tasks.geo.UPRNTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UPRNTaskTest extends TestCase {

  public void testCrossCrsUprnQuery() {
    CityObject cityObject = new CityObject();
    UPRNTask task = new UPRNTask(new ModelContext("", ""), null, "http://example.org/test/");
    // The [280000,180000,300000,200000] bounding box in converted from EPSG27700 to EPSG4326.
    UPRN[] uprns = task.queryUprns(-3.7304225, 51.5061517, -3.4480673, 51.6898178, "EPSG:4326", cityObject);
    for(UPRN uprn: uprns) {
      Coordinate en = uprn.getEastingNorthingCoordinate().coordinate;
      assertTrue(en.x >= 280000);
      assertTrue(en.x <= 300000);
      assertTrue(en.y >= 180000);
      assertTrue(en.y <= 200000);
    }
  }

  public void testUprnIntersectsGeometry() {
    GeometryType.setSourceCrsName("EPSG:27700");
    GeometryType geometry = new GeometryType(
        "543626.6202277706#259510.1093639868#18.5254077904#543621.8905667303#259500.9525121823#18.5092948517#543620.9679897745#259501.3891645554#18.5077098203#543623.6127418512#259511.5327963269#18.520240785#543626.6202277706#259510.1093639868#18.5254077904",
        "http://localhost/blazegraph/literals/POLYGON-3-15"
    );
    ModelContext context = new ModelContext("", "");
    UPRN uprn = UPRN.loadFromJson(context, new JSONObject("{\"geometry\":{\"coordinates\":[543623,259507],\"type\":\"Point\"},\"type\":\"Feature\",\"properties\":{\"OBJECTID\":38960595,\"UPRN\":200004159068,\"XCoordinate\":543623,\"Latitude\":52.2150766,\"Longitude\":0.1008421,\"YCoordinate\":259507}}"));

    assertTrue(UPRNTask.uprnIntersectsGeometry(uprn, geometry));
  }

  public void getTailTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method getTailMethod = UPRNTask.class.getMethod("getTail", String.class);
    getTailMethod.setAccessible(true);
    assertEquals("test4", getTailMethod.invoke(null, "test/test2/test3/test4"));
    assertEquals("test4", getTailMethod.invoke(null, "test/test2/test3/test4/"));
  }

}
