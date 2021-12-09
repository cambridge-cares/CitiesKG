package org.citydb.database.adapter.blazegraph;

import org.citydb.config.geometry.BoundingBox;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.config.geometry.GeometryType;
import org.citydb.config.geometry.MultiPolygon;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.config.project.database.DatabaseSrsType;
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
        getSrsInfo(srs);
    }

    @Override
    public void getSrsInfo(DatabaseSrs srs) throws SQLException {
        String connectionStr = databaseAdapter.getJDBCUrl(databaseAdapter.getConnectionDetails().getServer(), databaseAdapter.getConnectionDetails().getPort(), databaseAdapter.getConnectionDetails().getSid().replaceFirst("(namespace/)\\S*(/sparql)", "$1public$2"));
        Connection conn = DriverManager.getConnection(connectionStr);

        String schema = databaseAdapter.getConnectionDetails().getSchema();

        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("PREFIX " + SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + " <" + schema + ">\n" +
                     "SELECT ?name ?type ?srtext WHERE {\n" +
                     "        ?s " + SchemaManagerAdapter.ONTO_SRTEXT + " ?srtext\n" +
                     "        BIND(strbefore((strafter(?srtext, \"\\\"\")), \"\\\"\") as ?name)\n" +
                     "        BIND(strbefore(?srtext, \"[\") as ?type)\n" +
                     "        ?s " + SchemaManagerAdapter.ONTO_SRID + " " + srs.getSrid() +
                     "}")) {

            if (rs.next()) {
                srs.setSupported(true);
                if ((rs.getString(1) == null) && (rs.getString(2) == null)) { //srs is supported but no wktext
                    srs.setDatabaseSrsName("");
                    srs.setType(getSrsType(""));
                    srs.setWkText("");
                } else { //srs is supported and has wktext
                    srs.setDatabaseSrsName(rs.getString(1));
                    srs.setType(getSrsType(rs.getString(2)));
                    srs.setWkText(rs.getString(3));
                }
            } else { //srs is not supported
                DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
                srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
                srs.setType(tmp.getType());
                srs.setSupported(false);
            }
        } catch (Exception e) {
            DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
            srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
            srs.setType(tmp.getType());
            srs.setSupported(false);
        }
        srsInfoMap.put(srs.getSrid(), srs);
    }

    @Override
    public void changeSrs(DatabaseSrs srs, boolean doTransform, String schema) throws SQLException {
        String connectionStr = databaseAdapter.getJDBCUrl(databaseAdapter.getConnectionDetails().getServer(), databaseAdapter.getConnectionDetails().getPort(), databaseAdapter.getConnectionDetails().getSid());
        Connection connection = DriverManager.getConnection(connectionStr);
        changeSrs(srs, doTransform, schema, connection);
    }

    //the postgis implementation is to call citydb_pkg.change_schema_srid
    @Override
    protected void changeSrs(DatabaseSrs srs, boolean doTransform, String schema, Connection connection) throws SQLException {
        DatabaseSrs checkSrs = DatabaseSrs.createDefaultSrs();
        checkSrs.setSrid(srs.getSrid());
        getSrsInfo(checkSrs);

        String endpoint = "http://".concat(databaseAdapter.getConnectionDetails().getServer()).concat(":").concat(String.valueOf(databaseAdapter.getConnectionDetails().getPort())).concat(databaseAdapter.getConnectionDetails().getSid());

        if (checkSrs.isSupported()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(getChangeSrsUpdateStatement(schema, endpoint, srs));
            }
        } else {
            throw new SQLException("Graph spatialrefsys does not contain the SRID " + srs.getSrid() + ". Insert commands for missing SRIDs can be found at spatialreference.org");
        }
    }

    protected String getChangeSrsUpdateStatement(String schema, String endpoint, DatabaseSrs srs) {
        String updateStatement = "PREFIX " + SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + " <" + schema + ">\n" +
                "WITH <" + endpoint + "databasesrs/>\n" +
                "DELETE { ?srid " + SchemaManagerAdapter.ONTO_SRID + " ?currentSrid .\n" +
                "?srsname " + SchemaManagerAdapter.ONTO_SRSNAME + " ?currentSrsname }\n" +
                "INSERT { <" + endpoint + "> " + SchemaManagerAdapter.ONTO_SRID + " " + srs.getSrid() + ";\n" +
                SchemaManagerAdapter.ONTO_SRSNAME + " \"" + srs.getGMLSrsName() + "\" }\n" +
                "WHERE { OPTIONAL { ?srid " + SchemaManagerAdapter.ONTO_SRID + " ?currentSrid }\n" +
                "OPTIONAL { ?srsname " + SchemaManagerAdapter.ONTO_SRSNAME + " ?currentSrsname } }";
        return updateStatement;
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

        int numPolygon = geometry.getNumElements();
        double[][] coordinates = new double[numPolygon][];
        GeometryFactory fac = new GeometryFactory();
        GeoSpatialProcessor geospatial = new GeoSpatialProcessor();

        List<Geometry> polygonlist = new ArrayList<>();

        for (int i = 0; i < numPolygon; ++i){
            coordinates[i] = geometry.getCoordinates(i);
        }

        for (int j = 0; j < numPolygon; ++j) {
            List<Coordinate> polygoncoord = new ArrayList<>();
            for (int k = 0; k < coordinates[j].length; k = k + 2){
                polygoncoord.add(new Coordinate(coordinates[j][k], coordinates[j][k+1]));
            }
            polygonlist.add(fac.createPolygon(polygoncoord.toArray(polygoncoord.toArray(new Coordinate[0]))));
        }

        int sourceSrsId;
        if (geometry.getSrid() == 0){
            sourceSrsId = 31466;
        }else{
            sourceSrsId = geometry.getSrid();
        }

        List<Geometry> convertedGeometry = new ArrayList<>();
        for (int i = 0; i < numPolygon; ++i){
            Geometry converted = geospatial.Transform(polygonlist.get(i),25833, targetSrs.getSrid()); // the hague: 28992, berlin: 25933
            Coordinate[] reverseCoord = geospatial.getReversedCoordinates(converted);
            Geometry reverseConverted = fac.createPolygon(reverseCoord);
            convertedGeometry.add(reverseConverted);
        }

        // need to reverse the coordinates to match POSTGIS results
        Geometry union = geospatial.UnaryUnion(convertedGeometry);
        result = databaseAdapter.getGeometryConverter().getGeometry(union);
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

    private DatabaseSrsType getSrsType(String srsType) {
        if ("PROJCS".equals(srsType))
            return DatabaseSrsType.PROJECTED;
        else if ("GEOGCS".equals(srsType))
            return DatabaseSrsType.GEOGRAPHIC2D;
        else if ("GEOCCS".equals(srsType))
            return DatabaseSrsType.GEOCENTRIC;
        else if ("VERT_CS".equals(srsType))
            return DatabaseSrsType.VERTICAL;
        else if ("LOCAL_CS".equals(srsType))
            return DatabaseSrsType.ENGINEERING;
        else if ("COMPD_CS".equals(srsType))
            return DatabaseSrsType.COMPOUND;
        else if ("GEOGCS3D".equals(srsType))
            return DatabaseSrsType.GEOGRAPHIC3D;
        else
            return DatabaseSrsType.UNKNOWN;
    }
}
