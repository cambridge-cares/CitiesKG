package org.citydb.database.adapter.blazegraph;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.List;

public class GeoSpatialProcessor {


    /* Equivalent to ST_isValidDetail, geoJena_IsValidDetail
    * Input of IsValidOp is; POLYGON () , 2D polygon of LinearRing
    * Return: Object[] with [Boolean, String, Null/Point]*/
    public static Object[] IsValid(Geometry geom) {

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

    /* Equivalen ST_Transform(Geometry g1, integer srid)
     * https://postgis.net/docs/ST_Transform.html
     * Return: Geometry
     * Default : SRID 4326
    * */
    public static Geometry transform (Geometry geom, int srcSRID, int dstSRID){

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

    public static Geometry Force2D (Geometry geom){

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
    public static double CalculateArea(Geometry geom){

        if (geom instanceof Polygon){
            return geom.getArea();
        }
        return 0.0;
    }
    /* Equivalent as ST_UNION(geometry g1, geometry g2)
    * https://postgis.net/docs/ST_Union.html
    * */
    public static Geometry Union (Geometry geom1, Geometry geom2){

        try {
            // Convert to 2D first
            if (geom1.getSRID() == 0){
                geom1.setSRID(4326);
            }
            if (geom2.getSRID() == 0){
                geom2.setSRID(4326);
            }

            Geometry transGeom2 = transform(geom2, geom2.getSRID(), geom1.getSRID());
            // Union
            Geometry uniongeom = geom1.union(transGeom2);
            return uniongeom;
        } catch (DatatypeFormatException | MismatchedDimensionException ex) {
            throw new ExprEvalException(ex.getMessage(), ex);
        }

    }


}
