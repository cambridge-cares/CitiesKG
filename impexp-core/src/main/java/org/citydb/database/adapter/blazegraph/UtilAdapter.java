package org.citydb.database.adapter.blazegraph;

import java.util.Arrays;
import org.citydb.config.geometry.BoundingBox;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.config.geometry.GeometryType;
import org.citydb.config.geometry.MultiPolygon;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractUtilAdapter;
import org.citydb.database.adapter.IndexStatusInfo;
import org.citydb.database.connection.DatabaseMetaData;
import org.citydb.database.version.DatabaseVersion;
import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.LinearRing;

public class UtilAdapter extends AbstractUtilAdapter {

    protected UtilAdapter(AbstractDatabaseAdapter databaseAdapter) {
        super(databaseAdapter);
    }

    @Override
    protected void getCityDBVersion(DatabaseMetaData metaData, String schema, Connection connection) throws SQLException {

        //@TODO: Replace those values with the ones actually retrieved from the database
        String productVersion = "4.0.x";
        int major = 4;
        int minor = 0;
        int revision = 0;

        metaData.setCityDBVersion(new DatabaseVersion(major, minor, revision, productVersion));
    }

    @Override
    protected void getDatabaseMetaData(DatabaseMetaData metaData, String schema, Connection connection) throws SQLException {

    }

    @Override
    protected void getSrsInfo(DatabaseSrs srs, Connection connection) throws SQLException {

    }

    @Override
    protected void changeSrs(DatabaseSrs srs, boolean doTransform, String schema, Connection connection) throws SQLException {

    }

    @Override
    protected String[] createDatabaseReport(String schema, Connection connection) throws SQLException {
        return new String[0];
    }

    @Override
    protected BoundingBox calcBoundingBox(String schema, List<Integer> classIds, Connection connection) throws SQLException {
        return null;
    }

    @Override
    protected BoundingBox createBoundingBoxes(List<Integer> classIds, boolean onlyIfNull, Connection connection) throws SQLException {
        return null;
    }

    /* This method is used for boundingbox method for exporting tiles
    *  2D ST_TRANSFORM */
    @Override
    protected BoundingBox transformBoundingBox(BoundingBox bbox, DatabaseSrs sourceSrs, DatabaseSrs targetSrs, Connection connection) throws SQLException {
        BoundingBox result = new BoundingBox(bbox);
        int sourceSrid = sourceSrs.getSrid(); // 4326
        int targetSrid = targetSrs.getSrid(); // 25833 for berlin // 31466 for testing purpose
        targetSrid = 31466 ;

        List<Coordinate> bboxCoordList = new ArrayList<>();

        bboxCoordList.add(new Coordinate( bbox.getLowerCorner().getX() , bbox.getLowerCorner().getY()) );
        bboxCoordList.add(new Coordinate( bbox.getLowerCorner().getX() , bbox.getUpperCorner().getY()) );
        bboxCoordList.add(new Coordinate( bbox.getUpperCorner().getX() , bbox.getUpperCorner().getY()) );
        bboxCoordList.add(new Coordinate( bbox.getUpperCorner().getX() , bbox.getLowerCorner().getY()) );
        bboxCoordList.add(new Coordinate( bbox.getLowerCorner().getX() , bbox.getLowerCorner().getY()) );
         /**
        bboxCoordList.add(new Coordinate( 743238, 2967416 ));
        bboxCoordList.add(new Coordinate( 743238, 2967450 ));
        bboxCoordList.add(new Coordinate( 743265, 2967450 ));
        bboxCoordList.add(new Coordinate( 743265.625, 2967416 ));
        bboxCoordList.add(new Coordinate( 743238, 2967416 ));
        **/
        Coordinate[] bboxCoord = bboxCoordList.toArray(bboxCoordList.toArray(new Coordinate[0]));
        GeometryFactory fac = new GeometryFactory();
        Geometry bboxPolygon = fac.createPolygon(bboxCoord);
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        Geometry converted = geospatial.Transform(bboxPolygon, sourceSrid, sourceSrid);
        //Geometry testconverted = geospatial.reProject(bboxPolygon, sourceSrid, sourceSrid);
        Coordinate[] reverseCoord = geospatial.getReversedCoordinates(converted);

        result.getLowerCorner().setX(reverseCoord[0].x);
        result.getLowerCorner().setY(reverseCoord[0].y);
        result.getUpperCorner().setX(reverseCoord[2].x);
        result.getUpperCorner().setY(reverseCoord[2].y);

        result.setSrs(targetSrs);

        return result;
    }

