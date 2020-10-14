package org.citydb.database.adapter.blazegraph;

import org.apache.jena.graph.NodeFactory;
import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractGeometryConverterAdapter;

import java.sql.Connection;
import java.sql.SQLException;

public class GeometryConverterAdapter extends AbstractGeometryConverterAdapter {
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
        return NodeFactory.createBlankNode();
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
