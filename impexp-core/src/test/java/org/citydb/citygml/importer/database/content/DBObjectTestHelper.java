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
        return null;
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