package org.citydb.citygml.importer.database.content;


import org.citydb.citygml.common.database.uid.UIDCacheManager;
import org.citydb.citygml.common.database.xlink.DBXlink;
import org.citydb.citygml.importer.util.AffineTransformer;
import org.citydb.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.concurrent.Worker;
import org.citydb.concurrent.WorkerFactory;
import org.citydb.concurrent.WorkerPool;
import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.DatabaseAdapterFactory;
import org.citydb.database.connection.ConnectionManager;
import org.citydb.database.connection.DatabaseConnectionDetails;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.file.InputFile;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.citygml4j.builder.jaxb.CityGMLBuilderFactory;
import org.opengis.metadata.Datatype;
import org.citydb.database.connection.DatabaseMetaData;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class DBObjectTestHelper {

    // public
    public static Connection getConnection() {
        return new Connection() {
            @Override
            public Statement createStatement() throws SQLException {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql) throws SQLException {
                return null;
            }

            @Override
            public CallableStatement prepareCall(String sql) throws SQLException {
                return null;
            }

            @Override
            public String nativeSQL(String sql) throws SQLException {
                return null;
            }

            @Override
            public void setAutoCommit(boolean autoCommit) throws SQLException {

            }

            @Override
            public boolean getAutoCommit() throws SQLException {
                return false;
            }

            @Override
            public void commit() throws SQLException {

            }

            @Override
            public void rollback() throws SQLException {

            }

            @Override
            public void close() throws SQLException {

            }

            @Override
            public boolean isClosed() throws SQLException {
                return false;
            }

            @Override
            public java.sql.DatabaseMetaData getMetaData() throws SQLException {
                return null;
            }

            @Override
            public void setReadOnly(boolean readOnly) throws SQLException {

            }

            @Override
            public boolean isReadOnly() throws SQLException {
                return false;
            }

            @Override
            public void setCatalog(String catalog) throws SQLException {

            }

            @Override
            public String getCatalog() throws SQLException {
                return null;
            }

            @Override
            public void setTransactionIsolation(int level) throws SQLException {

            }

            @Override
            public int getTransactionIsolation() throws SQLException {
                return 0;
            }

            @Override
            public SQLWarning getWarnings() throws SQLException {
                return null;
            }

            @Override
            public void clearWarnings() throws SQLException {

            }

            @Override
            public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                return null;
            }

            @Override
            public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                return null;
            }

            @Override
            public Map<String, Class<?>> getTypeMap() throws SQLException {
                return null;
            }

            @Override
            public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

            }

            @Override
            public void setHoldability(int holdability) throws SQLException {

            }

            @Override
            public int getHoldability() throws SQLException {
                return 0;
            }

            @Override
            public Savepoint setSavepoint() throws SQLException {
                return null;
            }

            @Override
            public Savepoint setSavepoint(String name) throws SQLException {
                return null;
            }

            @Override
            public void rollback(Savepoint savepoint) throws SQLException {

            }

            @Override
            public void releaseSavepoint(Savepoint savepoint) throws SQLException {

            }

            @Override
            public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                return null;
            }

            @Override
            public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
                return null;
            }

            @Override
            public Clob createClob() throws SQLException {
                return null;
            }

            @Override
            public Blob createBlob() throws SQLException {
                return null;
            }

            @Override
            public NClob createNClob() throws SQLException {
                return null;
            }

            @Override
            public SQLXML createSQLXML() throws SQLException {
                return null;
            }

            @Override
            public boolean isValid(int timeout) throws SQLException {
                return false;
            }

            @Override
            public void setClientInfo(String name, String value) throws SQLClientInfoException {

            }

            @Override
            public void setClientInfo(Properties properties) throws SQLClientInfoException {

            }

            @Override
            public String getClientInfo(String name) throws SQLException {
                return null;
            }

            @Override
            public Properties getClientInfo() throws SQLException {
                return null;
            }

            @Override
            public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
                return null;
            }

            @Override
            public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
                return null;
            }

            @Override
            public void setSchema(String schema) throws SQLException {

            }

            @Override
            public String getSchema() throws SQLException {
                return null;
            }

            @Override
            public void abort(Executor executor) throws SQLException {

            }

            @Override
            public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

            }

            @Override
            public int getNetworkTimeout() throws SQLException {
                return 0;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
        };
    }

    public static Config getConfig() {
        return new Config();
    }

    public static CityGMLBuilder getCityGMLBuilder() throws CityGMLBuilderException {
        
        return CityGMLBuilderFactory.defaults().build();
    }

    public static InputFile getInputFile(){
        return null;
    }

    public static DatabaseMetaData getDatabaseMetaData(DatabaseConnectionDetails connectionDetails) {
        return new DatabaseMetaData(connectionDetails);
    }

    public static AbstractDatabaseAdapter getAbstractDatabaseAdapter(){

        // setup the connection and set the connection details
        DatabaseType blaze = DatabaseType.fromValue("Blazegraph");
        AbstractDatabaseAdapter abstractDatabaseAdapter = DatabaseAdapterFactory.getInstance().createDatabaseAdapter(blaze);

        DBConnection connection = new DBConnection();
        connection.setDatabaseType(blaze);
        DatabaseConnectionDetails connectionDetails = new DatabaseConnectionDetails(connection);
        abstractDatabaseAdapter.setConnectionDetails(connectionDetails);
        DatabaseMetaData metaData = getDatabaseMetaData(connectionDetails);
        abstractDatabaseAdapter.setConnectionMetaData(metaData);   // cause problem in GeometryConverter.java
        return abstractDatabaseAdapter;
    }
    
    public static SchemaMapping getSchemapping(){
        return new SchemaMapping();
    }

    public static WorkerPool<DBXlink> getWorkerPool(){
        WorkerFactory<DBXlink> workerFactory = new WorkerFactory() {
            @Override
            public Worker createWorker() {
                return null;
            }
        };
        PoolSizeAdaptationStrategy poolSizeAdaptationStrategy = null;
        return new WorkerPool("dbxlink", 1, 2, poolSizeAdaptationStrategy, workerFactory, 1);
    }

    public static CityGMLImportManager getCityGMLImportManager() throws Exception {
        
        InputFile inputFile = getInputFile();
        Connection connection = getConnection();
        AbstractDatabaseAdapter abstractDatabaseAdapter = getAbstractDatabaseAdapter();
        SchemaMapping schemaMapping = getSchemapping();
        CityGMLBuilder cityGMLBuilder = getCityGMLBuilder();
        WorkerPool<DBXlink> dbXlinkWorkerPool = getWorkerPool();
        UIDCacheManager uidCacheManager = new UIDCacheManager();
        Config config = getConfig();
        AffineTransformer affineTransformer = new AffineTransformer(config);
        return new CityGMLImportManager(inputFile, connection, abstractDatabaseAdapter, schemaMapping, cityGMLBuilder, dbXlinkWorkerPool, uidCacheManager, affineTransformer, config);
    }
}