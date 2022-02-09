package org.citydb.database.adapter.blazegraph;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.citydb.config.geometry.GeometryObject;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class GeoSpatialProcessor {
    public GeoSpatialProcessor() {
    }

    public boolean IsValid(Geometry geom) {
        Object[] result = IsValidDetail(geom);
        return (boolean) result[0];
    }

    /* Equivalent to ST_isValidDetail, geoJena_IsValidDetail
     * Input of IsValidOp is; POLYGON () , 2D polygon of LinearRing
     * Return: Object[] with [Boolean, String, Null/Point]*/
    public Object[] IsValidDetail(Geometry geom) {

        GeometryFactory fac = new GeometryFactory();
        IsValidOp isValidOp = new IsValidOp(geom); // Polygon 2D consists of LinearRing with shell and hole
        boolean result_boolean = isValidOp.isValid();
        Object[] details = new Object[3];

        TopologyValidationError error = isValidOp.getValidationError();
        if (error != null) {
            details[0] = false;
            details[1] = error.getMessage();
            details[2] = fac.createPoint(error.getCoordinate());
        } else {
            details[0] = true;
            details[1] = "Valid Geometry";
        }
        //System.out.println(details[0].getClass().getSimpleName());
        //NodeValue result_message = NodeValue.makeString(Arrays.toString(details));
        return details;
    }

    /* Method using osgeo gdal CoordinateTransformation */
    public Geometry reProject (Geometry sourceGeom, int from_epsg, int to_epsg) {

        Coordinate[] coordinates = sourceGeom.getCoordinates();
        org.gdal.ogr.Geometry ring = new org.gdal.ogr.Geometry(ogr.wkbLinearRing);

        for ( Coordinate coord : coordinates) {
            ring.AddPoint(coord.getX(), coord.getY());
        }

        org.gdal.ogr.Geometry polygon = new org.gdal.ogr.Geometry(ogr.wkbPolygon);
        polygon.AddGeometry(ring);

        SpatialReference source = new SpatialReference();
        source.ImportFromEPSG(from_epsg);

        SpatialReference target = new SpatialReference();
        target.ImportFromEPSG(to_epsg);

        CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);

        polygon.Transform(transformMatrix);

        double[][] convertedPoints = polygon.GetPoints();

        List<Coordinate> convertedCoords = new ArrayList<>();
        for ( int i = 0; i < convertedPoints[0].length; ++i) {
            //convertedCoords.add(new Coordinate())
        }

        return null;
    }



    /**
     * Simulate ST_Transform for blazegraph based on JTS, equivalent ST_Transform(Geometry g1, integer srid)
     * https://postgis.net/docs/ST_Transform.html
     *
     * @param sourceGeometry     - Geometry to be transformed
     * @param srcSRID            - Source srid
     * @param dstSRID            - Target srid (Default : SRID 4326)
     * @return Geometry - make sure the incoming and outgoing has the same format (type, dimension)
     */
    public Geometry Transform(Geometry sourceGeometry, int srcSRID, int dstSRID) {

        GeometryFactory fac = new GeometryFactory();

        // need to reverse the coordinates of the polygpn
        //Coordinate[] sourceCoords = getReversedCoordinates(geom);

        //Geometry sourceGeometry = fac.createGeometry(geom);

        Geometry targetGeometry = null;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + srcSRID);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:" + dstSRID);
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            targetGeometry = JTS.transform(sourceGeometry, transform);
            targetGeometry.setSRID(dstSRID);

        } catch (FactoryException | TransformException e) {
            e.printStackTrace();
        }
        return targetGeometry;
    }

    public Geometry Force2D(Geometry geom) {

        GeometryFactory fac = new GeometryFactory();

        List<Coordinate> newcoords = new ArrayList<>();
        for (Coordinate coord : geom.getCoordinates()) {
            newcoords.add(new Coordinate(coord.x, coord.y));
        }

        Geometry xyGeometry = fac.createPolygon(newcoords.toArray(new Coordinate[0]));

        return xyGeometry;
    }

    /* Equivalent ST_AREA (Geometry)
     * return sqft
     * Returns the area of a polygonal geometry. For geometry types a 2D Cartesian (planar) area is computed, with units specified by the SRID.
     * */
    public double CalculateArea(Geometry geom) {

        if (geom instanceof Polygon) {
            return geom.getArea();
        }
        return 0.0;
    }

    /* Equivalent as ST_UNION(geometry g1, geometry g2)
     * https://postgis.net/docs/ST_Union.html
     * */
    public Geometry Union(Geometry geom1, Geometry geom2) {

        try {
            // Convert to 2D first
            if (geom1.getSRID() == 0) {
                geom1.setSRID(4326);
            }
            if (geom2.getSRID() == 0) {
                geom2.setSRID(4326);
            }

            Geometry transGeom2 = Transform(geom2, geom2.getSRID(), geom1.getSRID());
            // Union
            Geometry uniongeom = geom1.union(transGeom2);
            return uniongeom;
        } catch (DatatypeFormatException | MismatchedDimensionException ex) {
            throw new ExprEvalException(ex.getMessage(), ex);
        }
    }

    /* Equivalent ST_UNION with multiple Geometry together */
    public Geometry UnaryUnion(List<Geometry> geomlist) {
        GeometryFactory fac = new GeometryFactory();
        Geometry[] col = geomlist.toArray(new Geometry[0]);
        GeometryCollection coll = new GeometryCollection(col, fac);
        UnaryUnionOp op = new UnaryUnionOp(coll);
        Geometry union = op.union();
        return union;
    }

    /*Convert the input String into list of coordinates
     * Polygon testpoly2 = fac.createPolygon(str2coords(testpolygon2).toArray(new Coordinate[0]));
     * */
    public static List<Coordinate> str2coords(String st_geometry) {
        String[] pointXYZList = null;
        List<Coordinate> coords = new LinkedList<Coordinate>();

        if (st_geometry.contains(",")) {
            //System.out.println("====================== InputString is from POSTGIS");
            pointXYZList = st_geometry.split(",");

            for (int i = 0; i < pointXYZList.length; ++i) {
                String[] pointXYZ = pointXYZList[i].split(" ");
                List<String> coordinates = new LinkedList<String>(Arrays.asList(pointXYZ));
                coordinates.removeIf(String::isEmpty);
                //coordinates.removeAll(Arrays.asList(null, ""));
                pointXYZ = coordinates.toArray(new String[0]);
                if (pointXYZ.length == 2) {
                    coords.add(new Coordinate(Double.valueOf(pointXYZ[0]), Double.valueOf(pointXYZ[1])));
                } else if (pointXYZ.length == 3) {
                    coords.add(new Coordinate(Double.valueOf(pointXYZ[0]), Double.valueOf(pointXYZ[1]), Double.valueOf(pointXYZ[2])));
                } else {
                    System.out.println("InputString has no valid format");
                    return null;
                }
            }
        } else if (st_geometry.contains("#")) {
            //System.out.println("====================== InputString is from Blazegraph");
            pointXYZList = st_geometry.split("#");
            if (pointXYZList.length % 3 == 0) {
                // 3d coordinates
                for (int i = 0; i < pointXYZList.length; i = i + 3) {
                    coords.add(new Coordinate(Double.valueOf(pointXYZList[i]), Double.valueOf(pointXYZList[i + 1]), Double.valueOf(pointXYZList[i + 2])));
                }
            } else if (pointXYZList.length % 2 == 0) {
                // 2d coordinates
                for (int i = 0; i < pointXYZList.length; i = i + 2) {
                    coords.add(new Coordinate(Double.valueOf(pointXYZList[i]), Double.valueOf(pointXYZList[i + 1])));
                }
            }
        } else {
            System.out.println("InputString has no valid format");
            return null;
        }
        return coords;

    }

    public Geometry createGeometry(String coordlist, String geomtype, int dimension, int[] dimOfRings) {
        GeometryFactory fac = new GeometryFactory();
        Coordinate[] coordinates = str2coords(coordlist).toArray(new Coordinate[0]);

        Geometry geom = null;
        if (geomtype.equals("POLYGON")){
            if (dimOfRings.length >= 2){ // Polygon with Holes : LinearRing shell and LinearRing[] holes
                Coordinate[] shell_coords = Arrays.copyOfRange(coordinates, 0, dimOfRings[0] / dimension);
                LinearRing shell = fac.createLinearRing(shell_coords);
                ArrayList<LinearRing> holeslist = new ArrayList<>();
                int start = dimOfRings[0] / dimension;
                for (int k = 1; k < dimOfRings.length; ++k){
                    Coordinate[] holes_coords = Arrays.copyOfRange(coordinates, start, start + dimOfRings[k] /dimension);
                    holeslist.add(fac.createLinearRing(holes_coords));
                    start = start + dimOfRings[k] / dimension;
                }
                LinearRing[] holes = holeslist.toArray(new LinearRing[0]);
                geom = fac.createPolygon(shell, holes);
            } else if (dimOfRings.length == 1) {
                // Polygon without holes
                LinearRing shell = fac.createLinearRing(coordinates);
                geom = fac.createPolygon(shell);
            }
        }
        return geom;
    }

    public Geometry createGeometry(String coordlist) {
        GeometryFactory fac = new GeometryFactory();
        Geometry geom = fac.createPolygon(str2coords(coordlist).toArray(new Coordinate[0]));

        // Either coordlist to double[] or to Coordinates[]
        //GeometryObject geomObj = GeometryObject.createPolygon();

        return geom;
    }
    // Need to consider 2d or 3d, @todo: CHECK 2D and 3D
    public Coordinate[] getReversedCoordinates(Geometry geometry) {

        Coordinate[] original = geometry.getCoordinates();
        Coordinate[] reversed = new Coordinate[original.length];

        for (int i = 0; i < original.length; i++) {
                reversed[i] = new Coordinate(original[i].getY(), original[i].getX(), original[i].getZ());
        }

        return reversed;
    }

    // Reverse the coordinates X and Y
    public Coordinate[] reverseCoordinates (Coordinate[] original) {

        Coordinate[] reversed = new Coordinate[original.length];
        for (int i = 0; i < original.length; i++) {
            reversed[i] = new Coordinate(original[i].getY(), original[i].getX(), original[i].getZ());
        }
        return reversed;
    }


    /* Todo String geomtype, int dimension, int[] dimOfRings */
    public static GeometryObject create3dPolygon(String coordlist, String datatypeURI){

        String datatype = datatypeURI.substring(datatypeURI.lastIndexOf('/') + 1);
        String[] datatype_list = datatype.split("-");
        String geomtype = datatype_list[0];
        int dim = Integer.valueOf(datatype_list[1]);
        int[] dimOfRings = new int[datatype_list.length-2];
        for (int i = 2; i < datatype_list.length; ++i){
            dimOfRings[i-2] = Integer.valueOf(datatype_list[i]);
        }
        // put in createGeopmetry (extracted, geomtype, listOfDim)
        //Geometry geomobj = geospatial.createGeometry(extracted, geomtype, dim, dimOfRings);

        GeometryBuilder builder = new GeometryBuilder();
        String[] coords = coordlist.split("#");
        double[] ord = new double[coords.length];
        for (int i = 0; i < coords.length; ++i) {
            ord[i] = Double.valueOf(coords[i]);
        }
        Polygon polygon3d = null;
        Geometry geomObj = null;
        LinearRing lingring3d = null;
        Coordinate[] coordinates = str2coords(coordlist).toArray(new Coordinate[0]);
        GeometryObject geom = null;
        if (dimOfRings.length == 1) {
            //polygon3d = builder.polygonZ(ord);
            //lingring3d = builder.linearRingZ(ord);
            geom = GeometryObject.createPolygon(ord, dim, 4326);
            //LinearRing shell = fac.createLinearRing(coordinates);
            //geom = fac.createPolygon(shell);
            //System.out.println(polygon3d);
        }

        return geom;
    }

    /* Convert two-dimensional GeometryObject to three-dimensional GeometryObject */
    // make sure the geometryObj2d has srid
    public static GeometryObject convertTo3d (GeometryObject geometryObj2d, GeometryObject originalGeomObj) {

        double[][] coordinates2d = geometryObj2d.getCoordinates();
        double[][] origCoords = originalGeomObj.getCoordinates();

        if (originalGeomObj.getDimension() == 2 && geometryObj2d.getDimension() == 2) {
            return geometryObj2d;
        } else if (originalGeomObj.getDimension() == 3 && geometryObj2d.getDimension() == 2) { // replace x and y of orignalGeomObj with new coordinates
            for (int i = 0 ; i < origCoords.length; ++i) {
                int k = 0;
                for (int j = 0; j < origCoords[i].length; j+=3) {
                    origCoords[i][j] = coordinates2d[i][k];
                    origCoords[i][j+1] = coordinates2d[i][k+1];
                    k += 2;
                }
            }
        }
        originalGeomObj.setSrid(geometryObj2d.getSrid());

        return originalGeomObj;
    }
}
