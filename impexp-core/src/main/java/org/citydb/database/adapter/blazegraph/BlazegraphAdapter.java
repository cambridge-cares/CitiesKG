package org.citydb.database.adapter.blazegraph;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;


import java.net.URI;
import java.net.URISyntaxException;

public class BlazegraphAdapter extends AbstractDatabaseAdapter {

    private final static String PROTOCOL = "http";
    private final static String DRIVER_JENA_CLASS = "org.apache.jena.jdbc.JenaDriver";
    private final static String DRIVER_JENA_REMOTE_ENDPOINT = "jdbc:jena:remote:";
    private final static String SPARQL_REMOTE_QUERY = "query=";
    private final static String SPARQL_REMOTE_UPDATE = "update=";

    public BlazegraphAdapter() {
        schemaAdapter = new SchemaManagerAdapter(this);
    }

    @Override
    public int getDefaultPort() {
        return 9999;
    }

    @Override
    public String getConnectionFactoryClassName() {
        return DRIVER_JENA_CLASS;
    }

    @Override
    public String getJDBCUrl(String server, int port, String database) {

        String JDBCUrl;
        try {
            String endpoint = new URI(PROTOCOL + "://" + server + ":" + port + database).toString();
            JDBCUrl = DRIVER_JENA_REMOTE_ENDPOINT + SPARQL_REMOTE_QUERY + endpoint + "&" + SPARQL_REMOTE_UPDATE + endpoint;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        return JDBCUrl;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.BLAZE;
    }

    @Override
    public boolean hasVersioningSupport() {
        return false;
    }

    @Override
    public boolean hasTableStatsSupport() {
        return false;
    }

    @Override
    public int getMaxBatchSize() {
        return 0;
    }
}