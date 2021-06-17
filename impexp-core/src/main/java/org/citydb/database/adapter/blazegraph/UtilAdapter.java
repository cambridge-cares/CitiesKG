package org.citydb.database.adapter.blazegraph;

import org.citydb.config.geometry.BoundingBox;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractUtilAdapter;
import org.citydb.database.adapter.IndexStatusInfo;
import org.citydb.database.connection.DatabaseMetaData;
import org.citydb.database.version.DatabaseVersion;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    //The postgis implementation is to execute ST_Transform in postgis
    @Override
    protected GeometryObject transform(GeometryObject geometry, DatabaseSrs targetSrs, Connection connection) throws SQLException {
        GeometryObject result = null;

        Object unconverted = databaseAdapter.getGeometryConverter().getDatabaseObject(geometry, connection);
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();
        List<Coordinate> coords = new ArrayList<>();
        double[] coordlist = geometry.getCoordinates(0);
        for (int i  = 0; i < coordlist.length; i = i+2){
            Coordinate cor = new Coordinate(coordlist[i], coordlist[i+1]);
            coords.add(cor);
        }
        GeometryFactory fac = new GeometryFactory();
        Geometry geom = fac.createPolygon(coords.toArray(new Coordinate[0]));

        int sourceSrsId;
        if (geometry.getSrid() == 0){
            sourceSrsId = 31466;
        }else{
            sourceSrsId = geometry.getSrid();
        }

        Geometry converted = geospatial.Transform(geom,28992, targetSrs.getSrid());

        // need to reverse the coordinates to match POSTGIS results
        Coordinate[] reverseCoord = geospatial.getReversedCoordinates(converted);
        Geometry reverseConverted = fac.createPolygon(reverseCoord);
        result = databaseAdapter.getGeometryConverter().getGeometry(reverseConverted);
        return result;
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
