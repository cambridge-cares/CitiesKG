package org.citydb.database.adapter.blazegraph;

import java.util.Arrays;
import org.citydb.citygml.exporter.util.Metadata;
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
import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
        getSrsInfo(srs);
    }

    @Override
    public void getSrsInfo(DatabaseSrs srs) throws SQLException {
        String connectionStr = databaseAdapter.getJDBCUrl(databaseAdapter.getConnectionDetails().getServer(), databaseAdapter.getConnectionDetails().getPort(), databaseAdapter.getConnectionDetails().getSid());
        //extract the endpoint url of public namespace
        //remove / at the end of endpoint if any - if endpoint ends with /, existsEndpoint will fail even if public namespace exists
        String endpointUrl = connectionStr.substring(23, connectionStr.indexOf("&"));
        if (endpointUrl.endsWith("/")) {
            endpointUrl = endpointUrl.substring(0, endpointUrl.length() - 1);
        }
        String schema = databaseAdapter.getConnectionDetails().getSchema();
        StringBuilder sparqlString = new StringBuilder();

        sparqlString.append("PREFIX ocgml: <" + schema + "> " +
                "SELECT ?s ?srid ?srsname {\n" +
                "    ?s ocgml:srid ?srid;\n" +
                "        ocgml:srsname ?srsname\n" +
                "}");

        String query = sparqlString.toString();

        //check if public namespace exists at endpoint
        Boolean exists = existsEndpoint(endpointUrl);

        if (exists) {
            try (Connection conn = DriverManager.getConnection(connectionStr);
                 PreparedStatement statement = conn.prepareStatement(query);
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    srs.setSupported(true);
                    if ((rs.getString(1) == null) && (rs.getString(2) == null)) { //srs is supported but no wktext
                        srs.setDatabaseSrsName("");
                        srs.setType(getSrsType(""));
                        srs.setWkText("");
                    } else { //srs is supported and has wktext
                        srs.setGMLSrsName(rs.getString(1));
                        srs.setSrid(rs.getInt(2));
                        srs.setDatabaseSrsName(rs.getString(3));
                        srs.setDescription(rs.getString(3));
                        databaseAdapter.getConnectionMetaData().setReferenceSystem(srs);
                    }
                } else { //srs is not supported
                    DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
                    srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
                    srs.setType(tmp.getType());
                    srs.setSupported(false);
                }
            }
        } else { //public namespace is not available at endpoint
            DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
            srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
            srs.setType(tmp.getType());
            srs.setSupported(false);
        }
        srsInfoMap.put(srs.getSrid(), srs);
    }

    public Boolean existsEndpoint(String endpointUrl) {
        Boolean exists = true;
        try {
            URL url = new URL(endpointUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                exists = false;
            }
        } catch (IOException e) {
            exists = false;
        }
        return exists;
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
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        String updateStatement = "PREFIX " + SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + " <" + schema + ">\n" +
                "WITH <" + endpoint + "/databasesrs/>\n" +
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
