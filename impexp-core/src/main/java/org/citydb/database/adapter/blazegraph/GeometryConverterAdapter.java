package org.citydb.database.adapter.blazegraph;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractGeometryConverterAdapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

public class GeometryConverterAdapter extends AbstractGeometryConverterAdapter {

    private static final String BASE_URL_LITERALS = "http://localhost/blazegraph/literals/";

    protected GeometryConverterAdapter(AbstractDatabaseAdapter databaseAdapter) {
        super(databaseAdapter);
    }

    @Override
    public GeometryObject getEnvelope(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getPoint(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getMultiPoint(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getCurve(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getMultiCurve(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getPolygon(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getMultiPolygon(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public GeometryObject getGeometry(Object geomObj) throws SQLException {
        return null;
    }

    @Override
    public Object getDatabaseObject(GeometryObject geomObj, Connection connection) throws SQLException {

        Node dbObject;

        try {
            URI datatypeURI = new URI(BASE_URL_LITERALS + geomObj.getGeometryType().name());
            RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
            String geoLiteral = "";

            double[][] coordinates = geomObj.getCoordinates();

            StringBuilder sb = new StringBuilder(geoLiteral);
            for (int i = 0; i < coordinates[0].length; i++) {
                sb.append(coordinates[0][i]).append("#");
            }

            geoLiteral = sb.toString();

            dbObject = NodeFactory.createLiteral(geoLiteral.substring(0, geoLiteral.length() - 1), geoDatatype);
        } catch (URISyntaxException e) {
            dbObject = NodeFactory.createBlankNode();
        }

        return dbObject;
    }

    @Override
    public int getNullGeometryType() {
        return 0;
    }

    @Override
    public String getNullGeometryTypeName() {
        return "";
    }
}
