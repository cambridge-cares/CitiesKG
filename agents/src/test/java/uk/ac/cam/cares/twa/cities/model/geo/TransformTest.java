package uk.ac.cam.cares.twa.cities.model.geo;

import org.gdal.ogr.Geometry;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TransformTest {

    @Test
    public void testNewTransform() {
        Transform transform = new Transform();
        assertNotNull(transform);
    }

    @Test
    public void testNewTransformMethods() {
        Transform transform = new Transform();
        assertEquals(5, transform.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testBuildPolygon() {
        // test case where dimension = 2
        double[][] geometry1 = {{0.0, 0.0}, {1.0, 0.0}, {0.0, 1.0}, {1.0, 1.0}, {0.0, 0.0}};
        Geometry geom1 = Transform.buildPolygon(geometry1);
        double[] res1 = new double[4];
        geom1.GetEnvelope(res1);

        assertEquals("POLYGON", geom1.GetGeometryName());
        assertEquals(0.0, res1[0]);
        assertEquals(1.0, res1[1]);
        assertEquals(0.0, res1[2]);
        assertEquals(1.0, res1[3]);

        // test case where dimension = 3
        double[][] geometry2 = {{0.0, 0.0, 0.0}, {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {1.0, 1.0, 0.0}, {0.0, 0.0, 0.0}};
        Geometry geom2 = Transform.buildPolygon(geometry2);
        double[] res2 = new double[6];
        geom2.GetEnvelope3D(res2);

        assertEquals("POLYGON", geom2.GetGeometryName());
        assertEquals(0.0, res2[0]);
        assertEquals(1.0, res2[1]);
        assertEquals(0.0, res2[2]);
        assertEquals(1.0, res2[3]);
        assertEquals(0.0, res2[4]);
        assertEquals(0.0, res2[5]);
    }

    @Test
    public void testGetEnvelop() {
        double[][] geometry = {{0.0, 0.0}, {1.0, 0.0}, {0.0, 1.0}, {1.0, 1.0}, {0.0, 0.0}};
        double[] res = Transform.getEnvelop(geometry);

        assertEquals(0.0, res[0]);
        assertEquals(1.0, res[1]);
        assertEquals(0.0, res[2]);
        assertEquals(1.0, res[3]);
    }

    @Test
    public void testReprojectPoint() {
        double[] centroid = {52.5, 13.3};
        double[] res = Transform.reprojectPoint(centroid, 4326, 25833);
        DecimalFormat df = new DecimalFormat("#.#####");
        assertEquals(384603.15117, Double.valueOf(df.format(res[0])));
        assertEquals(5818010.35914, Double.valueOf(df.format(res[1])));
    }

    @Test
    public void testReprojectEnvelope() {
        double[] envelope = {52.5, 52.7, 13.3, 13.5};
        double[] res = Transform.reprojectEnvelope(envelope, 4326, 25833);
        DecimalFormat df = new DecimalFormat("#.#####");
        assertEquals(5817709.52628, Double.valueOf(df.format(res[0])));
        assertEquals(5840254.64080, Double.valueOf(df.format(res[1])));
        assertEquals(384603.15117, Double.valueOf(df.format(res[2])));
        assertEquals(398641.14503, Double.valueOf(df.format(res[3])));
    }
}
