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

    @Override
    protected BoundingBox transformBoundingBox(BoundingBox bbox, DatabaseSrs sourceSrs, DatabaseSrs targetSrs, Connection connection) throws SQLException {
        return null;
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
        int srcSrs = geometry.getSrid();

        GeometryFactory fac = new GeometryFactory();  // no polygonZ
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        GeometryBuilder geometrybuilder = new GeometryBuilder();
        List<Geometry> polygonlist = new ArrayList<>();

        //for (int i = 0; i < numGeometry; ++i){
        //    coordinates[i] = geometry.getCoordinates(i);
        //}

        for (int j = 0; j < numGeometry; ++j) {
            List<Coordinate> polygoncoord = new ArrayList<>();
            for (int k = 0; k < coordinates[j].length; k = k + dim){
                if (dim == 2) {
                    polygoncoord.add(new Coordinate(coordinates[j][k], coordinates[j][k+1]));  // Things go wrong here
                } else {
                    polygoncoord.add(new Coordinate(coordinates[j][k], coordinates[j][k+1], coordinates[j][k+2]));
                }

            }
            polygonlist.add(fac.createPolygon(polygoncoord.toArray(polygoncoord.toArray(new Coordinate[0]))));
        }

        // only GeoSpatialProcessor has Transform
        Geometry coll = fac.buildGeometry(polygonlist);
        Geometry converted = geospatial.Transform(coll, srcSrs , targetSrs.getSrid());
        //List<Geometry> convertedGeometry = new ArrayList<>();
        //for (int i = 0; i < numGeometry; ++i){
        //    Geometry converted = geospatial.Transform(polygonlist.get(i), geometry.getSrid(), targetSrs.getSrid()); // the hague: 28992, berlin: 25933 / 25833
        //    Coordinate[] reverseCoord = geospatial.getReversedCoordinates(converted);
        //    Geometry reverseConverted = fac.createPolygon(reverseCoord);
        //    convertedGeometry.add(reverseConverted);
        //}

        // need to reverse the coordinates to match POSTGIS results --> move to somewhere
        //Geometry union = geospatial.UnaryUnion(convertedGeometry);

        GeometryObject geomObj2d = databaseAdapter.getGeometryConverter().getGeometry(converted);
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
