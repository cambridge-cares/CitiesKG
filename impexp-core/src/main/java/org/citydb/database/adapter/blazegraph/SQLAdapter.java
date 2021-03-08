package org.citydb.database.adapter.blazegraph;

import org.citydb.config.geometry.GeometryObject;
import org.citydb.database.adapter.*;
import org.citydb.query.filter.selection.operator.spatial.SpatialOperatorName;
import org.citydb.sqlbuilder.SQLStatement;
import org.citydb.sqlbuilder.schema.Column;
import org.citydb.sqlbuilder.select.PredicateToken;
import org.citydb.sqlbuilder.select.projection.Function;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLAdapter extends AbstractSQLAdapter {

    protected SQLAdapter(AbstractDatabaseAdapter databaseAdapter) {
        super(databaseAdapter);
    }

    @Override
    public String getInteger() {
        return null;
    }

    @Override
    public String getSmallInt() {
        return null;
    }

    @Override
    public String getBigInt() {
        return null;
    }

    @Override
    public String getNumeric() {
        return null;
    }

    @Override
    public String getNumeric(int precision) {
        return null;
    }

    @Override
    public String getNumeric(int precision, int scale) {
        return null;
    }

    @Override
    public String getReal() {
        return null;
    }

    @Override
    public String getDoublePrecision() {
        return null;
    }

    @Override
    public String getCharacter(int nrOfChars) {
        return null;
    }

    @Override
    public String getCharacterVarying(int nrOfChars) {
        return null;
    }

    @Override
    public String getPolygon2D() {
        return null;
    }

    @Override
    public String getCreateUnloggedTable(String tableName, String columns) {
        return null;
    }

    @Override
    public String getCreateUnloggedTableAsSelect(String tableName, String select) {
        return null;
    }

    @Override
    public String getUnloggedIndexProperty() {
        return null;
    }

    @Override
    public boolean requiresPseudoTableInSelect() {
        return false;
    }

    @Override
    public String getPseudoTableName() {
        return null;
    }

    @Override
    public boolean spatialPredicateRequiresNoIndexHint() {
        return false;
    }

    @Override
    public boolean supportsFetchFirstClause() {
        return false;
    }

    @Override
    public String getHierarchicalGeometryQuery() {
        return null;
    }

    @Override
    public String getNextSequenceValue(String sequence) {
        return null;
    }

    @Override
    public String getCurrentSequenceValue(String sequence) {
        return null;
    }

    @Override
    public String getNextSequenceValuesQuery(String sequence) {
        return null;
    }

    @Override
    public int getMaximumNumberOfItemsForInOperator() {
        return 0;
    }

    @Override
    public PredicateToken getBinarySpatialPredicate(SpatialOperatorName operator, Column targetColumn, GeometryObject geometry, boolean negate) {
        return null;
    }

    @Override
    public PredicateToken getDistancePredicate(SpatialOperatorName operator, Column targetColumn, GeometryObject geometry, double distance, boolean negate) {
        return null;
    }

    @Override
    public Function getAggregateExtentFunction(Column envelope) {
        return null;
    }

    @Override
    public BlobImportAdapter getBlobImportAdapter(Connection connection, BlobType type) throws SQLException {
        return null;
    }

    @Override
    public BlobExportAdapter getBlobExportAdapter(Connection connection, BlobType type) {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(SQLStatement sqlstatement, Connection connection) throws SQLException {
        // @TODO: implement SQLStatement into SPARQLStatement translator
        String sparql = sql2sparqlStatement.transformer1(sqlstatement);
        PreparedStatement preparedStatement = connection.prepareStatement(sparql);  // same as importer

        //PreparedStatement preparedStatement = connection.prepareStatement(statement.toString());
        //fillPlaceHolders(statement, preparedStatement, connection);   // this will find placeholder"?" symbol and replace it with corresponding ID

        return preparedStatement;
    }
}