    /**
     * Simulate ST_Transform for blazegraph using the GeoSpatialProcessor (based on JTS)
     *
     * @param geometry     - GeometryObject to be transformed, can contain multipolygon
     * @param targetSrs    - target SRID
     * @param connection   - database connection
     * @return GeometryObject - make sure the incoming and outgoing has the same format (dimension)
     */
    @Override
    protected GeometryObject transform(GeometryObject geometry, DatabaseSrs targetSrs, Connection connection) throws SQLException {

        GeometryObject converted3d = null;

        int numGeometry = geometry.getNumElements();
        double[][] coordinates = geometry.getCoordinates();
        int dim = geometry.getDimension();


        if (geometry.getSrid() == 0){
            geometry.setSrid(25833);  // 25833 for berlin data // 28992 for the hague
        }
        if (targetSrs.getSrid() == 0) {
            targetSrs.setSrid(4326);
        }

        GeometryFactory fac = new GeometryFactory();  // no polygonZ
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        GeometryBuilder geometrybuilder = new GeometryBuilder();
        List<Geometry> polygonlist = new ArrayList<>();

        for (int j = 0; j < numGeometry; ++j) {
            List<Coordinate> polygoncoord = new ArrayList<>();
            for (int k = 0; k < coordinates[j].length; k = k + dim){
                if (dim == 2) {
                    polygoncoord.add(new Coordinate(coordinates[j][k], coordinates[j][k+1]));  // Things go wrong here
                } else {
                    polygoncoord.add(new Coordinate(coordinates[j][k], coordinates[j][k+1], coordinates[j][k+2]));
                }

            }
            Coordinate[] polygonCoord = polygoncoord.toArray(polygoncoord.toArray(new Coordinate[0]));
            //Coordinate[] reverseedPolygonCoord = geospatial.reverseCoordinates(polygonCoord);
            polygonlist.add(fac.createPolygon(polygonCoord));
        }

        ArrayList<Geometry> convertedGeometry = new ArrayList<>();

        // need to reverse the coordinates to match POSTGIS results, and for multiolygon we use union.
        for (int i = 0; i < numGeometry; ++i){ // For multipolygon, numGeometry = 2
            Geometry converted = geospatial.Transform(polygonlist.get(i), geometry.getSrid(), targetSrs.getSrid()); // The hague: 28992, berlin: 25933 / 25833
            Coordinate[] reverseCoord = geospatial.getReversedCoordinates(converted);
            Geometry reverseConverted = fac.createPolygon(reverseCoord);
            reverseConverted.setSRID(targetSrs.getSrid());
            convertedGeometry.add(reverseConverted);
        }


        GeometryCollection geometryCollection = null;
        Geometry union = null;
        if (numGeometry > 1) {
            geometryCollection = (GeometryCollection) fac.buildGeometry(convertedGeometry);
            union = geometryCollection.union();
        } else {
            union = geospatial.UnaryUnion(convertedGeometry);
        }
        ////Geometry union = geospatial.UnaryUnion(convertedGeometry);
        ////GeometryObject geomObj2d = databaseAdapter.getGeometryConverter().getGeometry(union);

        GeometryObject geomObj2d = databaseAdapter.getGeometryConverter().getGeometry(union);

        // need to reverse the coordinates to match POSTGIS results --> move to somewhere
        //Geometry union = geospatial.UnaryUnion(convertedGeometry);
        //Coordinate[] convertedCoords = converted.getCoordinates();  // @Note: this does not work with multipolygon. The coordinates are combined together.
        //Coordinate[] reversedConvertedCoords = geospatial.reverseCoordinates(convertedCoords);
        //Geometry convertedColl = fac.createPolygon(reversedConvertedCoords);
        //GeometryObject geomObj2d = databaseAdapter.getGeometryConverter().getGeometry(converted);
        converted3d = GeoSpatialProcessor.convertTo3d(geomObj2d, geometry);  // convert 2d to 3d coordinates

        //double[][] convertedCoords = result.getCoordinates();
        //result.setSrid(targetSrs.getSrid());

        return converted3d;
    }

    @Override
    protected int get2DSrid(DatabaseSrs srs, Connection connection) throws SQLException {
        return 0;
    }

    @Override
    protected IndexStatusInfo manageIndexes(String operation, IndexStatusInfo.IndexType type, String schema, Connection connection) throws SQLException {
        //@TODO: replace with implementation retrieving values from database
        String[] result = new String[] {};
        return IndexStatusInfo.createFromDatabaseQuery(result, type);
    }

    @Override
    protected boolean updateTableStats(IndexStatusInfo.IndexType type, String schema, Connection connection) throws SQLException {
        return false;
    }

    @Override
    protected boolean containsGlobalAppearances(Connection connection) throws SQLException {
        return false;
    }

    @Override
    protected int cleanupGlobalAppearances(String schema, Connection connection) throws SQLException {
        return 0;
    }

    @Override
    public DatabaseSrs getWGS843D() {
        return null;
    }

    @Override
    public boolean isIndexEnabled(String tableName, String columnName) throws SQLException {

        // @TODO: Implement the check for spatial indexes on geometry columns of involved tables
        // For the moment, we set it to always return true. In the future, it should have checked if the spatial index is enabled in DB
        boolean isIndexed = true;

        return isIndexed;
    }
}
