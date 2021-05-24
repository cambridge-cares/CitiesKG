package org.citydb.citygml.importer.database.content;


import org.citydb.citygml.common.database.uid.UIDCacheManager;
import org.citydb.citygml.common.database.xlink.DBXlink;
import org.citydb.citygml.importer.util.AffineTransformer;
import org.citydb.concurrent.WorkerFactory;
import org.citydb.concurrent.WorkerPool;
import org.citydb.config.Config;
import org.citydb.config.project.database.DBConnection;
import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.DatabaseAdapterFactory;
import org.citydb.database.connection.DatabaseConnectionDetails;
import org.citydb.database.connection.DatabaseConnectionPool;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.database.version.DatabaseVersion;
import org.citydb.event.EventDispatcher;
import org.citydb.file.InputFile;
import org.citydb.registry.ObjectRegistry;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.citygml4j.builder.jaxb.CityGMLBuilderFactory;
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
            public Statement createStatement() {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql) {
                return null;
            }

            @Override
            public CallableStatement prepareCall(String sql) {
                return null;
            }

            @Override
            public String nativeSQL(String sql) {
                return null;
            }

            @Override
            public void setAutoCommit(boolean autoCommit) {

            }

            @Override
            public boolean getAutoCommit() {
                return false;
            }

            @Override
            public void commit() {

            }

            @Override
            public void rollback() {

            }

            @Override
            public void close() {

            }

            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public java.sql.DatabaseMetaData getMetaData() {
                return null;
            }

            @Override
            public void setReadOnly(boolean readOnly) {

            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public void setCatalog(String catalog) {

            }

            @Override
            public String getCatalog() {
                return null;
            }

            @Override
            public void setTransactionIsolation(int level)  {

            }

            @Override
            public int getTransactionIsolation() {
                return Connection.TRANSACTION_NONE;
            }

            @Override
            public SQLWarning getWarnings() {
                return null;
            }

            @Override
            public void clearWarnings() {

            }

            @Override
            public Statement createStatement(int resultSetType, int resultSetConcurrency) {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
                return null;
            }

            @Override
            public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
                return null;
            }

            @Override
            public Map<String, Class<?>> getTypeMap() {
                return null;
            }

            @Override
            public void setTypeMap(Map<String, Class<?>> map) {

            }

            @Override
            public void setHoldability(int holdability) {

            }

            @Override
            public int getHoldability() {
                return 0;
            }

            @Override
            public Savepoint setSavepoint() {
                return null;
            }

            @Override
            public Savepoint setSavepoint(String name) {
                return null;
            }

            @Override
            public void rollback(Savepoint savepoint) {

            }

            @Override
            public void releaseSavepoint(Savepoint savepoint) {

            }

            @Override
            public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
                return null;
            }

            @Override
            public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, int[] columnIndexes) {
                return null;
            }

            @Override
            public PreparedStatement prepareStatement(String sql, String[] columnNames) {
                return null;
            }

            @Override
            public Clob createClob() {
                return null;
            }

            @Override
            public Blob createBlob() {
                return null;
            }

            @Override
            public NClob createNClob() {
                return null;
            }

            @Override
            public SQLXML createSQLXML() {
                return null;
            }

            @Override
            public boolean isValid(int timeout) {
                return false;
            }

            @Override
            public void setClientInfo(String name, String value) {

            }

            @Override
            public void setClientInfo(Properties properties) {

            }

            @Override
            public String getClientInfo(String name) {
                return null;
            }

            @Override
            public Properties getClientInfo() {
                return null;
            }

            @Override
            public Array createArrayOf(String typeName, Object[] elements) {
                return null;
            }

            @Override
            public Struct createStruct(String typeName, Object[] attributes) {
                return null;
            }

            @Override
            public void setSchema(String schema) {

            }

            @Override
            public String getSchema() {
                return null;
            }

            @Override
            public void abort(Executor executor) {

            }

            @Override
            public void setNetworkTimeout(Executor executor, int milliseconds) {

            }

            @Override
            public int getNetworkTimeout() {
                return 0;
            }

            @Override
            public <T> T unwrap(Class<T> iface) {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }
        };
    }

    public static Config getConfig() {

        Config config = new Config();
        DBConnection dbConnection = DBObjectTestHelper.getDBConnection();
        config.getProject().getDatabase().setActiveConnection(dbConnection);

        return config;
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

    public static DBConnection getDBConnection(){
        return createDbConnection("Blazegraph");
    }

    public static DBConnection createDbConnection(String type) {
        DatabaseType dbType = DatabaseType.fromValue(type);
        DBConnection connection = new DBConnection();
        connection.setDatabaseType(dbType);
        connection.setUser("anonymous");
        connection.setInternalPassword("anonymous");
        connection.setServer("127.0.0.1");
        connection.setPort(9999);
        connection.setSid("/blazegraph/namespace/berlin/sparql");
        return connection;
    }

    public static ObjectRegistry getObjectRegistry(){
        // Note: ObjectRegistry class has a static and synchronized method for shared resource.
        // EventDispatchner only need to be registered once.

        ObjectRegistry objectRegistry = ObjectRegistry.getInstance();
        if (objectRegistry.getEventDispatcher() == null) {
            EventDispatcher eventDispatcher = new EventDispatcher();
            objectRegistry.setEventDispatcher(eventDispatcher);
        }
        return objectRegistry;
    }


    public static AbstractDatabaseAdapter createAbstractDatabaseAdapter(String DbType){
        // setup the connection and set the connection details
        DatabaseType blaze = DatabaseType.fromValue(DbType);
        AbstractDatabaseAdapter abstractDatabaseAdapter = DatabaseAdapterFactory.getInstance().createDatabaseAdapter(blaze);

        DBConnection connection = createDbConnection(DbType);
        DatabaseConnectionDetails connectionDetails = new DatabaseConnectionDetails(connection);
        abstractDatabaseAdapter.setConnectionDetails(connectionDetails);
        DatabaseMetaData metaData = getDatabaseMetaData(connectionDetails);

        // setup database version for DBAddress
        DatabaseVersion databaseVersion = new DatabaseVersion(4,0,0,"4.0x");
        metaData.setCityDBVersion(databaseVersion);

        abstractDatabaseAdapter.setConnectionMetaData(metaData);   // cause problem in GeometryConverter.java
        return abstractDatabaseAdapter;
    }
    
    public static SchemaMapping getSchemapping(){
        return new SchemaMapping();
    }

    public static WorkerPool<DBXlink> getWorkerPool(){

        WorkerFactory<DBXlink> workerFactory = () -> null;

        return new WorkerPool<>("dbxlink", 1, 2, null, workerFactory, 1);
    }

    public static CityGMLImportManager getCityGMLImportManager() throws Exception {
        return createCityGMLImportManager("Blazegraph");
    }

    public static CityGMLImportManager getCityGMLImportManagerPostGis() throws Exception {
        return createCityGMLImportManager("PostGIS");
    }

    public static CityGMLImportManager createCityGMLImportManager(String DbType) throws Exception {
        InputFile inputFile = getInputFile();
        Connection connection = getConnection();
        AbstractDatabaseAdapter abstractDatabaseAdapter = createAbstractDatabaseAdapter(DbType);
        SchemaMapping schemaMapping = getSchemapping();
        CityGMLBuilder cityGMLBuilder = getCityGMLBuilder();
        WorkerPool<DBXlink> dbXlinkWorkerPool = getWorkerPool();
        UIDCacheManager uidCacheManager = new UIDCacheManager();
        Config config = getConfig();
        AffineTransformer affineTransformer = new AffineTransformer(config);
        return new CityGMLImportManager(inputFile, connection,
                abstractDatabaseAdapter, schemaMapping, cityGMLBuilder, dbXlinkWorkerPool, uidCacheManager,
                affineTransformer, config);
    }

    public static DatabaseConnectionPool getDatabaseConnectionPool(){
        return DatabaseConnectionPool.getInstance();
    }
}