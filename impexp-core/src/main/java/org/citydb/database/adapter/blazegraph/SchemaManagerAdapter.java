package org.citydb.database.adapter.blazegraph;

import org.apache.commons.lang3.StringUtils;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractSchemaManagerAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SchemaManagerAdapter extends AbstractSchemaManagerAdapter {

    private static final String SCHEMA_DEFAULT = "ontocitygml";
    public static final String ONTO_PREFIX_NAME_ONTOCITYGML = "ocgml:";
    public static final String ONTO_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "id";
    public static final String ONTO_OBJECT_CLASS_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "objectClassId";
    public static final String ONTO_GML_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "gmlId";
    public static final String ONTO_PARENT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "parentId";
    public static final String ONTO_ROOT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "rootId";
    public static final String ONTO_CITY_OBJECT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "cityObjectId";
    public static final String ONTO_NAME = ONTO_PREFIX_NAME_ONTOCITYGML + "name";
    public static final String ONTO_NAME_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "nameCodespace";
    public static final String ONTO_DESCRIPTION = ONTO_PREFIX_NAME_ONTOCITYGML + "description";
    public static final String ONTO_ENVELOPE_TYPE = ONTO_PREFIX_NAME_ONTOCITYGML + "EnvelopeType";
    public static final String ONTO_CREATION_DATE = ONTO_PREFIX_NAME_ONTOCITYGML + "creationDate";
    public static final String ONTO_TERMINATION_DATE = ONTO_PREFIX_NAME_ONTOCITYGML + "terminationDate";
    public static final String ONTO_RELATIVE_TO_TERRAIN = ONTO_PREFIX_NAME_ONTOCITYGML + "relativeToTerrain";
    public static final String ONTO_RELATIVE_TO_WATER = ONTO_PREFIX_NAME_ONTOCITYGML + "relativeToWater";
    public static final String ONTO_LAST_MODIFICATION_DATE = ONTO_PREFIX_NAME_ONTOCITYGML + "lastModificationDate";
    public static final String ONTO_UPDATING_PERSON = ONTO_PREFIX_NAME_ONTOCITYGML + "updatingPerson";
    public static final String ONTO_REASON_FOR_UPDATE = ONTO_PREFIX_NAME_ONTOCITYGML + "reasonForUpdate";
    public static final String ONTO_LINEAGE = ONTO_PREFIX_NAME_ONTOCITYGML + "lineage";
    public static final String ONTO_GEOMETRY = ONTO_PREFIX_NAME_ONTOCITYGML + "GeometryType";
    public static final String ONTO_GEOMETRY_SOLID = ONTO_PREFIX_NAME_ONTOCITYGML + "SolidType";
    public static final String ONTO_GEOMETRY_IMPLICIT = ONTO_PREFIX_NAME_ONTOCITYGML + "ImplicitGeometryType";
    public static final String ONTO_IS_TRIANGULATED = ONTO_PREFIX_NAME_ONTOCITYGML + "isTriangulated";
    public static final String ONTO_IS_XLINK = ONTO_PREFIX_NAME_ONTOCITYGML + "isXlink";
    public static final String ONTO_IS_REVERSE = ONTO_PREFIX_NAME_ONTOCITYGML + "isReverse";
    public static final String ONTO_IS_SOLID = ONTO_PREFIX_NAME_ONTOCITYGML + "isSolid";
    public static final String ONTO_IS_COMPOSITE = ONTO_PREFIX_NAME_ONTOCITYGML + "isComposite";

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
