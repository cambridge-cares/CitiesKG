package org.citydb.database.adapter.blazegraph;

import org.citydb.config.project.database.DatabaseType;
import org.citydb.database.adapter.AbstractDatabaseAdapter;

public class BlazegraphAdapter extends AbstractDatabaseAdapter {

    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    public String getConnectionFactoryClassName() {
        return null;
    }

    @Override
    public String getJDBCUrl(String server, int port, String database) {
        return null;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return null;
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