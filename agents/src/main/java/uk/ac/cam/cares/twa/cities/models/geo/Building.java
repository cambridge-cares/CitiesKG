package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.Model;


public class Building extends Model {

  private String function;
  private String roofType;
  private URI buildingParentId;
  private URI buildingRootId;
  private String classID; //check-type
  private String classCodespace; //check-type
  private String functionCodespace; //check-type
  private URI id;
  private String lod0FootprintId; //check-type
  private String lod0RoofprintId; //check-type
  private String lod1MultiSurfaceId; //check-type
  private String lod1SolidId; //check-type
  private String lod1TerrainIntersection; //check-type
  private String lod2MultiCurve; //check-type
  private String lod2MultiSurfaceId; //check-type
  private String lod2SolidId; //check-type
  private String lod2TerrainIntersection; //check-type
  private String lod3MultiCurve; //check-type
  private String lod3MultiSurfaceId; //check-type
  private String lod3SolidId; //check-type
  private String lod3TerrainIntersection; //check-type
  private String lod4MultiCurve; //check-type
  private String lod4MultiSurfaceId; //check-type
  private String lod4SolidId; //check-type
  private String lod4TerrainIntersection; //check-type
  private double measuredHeigh;
  private String measuredHeightUnit; //check-type
  private int objectClassId;
  private String roofTypeCodespace; //check-type
  private String storeyHeightsAboveGround; //check-type
  private String storeyHeightsAgUnit; //check-type
  private String storeyHeightsBelowGround; //check-type
  private String storeyHeightsBgUnit; //check-type
  private String storeysAboveGround; //check-type
  private String storeysBelowGround; //check-type
  private String usage; //check-type
  private String usageCodespace; //check-type
  private String yearOfConstruction; //check-type
  private String yearOfDemolition; //check-type


  private ArrayList<ThematicSurface> thematicSurfaces;
  private ArrayList<String> thematicSurfacesIris;


  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(SchemaManagerAdapter.ONTO_FUNCTION, SchemaManagerAdapter.ONTO_ROOF_TYPE,
          SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID, SchemaManagerAdapter.ONTO_BUILDING_ROOT_ID,
          SchemaManagerAdapter.ONTO_BUILDING_ROOT_ID, SchemaManagerAdapter.ONTO_CLASS + "ID",
          SchemaManagerAdapter.ONTO_CLASS_CODESPACE, SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE,
          SchemaManagerAdapter.ONTO_ID, SchemaManagerAdapter.ONTO_FOOTPRINT_ID,
          SchemaManagerAdapter.ONTO_ROOFPRINT_ID, SchemaManagerAdapter.ONTO_LOD1_MULTI_SURFACE_ID,
          SchemaManagerAdapter.ONTO_LOD1_SOLID_ID, SchemaManagerAdapter.ONTO_LOD1_TERRAIN_INTERSECTION,
          SchemaManagerAdapter.ONTO_LOD2_MULTI_CURVE, SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID,
          SchemaManagerAdapter.ONTO_LOD2_SOLID_ID, SchemaManagerAdapter.ONTO_LOD2_TERRAIN_INTERSECTION,
          SchemaManagerAdapter.ONTO_LOD3_MULTI_CURVE, SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID,
          SchemaManagerAdapter.ONTO_LOD3_SOLID_ID, SchemaManagerAdapter.ONTO_LOD3_TERRAIN_INTERSECTION,
          SchemaManagerAdapter.ONTO_LOD4_MULTI_CURVE, SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID,
          SchemaManagerAdapter.ONTO_LOD4_SOLID_ID, SchemaManagerAdapter.ONTO_LOD4_TERRAIN_INTERSECTION,
          SchemaManagerAdapter.ONTO_MEASURED_HEIGHT, SchemaManagerAdapter.ONTO_MEASURED_HEIGHT_UNIT,
          SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, SchemaManagerAdapter.ONTO_ROOF_TYPE_CODESPACE,
          SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_ABOVE_GROUND, SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_AG_UNIT,
          SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_BELLOW_GROUND, SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_BG_UNIT,
          SchemaManagerAdapter.ONTO_STOREYS_ABOVE_GROUND,SchemaManagerAdapter.ONTO_STOREYS_BELLOW_GROUND,
          SchemaManagerAdapter.ONTO_USAGE, SchemaManagerAdapter.ONTO_USAGE_CODESPACE,
          SchemaManagerAdapter.ONTO_YEAR_CONSTRUCTION, SchemaManagerAdapter.ONTO_YEAR_DEMOLITION));

  protected HashMap<String, Field> fieldMap = new HashMap<>();

  public Building() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
  }

  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
      throws IllegalAccessException {
    if (predicate.equals(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID
        .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this, row.getInt(VALUE));
    } else if (predicate.equals(SchemaManagerAdapter.ONTO_MEASURED_HEIGHT
        .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this, row.getDouble(VALUE));
    } else if (predicate.equals(SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID
        .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_BUILDING_ROOT_ID
            .replace(OCGML + ":", ""))){
      fieldMap.get(predicate).set(this, URI.create(row.getString(VALUE)));
    }
    else {
      if (predicate.equals(SchemaManagerAdapter.ONTO_CLASS
          .replace(OCGML + ":", ""))) {
        predicate = predicate + "ID";
      }
      fieldMap.get(predicate).set(this, row.getString(VALUE));
    }
  }

}

