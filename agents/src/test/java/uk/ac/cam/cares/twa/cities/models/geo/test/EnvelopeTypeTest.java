package uk.ac.cam.cares.twa.cities.models.geo.test;

import junit.framework.TestCase;
import org.apache.jena.graph.Node;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.math.Vector3D;
import org.mockito.Mockito;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.twa.cities.models.geo.EnvelopeType;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnvelopeTypeTest extends TestCase {

  public void testComputeBounds() {
    Polygon bounds = EnvelopeType.computeBounds(new Coordinate[]{
        new Coordinate(0, -2, 0),
        new Coordinate(1, 0, 0),
        new Coordinate(0, 0, 2),
        new Coordinate(0, 4, 0),
        new Coordinate(0, -2, 0),
        new Coordinate(2, 3, 1),
        new Coordinate(3, 2, 1),
        new Coordinate(1, 2, 2),
        new Coordinate(2, 7, 6),
        new Coordinate(2, 3, 1)
    });
    assertEquals(new Coordinate(0, -2, 0), bounds.getExteriorRing().getCoordinateN(0));
    assertEquals(new Coordinate(3, 7, 6), bounds.getExteriorRing().getCoordinateN(2));
  }

}