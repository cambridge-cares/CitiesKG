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

/**
 * GeometryConverterAdapter implementation for Blazegraph.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class GeometryConverterAdapter extends AbstractGeometryConverterAdapter {

    private static final String BASE_URL_LITERALS = "http://localhost/blazegraph/literals/";


    /**
     * To be used with: {@link BlazegraphAdapter}
     *
     * @param databaseAdapter current database adapter
     */
    protected GeometryConverterAdapter(AbstractDatabaseAdapter databaseAdapter) {
        super(databaseAdapter);
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getEnvelope(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getPoint(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getMultiPoint(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getCurve(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getMultiCurve(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getPolygon(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getMultiPolygon(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Stub method, does nothing for now.
     *
     * @param geomObj geometry object
     * @return nothing
     * @throws SQLException given null argument
     */
    @Override
    public GeometryObject getGeometry(Object geomObj) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        return null;
    }

    /**
     * Turns {@link GeometryObject} to a geo-literal {@link Node}.
     * Used by: {@link org.citydb.citygml.importer.database.content} objects to build SPARQL statements.
     *
     * @param geomObj geometry object to be converted to a geo-literal {@link Node}
     * @param connection current database connection
     * @return a geo-literal {@link Node}
     * @throws SQLException when URI for a geo-literal is invalid.
     */
    @Override
    public Object getDatabaseObject(GeometryObject geomObj, Connection connection) throws SQLException {
        if (geomObj == null) {
            throw new SQLException();
        }
        Node dbObject;

        try {
            String geometryType = geomObj.getGeometryType().name();
            double[][] coordinates = geomObj.getCoordinates();
            String coordLen = String.valueOf(geomObj.getDimension());
            String geoLiteral = "";
            StringBuilder sb = new StringBuilder(geoLiteral);

            for (double[] coordinate : coordinates) {
                coordLen = coordLen + "-" + coordinate.length;
                for (double v : coordinate) {
                    sb.append(v).append("#");
                }
            }

            URI datatypeURI = new URI(BASE_URL_LITERALS + geometryType + "-" + coordLen);
            RDFDatatype geoDatatype = new BaseDatatype(datatypeURI.toString());
            geoLiteral = sb.toString();
            dbObject = NodeFactory.createLiteral(geoLiteral.substring(0, geoLiteral.length() - 1), geoDatatype);

            makeBlazegraphGeoDatatype(dbObject);
        } catch (URISyntaxException e) {
            dbObject = NodeFactory.createBlankNode();
        }

        return dbObject;
    }

    /**
     * Makes {@link BlazegraphGeoDatatype} out of a geo-literal {@link Node} made from {@link GeometryObject}
     * and adds it to an instance of {@link BlazegraphConfigBuilder} together with corresponding URI string.
     *
     * @param dbObject a geo-literal {@link Node} made from {@link GeometryObject}
     */
    private void makeBlazegraphGeoDatatype(Node dbObject) {
        BlazegraphGeoDatatype datatype = new BlazegraphGeoDatatype(dbObject);
        BlazegraphConfigBuilder.getInstance()
                .addGeoDataType(datatype.getGeodatatype())
                .addURIString(dbObject.getLiteral().getDatatypeURI());
    }

    /**
     * Returns null geometry type.
     *
     * @return 0
     */
    @Override
    public int getNullGeometryType() {
        return 0;
    }

    /**
     * Returns null geometry type name.
     *
     * @return empty string.
     */
    @Override
    public String getNullGeometryTypeName() {
        return "";
    }
}
