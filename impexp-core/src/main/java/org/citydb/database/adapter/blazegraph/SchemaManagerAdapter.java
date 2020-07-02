package org.citydb.database.adapter.blazegraph;

import org.apache.commons.lang3.StringUtils;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractSchemaManagerAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SchemaManagerAdapter extends AbstractSchemaManagerAdapter {

    private static final String SCHEMA_DEFAULT = "ontocitygml";

    protected SchemaManagerAdapter(AbstractDatabaseAdapter databaseAdapter) {
        super(databaseAdapter);
    }

    @Override
    public String getDefaultSchema() {
        return SCHEMA_DEFAULT;
    }

    @Override
    public boolean equalsDefaultSchema(String schema) {
        return (StringUtils.isEmpty(schema) || SCHEMA_DEFAULT.equals(schema.trim()));
    }

    @Override
    public boolean existsSchema(Connection connection, String schema) {
        return false;
    }

    @Override
    public List<String> fetchSchemasFromDatabase(Connection connection) throws SQLException {
        return null;
    }

    @Override
    public String formatSchema(String schema) {
        return null;
    }
}
