package org.citydb.database.adapter.blazegraph;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.citydb.config.geometry.BoundingBox;
import org.citydb.config.geometry.GeometryObject;
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
        String connectionStr = databaseAdapter.getJDBCUrl(databaseAdapter.getConnectionDetails().getServer(), databaseAdapter.getConnectionDetails().getPort(), databaseAdapter.getConnectionDetails().getSid());

        SelectBuilder builder = new SelectBuilder();
        builder.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, schema)
                .addVar("?srid").addVar("?srsname")
                .addWhere("?s", SchemaManagerAdapter.ONTO_SRID, "?srid")
                .addWhere("?s", SchemaManagerAdapter.ONTO_SRSNAME, "?srsname");

        try (Connection conn = DriverManager.getConnection(connectionStr);
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(builder.buildString())) {
            if (rs.next()) {
                DatabaseSrs srs = metaData.getReferenceSystem();
                srs.setSrid(Integer.valueOf(rs.getString(1)));
                srs.setGMLSrsName(rs.getString(2));
                getSrsInfo(srs);
                metaData.setVersioning(DatabaseMetaData.Versioning.NOT_SUPPORTED);
            }
        }
    }

    @Override
    protected void getSrsInfo(DatabaseSrs srs, Connection connection) throws SQLException {
        getSrsInfo(srs);
    }

    @Override
    public void getSrsInfo(DatabaseSrs srs) throws SQLException {
        String connectionStr = databaseAdapter.getJDBCUrl(databaseAdapter.getConnectionDetails().getServer(), databaseAdapter.getConnectionDetails().getPort(), databaseAdapter.getConnectionDetails().getSid().replaceFirst("(namespace/)\\S*(/sparql)", "$1public$2"));
        String endpointUrl = connectionStr.substring(23, connectionStr.indexOf("&"));
        endpointUrl = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length() - 1) : endpointUrl;
        Boolean exists = existsEndpoint(endpointUrl);

        //check if public namespace exists at endpoint
        if (exists) {
            try (Connection conn = DriverManager.getConnection(connectionStr);
                 Statement statement = conn.createStatement();
                 ResultSet rs = statement.executeQuery(getGetSrsInfoSelectStatement(databaseAdapter.getConnectionDetails().getSchema(), srs.getSrid()))) {
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
                }
            }
        }

        if (!exists || (exists && !srs.isSupported())) { //public namespace is not available at endpoint or srs is not supported
            DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
            srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
            srs.setType(tmp.getType());
            srs.setSupported(false);
        }
        srsInfoMap.put(srs.getSrid(), srs);
    }

    public String getGetSrsInfoSelectStatement(String schema, int srid) {
        SelectBuilder builder = new SelectBuilder();
        ExprFactory expr = builder.getExprFactory();

        builder.addPrefix(SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML, schema)
                .addVar("?name").addVar("?type").addVar("?srtext")
                .addWhere("?s", SchemaManagerAdapter.ONTO_SRTEXT, "?srtext")
                .addBind(expr.strbefore(expr.strafter("?srtext", "\""), "\""), "?name")
                .addBind(expr.strbefore("?srtext", "["), "?type")
                .addWhere("?s", SchemaManagerAdapter.ONTO_SRID, String.valueOf(srid));

        return builder.buildString();
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

    public String getChangeSrsUpdateStatement(String schema, String endpoint, DatabaseSrs srs) {
        endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        String updateStatement = "PREFIX " + SchemaManagerAdapter.ONTO_PREFIX_NAME_ONTOCITYGML + " <" + schema + ">\n" +
                "WITH <" + endpoint + "/databasesrs/>\n" +
                "DELETE { ?srid " + SchemaManagerAdapter.ONTO_SRID + " ?currentSrid .\n" +
                "?srsname " + SchemaManagerAdapter.ONTO_SRSNAME + " ?currentSrsname }\n" +
                "INSERT { <" + endpoint + "/> " + SchemaManagerAdapter.ONTO_SRID + " " + srs.getSrid() + ";\n" +
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

        //Coordinate[] convertedCoords = converted.getCoordinates();
        //Coordinate[] reversedConvertedCoords = geospatial.reverseCoordinates(convertedCoords);


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
