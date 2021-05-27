package org.citydb.database.adapter.blazegraph;

import org.apache.commons.lang3.StringUtils;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.AbstractSchemaManagerAdapter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
    public static final String ONTO_CITY_MODEL_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "cityModelId";
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
    public static final String ONTO_BUILDING_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "buildingId";
    public static final String ONTO_ROOM_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "roomId";
    public static final String ONTO_BUILDING_INSTALLATION_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "buildingInstallationId";
    public static final String ONTO_PARRENT_GENATTRIB_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "parentGenattribId";
    public static final String ONTO_ROOT_GENATTRIB_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "rootGenattribId";
    public static final String ONTO_SURFACE_GEOMETRY_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "surfaceGeometryId";
    public static final String ONTO_SURFACE_DATA_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "surfaceDataId";
    public static final String ONTO_ADDRESS_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "addressId";
    public static final String ONTO_APPEARANCE_ID = ONTO_PREFIX_NAME_ONTOCITYGML + "appearanceId";
    public static final String ONTO_NAME = ONTO_PREFIX_NAME_ONTOCITYGML + "name";
    public static final String ONTO_ATTR_NAME = ONTO_PREFIX_NAME_ONTOCITYGML + "attrName";
    public static final String ONTO_NAME_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "nameCodespace";
    public static final String ONTO_GENATTRIBSET_CODESPACE = ONTO_PREFIX_NAME_ONTOCITYGML + "genattribsetCodespace";
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
    public static final String ONTO_LOD0_TERRAIN_INTERSECTION = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0TerrainIntersection";
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
    public static final String ONTO_THEME = ONTO_PREFIX_NAME_ONTOCITYGML + "theme";
    public static final String ONTO_INFO_SYS = ONTO_PREFIX_NAME_ONTOCITYGML + "infoSys";
    public static final String ONTO_URI = ONTO_PREFIX_NAME_ONTOCITYGML + "URI";
    public static final String ONTO_DATA_TYPE = ONTO_PREFIX_NAME_ONTOCITYGML + "dataType";
    public static final String ONTO_STR_VAL = ONTO_PREFIX_NAME_ONTOCITYGML + "strVal";
    public static final String ONTO_INT_VAL = ONTO_PREFIX_NAME_ONTOCITYGML + "intVal";
    public static final String ONTO_REAL_VAL = ONTO_PREFIX_NAME_ONTOCITYGML + "realVal";
    public static final String ONTO_URI_VAL = ONTO_PREFIX_NAME_ONTOCITYGML + "uriVal";
    public static final String ONTO_DATE_VAL = ONTO_PREFIX_NAME_ONTOCITYGML + "dateVal";
    public static final String ONTO_UNIT = ONTO_PREFIX_NAME_ONTOCITYGML + "unit";
    public static final String ONTO_IS_TEXTURE_PARAMETRIZATION = ONTO_PREFIX_NAME_ONTOCITYGML + "isTextureParametrization";
    public static final String ONTO_WORLD_TO_TEXTURE  = ONTO_PREFIX_NAME_ONTOCITYGML + "worldToTexture";
    public static final String ONTO_TEXTURE_COORDINATES  = ONTO_PREFIX_NAME_ONTOCITYGML + "textureCoordinates";
    public static final String ONTO_LOD0_BREP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0BrepId";
    public static final String ONTO_LOD1_BREP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1BrepId";
    public static final String ONTO_LOD2_BREP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2BrepId";
    public static final String ONTO_LOD3_BREP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3BrepId";
    public static final String ONTO_LOD4_BREP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4BrepId";
    public static final String ONTO_LOD0_OTHER_GEOM  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0OtherGeom";
    public static final String ONTO_LOD1_OTHER_GEOM  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1OtherGeom";
    public static final String ONTO_LOD2_OTHER_GEOM  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2OtherGeom";
    public static final String ONTO_LOD3_OTHER_GEOM  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3OtherGeom";
    public static final String ONTO_LOD4_OTHER_GEOM  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4OtherGeom";
    public static final String ONTO_LOD0_IMPLICIT_REP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0ImplicitRepId";
    public static final String ONTO_LOD1_IMPLICIT_REP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1ImplicitRepId";
    public static final String ONTO_LOD2_IMPLICIT_REP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2ImplicitRepId";
    public static final String ONTO_LOD3_IMPLICIT_REP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3ImplicitRepId";
    public static final String ONTO_LOD4_IMPLICIT_REP_ID  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4ImplicitRepId";
    public static final String ONTO_LOD0_IMPLICIT_REF_POINT  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0ImplicitRefPoint";
    public static final String ONTO_LOD1_IMPLICIT_REF_POINT  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1ImplicitRefPoint";
    public static final String ONTO_LOD2_IMPLICIT_REF_POINT  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2ImplicitRefPoint";
    public static final String ONTO_LOD3_IMPLICIT_REF_POINT  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3ImplicitRefPoint";
    public static final String ONTO_LOD4_IMPLICIT_REF_POINT  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4ImplicitRefPoint";
    public static final String ONTO_LOD0_IMPLICIT_TRANSFORMATION  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod0ImplicitTransformation";
    public static final String ONTO_LOD1_IMPLICIT_TRANSFORMATION  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod1ImplicitTransformation";
    public static final String ONTO_LOD2_IMPLICIT_TRANSFORMATION  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod2ImplicitTransformation";
    public static final String ONTO_LOD3_IMPLICIT_TRANSFORMATION  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod3ImplicitTransformation";
    public static final String ONTO_LOD4_IMPLICIT_TRANSFORMATION  = ONTO_PREFIX_NAME_ONTOCITYGML + "lod4ImplicitTransformation";
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
        boolean exists = true;
        try {
            URL myURL = new URL(schema);
            HttpURLConnection myURLConnection = (HttpURLConnection) myURL.openConnection();
            myURLConnection.connect();
            if (myURLConnection.getResponseCode() != 200) {
                exists = false;
            }
        }
        catch (IOException e){
            exists = false;
        }
        return exists;
    }

    @Override
    public List<String> fetchSchemasFromDatabase(Connection connection) throws SQLException {
        return null;
    }

    @Override
    public String formatSchema(String schema) {
        return schema != null ? schema.trim() : null;
    }
}
