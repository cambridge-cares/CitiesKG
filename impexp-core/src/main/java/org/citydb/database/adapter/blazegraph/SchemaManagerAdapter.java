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
    public static final String ONTO_BUILDING_PARENT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "buildingParentId";
    public static final String ONTO_BUILDING_ROOT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "buildingRootId";
    public static final String ONTO_CITY_OBJECT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "cityObjectId";
    public static final String ONTO_FOOTPRINT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0FootprintId";
    public static final String ONTO_ROOFPRINT_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0RoofprintId";
    public static final String ONTO_LOD1_MULTI_SURFACE_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1MultiSurfaceId";
    public static final String ONTO_LOD2_MULTI_SURFACE_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2MultiSurfaceId";
    public static final String ONTO_LOD3_MULTI_SURFACE_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3MultiSurfaceId";
    public static final String ONTO_LOD4_MULTI_SURFACE_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4MultiSurfaceId";
    public static final String ONTO_LOD1_SOLID_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1SolidId";
    public static final String ONTO_LOD2_SOLID_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2SolidId";
    public static final String ONTO_LOD3_SOLID_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3SolidId";
    public static final String ONTO_LOD4_SOLID_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4SolidId";
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
    public static final String ONTO_CLASS = ONTO_PREFIX_NAME_ONTOCITYGML + "class";
    public static final String ONTO_CLASS_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "classCodespace";
    public static final String ONTO_FUNCTION = ONTO_PREFIX_NAME_ONTOCITYGML + "function";
    public static final String ONTO_FUNCTION_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "functionCodespace";
    public static final String ONTO_USAGE = ONTO_PREFIX_NAME_ONTOCITYGML + "usage";
    public static final String ONTO_USAGE_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "usageCodespace";
    public static final String ONTO_ROOF_TYPE = ONTO_PREFIX_NAME_ONTOCITYGML + "roofType";
    public static final String ONTO_ROOF_TYPE_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "roofTypeCodespace";
    public static final String ONTO_YEAR_CONSTRUCTION = ONTO_PREFIX_NAME_ONTOCITYGML + "yearOfConstruction";
    public static final String ONTO_YEAR_DEMOLITION = ONTO_PREFIX_NAME_ONTOCITYGML + "yearOfDemolition";
    public static final String ONTO_MEASURED_HEIGHT = ONTO_PREFIX_NAME_ONTOCITYGML + "measuredHeigh";
    public static final String ONTO_MEASURED_HEIGHT_UNIT = ONTO_PREFIX_NAME_ONTOCITYGML + "measuredHeightUnit";
    public static final String ONTO_STOREYS_ABOVE_GROUND = ONTO_PREFIX_NAME_ONTOCITYGML + "storeysAboveGround";
    public static final String ONTO_STOREYS_BELLOW_GROUND = ONTO_PREFIX_NAME_ONTOCITYGML + "storeysBelowGround";
    public static final String ONTO_STOREY_HEIGHTS_ABOVE_GROUND = ONTO_PREFIX_NAME_ONTOCITYGML + "storeyHeightsAboveGround";
    public static final String ONTO_STOREY_HEIGHTS_BELLOW_GROUND = ONTO_PREFIX_NAME_ONTOCITYGML + "storeyHeightsBelowGround";
    public static final String ONTO_STOREY_HEIGHTS_AG_UNIT = ONTO_PREFIX_NAME_ONTOCITYGML + "storeyHeightsAgUnit";
    public static final String ONTO_STOREY_HEIGHTS_BG_UNIT = ONTO_PREFIX_NAME_ONTOCITYGML + "storeyHeightsBgUnit";
    public static final String ONTO_LOD1_TERRAIN_INTERSECTION = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1TerrainIntersection";
    public static final String ONTO_LOD2_TERRAIN_INTERSECTION = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2TerrainIntersection";
    public static final String ONTO_LOD3_TERRAIN_INTERSECTION = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3TerrainIntersection";
    public static final String ONTO_LOD4_TERRAIN_INTERSECTION = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4TerrainIntersection";
    public static final String ONTO_LOD2_MULTI_CURVE = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2MultiCurve";
    public static final String ONTO_LOD3_MULTI_CURVE = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3MultiCurve";
    public static final String ONTO_LOD4_MULTI_CURVE = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4MultiCurve";
    public static final String ONTO_GML_ID_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "gmlIdCodespace";
    public static final String ONTO_MULTI_POINT = ONTO_PREFIX_NAME_ONTOCITYGML + "multiPoint";
    public static final String ONTO_STREET = ONTO_PREFIX_NAME_ONTOCITYGML + "street";
    public static final String ONTO_HOUSE_NUMBER = ONTO_PREFIX_NAME_ONTOCITYGML + "houseNumber";
    public static final String ONTO_PO_BOX = ONTO_PREFIX_NAME_ONTOCITYGML + "poBox";
    public static final String ONTO_ZIP_CODE = ONTO_PREFIX_NAME_ONTOCITYGML + "zipCode";
    public static final String ONTO_CITY = ONTO_PREFIX_NAME_ONTOCITYGML + "city";
    public static final String ONTO_COUNTRY = ONTO_PREFIX_NAME_ONTOCITYGML + "country";
    public static final String ONTO_XAL_SOURCE = ONTO_PREFIX_NAME_ONTOCITYGML + "xalSource";
    public static final String ONTO_TEX_IMAGE_URI = ONTO_PREFIX_NAME_ONTOCITYGML + "texImageURI";
    public static final String ONTO_TEX_MIME_TYPE = ONTO_PREFIX_NAME_ONTOCITYGML + "texMimeType";
    public static final String ONTO_TEX_MIME_TYPE_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "texMimeTypeCodespace";
    public static final String ONTO_IS_FRONT = ONTO_PREFIX_NAME_ONTOCITYGML + "isFront";
    public static final String ONTO_X3D_SHININESS = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dShininess";
    public static final String ONTO_X3D_TRANSPARENCY = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dTransparency";
    public static final String ONTO_X3D_AMBIENT_INTENSITY = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dAmbientIntensity";
    public static final String ONTO_X3D_SPECULAR_COLOR = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dSpecularColor";
    public static final String ONTO_X3D_DIFFUSE_COLOR = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dDiffuseColor";
    public static final String ONTO_X3D_EMISSIVE_COLOR = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dEmissiveColor";
    public static final String ONTO_X3D_IS_SMOOTH = ONTO_PREFIX_NAME_ONTOCITYGML + "x3dIsSmooth";
    public static final String ONTO_TEX_TEXTURE_TYPE = ONTO_PREFIX_NAME_ONTOCITYGML + "texTextureType";
    public static final String ONTO_TEX_WRAP_MODE = ONTO_PREFIX_NAME_ONTOCITYGML + "texWrapMode";
    public static final String ONTO_TEX_BORDER_COLOR = ONTO_PREFIX_NAME_ONTOCITYGML + "texBorderColor";
    public static final String ONTO_GT_PREFER_WORDFILE = ONTO_PREFIX_NAME_ONTOCITYGML + "gtPreferWorldFile";
    public static final String ONTO_GT_ORIENTATION = ONTO_PREFIX_NAME_ONTOCITYGML + "gtOrientation";
    public static final String ONTO_GT_REFERENCE_POINT = ONTO_PREFIX_NAME_ONTOCITYGML + "gtReferencePoint";
    //@TODO: NOT IN ONTOCITYGML - need to be added:
    //@END



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
