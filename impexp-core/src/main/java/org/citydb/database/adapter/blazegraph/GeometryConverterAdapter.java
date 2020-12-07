package org.citydb.database.adapter.blazegraph;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractGeometryConverterAdapter;
import org.citydb.log.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

public class GeometryConverterAdapter extends AbstractGeometryConverterAdapter {

    private static final String BASE_URL_LITERALS = "http://localhost/blazegraph/literals/";
    private final Logger log = Logger.getInstance();

    protected GeometryConverterAdapter(AbstractDatabaseAdapter databaseAdapter) {
        super(databaseAdapter);
    }

    @Override
    public GeometryObject getEnvelope(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getPoint(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getMultiPoint(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getCurve(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getMultiCurve(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getPolygon(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getMultiPolygon(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public GeometryObject getGeometry(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    @Override
    public Object getDatabaseObject(GeometryObject geomObj, Connection connection) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        Node dbObject;

        try {
            String geometryType = geomObj.getGeometryType().name();
            double[][] coordinates = geomObj.getCoordinates();
            int coordLen = coordinates.length;
            int coordTotal = 0;
            String geoLiteral = "";
            StringBuilder sb = new StringBuilder(geoLiteral);

            for (int i = 0; i < coordLen; i++) {
                for (int ii = 0; ii < coordinates[i].length; ii++) {
                    sb.append(coordinates[i][ii]).append("#");
                    coordTotal++;
                }
            }

            URI datatypeURI = new URI(BASE_URL_LITERALS + geometryType + "-" + coordLen + "-" + coordTotal);
            RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
            geoLiteral = sb.toString();

            //@TODO remove in final version - for debugging purposes only
            try {
                BufferedWriter writer = new BufferedWriter(
                        new FileWriter("datatypes.txt", true));
                writer.write(geometryType + "-" + coordLen + "-" + coordTotal);
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                log.error("could not write to datatypes.txt");
            }

            dbObject = NodeFactory.createLiteral(geoLiteral.substring(0, geoLiteral.length() - 1), geoDatatype);
            makeBlazegraphGeoDatatype(dbObject);
        } catch (URISyntaxException e) {
            dbObject = NodeFactory.createBlankNode();
        }

        return dbObject;
    }

    private void makeBlazegraphGeoDatatype(Node dbObject) {
        BlazegraphGeoDatatype datatype = new BlazegraphGeoDatatype(dbObject);
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
