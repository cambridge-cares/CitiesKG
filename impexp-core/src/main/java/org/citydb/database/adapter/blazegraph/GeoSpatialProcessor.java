package org.citydb.database.adapter.blazegraph;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.citydb.registry.ObjectRegistry;
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

    public boolean IsValid (Geometry geom) {
        Object[] result = IsValidDetail(geom);
        return (boolean)result[0];
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

    /* Equivalent ST_Transform(Geometry g1, integer srid)
     * https://postgis.net/docs/ST_Transform.html
     * Return: Geometry
     * Default : SRID 4326
    * */
    public Geometry Transform (Geometry geom, int srcSRID, int dstSRID){

        GeometryFactory fac = new GeometryFactory();
        Geometry sourceGeometry = fac.createGeometry(geom);

        Geometry targetGeometry = null;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + srcSRID);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:" + dstSRID);
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            targetGeometry = JTS.transform(sourceGeometry, transform);
            targetGeometry.setSRID(dstSRID);

        } catch (FactoryException |TransformException e) {
            e.printStackTrace();
        }
        return targetGeometry;
    }

    public Geometry Force2D (Geometry geom){

        GeometryFactory fac = new GeometryFactory();

        List<Coordinate> newcoords = new ArrayList<>();
        for(Coordinate coord : geom.getCoordinates()) {
            newcoords.add(new Coordinate(coord.x,coord.y));
        }

        Geometry xyGeometry = fac.createPolygon(newcoords.toArray(new Coordinate[0]));

        return xyGeometry;
    }

    /* Equivalent ST_AREA (Geometry)
    * return sqft
    * Returns the area of a polygonal geometry. For geometry types a 2D Cartesian (planar) area is computed, with units specified by the SRID.
    * */
    public double CalculateArea(Geometry geom){

        if (geom instanceof Polygon){
            return geom.getArea();
        }
        return 0.0;
    }

    /* Equivalent as ST_UNION(geometry g1, geometry g2)
    * https://postgis.net/docs/ST_Union.html
    * */
    public Geometry Union (Geometry geom1, Geometry geom2){

        try {
            // Convert to 2D first
            if (geom1.getSRID() == 0){
                geom1.setSRID(4326);
            }
            if (geom2.getSRID() == 0){
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
    public Geometry UnaryUnion (List<Geometry> geomlist){
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
    public List<Coordinate> str2coords(String st_geometry) {
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
                if (pointXYZ.length == 2){
                    coords.add(new Coordinate(Double.valueOf(pointXYZ[0]), Double.valueOf(pointXYZ[1])));
                }else if(pointXYZ.length == 3){
                    coords.add(new Coordinate(Double.valueOf(pointXYZ[0]), Double.valueOf(pointXYZ[1]), Double.valueOf(pointXYZ[2])));
                }else{
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
            }else if (pointXYZList.length % 2 == 0) {
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
    public Geometry createGeometry (String coordlist){
        GeometryFactory fac = new GeometryFactory();
        Geometry geom = fac.createPolygon(str2coords(coordlist).toArray(new Coordinate[0]));
        return geom;
    }

}
