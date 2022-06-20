package org.citydb.database.adapter.blazegraph;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class GeoSpatialProcessorTest {

    String polygon_valid = "743238 2967416,743238 2967450,743265 2967450,743265.625 2967416,743238 2967416";
    String polygon3D = "743238 2967416 0,743238 2967450 0,743265 2967450 0,743265.625 2967416 0,743238 2967416 0";
    String polygon_blaze = "743238#2967416#0#743238#2967450#0#743265#2967450#0#743265.625#2967416#0#743238#2967416#0";
    String polygon_invalid = "0 0,1 1,1 2,1 1,0 0"; // Note: can not have whitespace between the comma and the number
    GeometryFactory fac = new GeometryFactory();
    GeoSpatialProcessor geospatial = new GeoSpatialProcessor();

    @Test
    public void IsValidDetailTest1() {

        // For polygon_valid
        String expected1 = "[true, Valid Geometry, null]";
        String generated1;

        List<Coordinate> polyCoord = geospatial.str2coords(polygon_valid);
        Polygon testpolygon = fac.createPolygon(polyCoord.toArray(new Coordinate[0]));
        Object[] result = geospatial.IsValidDetail(testpolygon);
        generated1 = Arrays.toString(result);
        assertEquals(expected1, generated1);
    }

    @Test
    public void IsValidDetailTest2() {
        // For polygon3D
        String expected = "[true, Valid Geometry, null]";
        String generated;

        List<Coordinate> polyCoord3D = geospatial.str2coords(polygon3D);
        Polygon testpolygon3D = fac.createPolygon(polyCoord3D.toArray(new Coordinate[0]));
        Object[] result = geospatial.IsValidDetail(testpolygon3D);
        generated = Arrays.toString(result);
        assertEquals(expected, generated);
    }

    @Test
    public void IsValidDetailTest3() {

        // For polygon_blaze
        String expected = "[true, Valid Geometry, null]";
        String generated;

        List<Coordinate> polyCoordblaze = geospatial.str2coords(polygon_blaze);
        Polygon polygonblaze = fac.createPolygon(polyCoordblaze.toArray(new Coordinate[0]));
        Object[] result = geospatial.IsValidDetail(polygonblaze);
        generated = Arrays.toString(result);
        assertEquals(expected, generated);
    }

    @Test
    public void IsValidDetailTest4() {

        // For polygon_invalid
        String expected = "[false, Self-intersection, POINT (0 0)]";
        String generated;

        List<Coordinate> polyCoordblaze = geospatial.str2coords(polygon_invalid);
        Polygon polygonblaze = fac.createPolygon(polyCoordblaze.toArray(new Coordinate[0]));
        Object[] result = geospatial.IsValidDetail(polygonblaze);
        generated = Arrays.toString(result);
        assertEquals(expected, generated);
    }

    @Test
    public void TransformTest() {

        //@TODO: the actual geometry to expect from POSTGIS:
        // https://postgis.net/docs/ST_Transform.html
        // Change Massachusetts state plane US feet geometry to WGS 84 long lat
        // POLYGON((-71.1776848522251 42.3902896512902,-71.1776843766326 42.3903829478009,
        //-71.1775844305465 42.3903826677917,-71.1775825927231 42.3902893647987,-71.177684
        //8522251 42.3902896512902));

        // it works expectly if the src and dst are the same CRS 4326
        //String expected = "POLYGON ((34.24438675212282 -73.6513909034731, 34.24438954245485 -73.6513877088177, 34.24439226412469 -73.65138779149818, 34.24438954245485 -73.6513877088177, 34.24438675212282 -73.6513909034731))";
        String expected = "POLYGON ((743238 2967416, 743238 2967450, 743265 2967450, 743265.625 2967416, 743238 2967416))";
        String generated;

        List<Coordinate> polyCoordblaze = geospatial.str2coords(polygon_valid);
        Polygon polygonblaze = fac.createPolygon(polyCoordblaze.toArray(new Coordinate[0]));
        Geometry result = geospatial.Transform(polygonblaze, 4326, 4326);
        generated = result.toString();
        assertEquals(expected, generated);
    }

    @Test
    public void Force2DTest() {

        List<Coordinate> polyCoord = geospatial.str2coords(polygon_valid);
        Polygon polygon2D = fac.createPolygon(polyCoord.toArray(new Coordinate[0]));
        Geometry expected = polygon2D;

        Geometry generated;

        List<Coordinate> polyCoord3D = geospatial.str2coords(polygon3D);
        Polygon polygon3D = fac.createPolygon(polyCoord3D.toArray(new Coordinate[0]));
        generated = geospatial.Force2D(polygon3D);
        assertEquals(expected, generated);
    }

    @Test
    public void CalculateAreaTest() {

        Double expected = 928.625;
        Double generated;
        List<Coordinate> polyCoord = geospatial.str2coords(polygon_valid);
        Polygon polygon = fac.createPolygon(polyCoord.toArray(new Coordinate[0]));
        generated = geospatial.CalculateArea(polygon);
        assertEquals(expected, generated);
    }

    @Test
    public void UnionTest() {

        String expected = "GEOMETRYCOLLECTION (LINESTRING (5 5, 10 10), POLYGON ((-7 4.2, -7.1 4.2, -7.1 4.3, -7 4.2)))";
        String generated = "";

        String polygon = "-7 4.2,-7.1 4.2,-7.1 4.3,-7 4.2";
        String linestring = "5 5 5,10 10 10";
        Polygon testpolygon = fac.createPolygon(geospatial.str2coords(polygon).toArray(new Coordinate[0]));
        LineString testlinestring = fac.createLineString(geospatial.str2coords(linestring).toArray(new Coordinate[0]));
        Geometry union = geospatial.Union(testpolygon, testlinestring);
        generated = union.toString();
        assertEquals(expected, generated);
    }

    @Test
    public void UnaryUnionTest() {

        String expected = "GEOMETRYCOLLECTION (LINESTRING (5 5, 10 10), POLYGON ((-7 4.2, -7.1 4.2, -7.1 4.3, -7 4.2)))";
        String generated;

        String polygon = "-7 4.2,-7.1 4.2,-7.1 4.3,-7 4.2";
        String linestring = "5 5 5,10 10 10";
        String point = "5 5 5";
        Polygon testpolygon = fac.createPolygon(geospatial.str2coords(polygon).toArray(new Coordinate[0]));
        LineString testlinestring = fac.createLineString(geospatial.str2coords(linestring).toArray(new Coordinate[0]));
        Coordinate pcor = new Coordinate(5, 5, 5);
        Point testpoint = fac.createPoint(pcor);
        List<Geometry> collection = new LinkedList<Geometry>();
        collection.add(testpolygon);
        collection.add(testlinestring);
        collection.add(testpoint);
        Geometry unaryunion = geospatial.UnaryUnion(collection);
        generated = unaryunion.toString();
        assertEquals(expected, generated);
    }

    @Test
    public void str2coordsTest() {
        String expected = "LINESTRING (5 5, 10 10)";
        String generated;
        String linestring = "5 5 5, 10 10 10";
        LineString testlinestring = fac.createLineString(geospatial.str2coords(linestring).toArray(new Coordinate[0]));
        generated = testlinestring.toString();
        assertEquals(expected, generated);
    }

}