package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.jena.query.Query;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.twa.cities.Model;
import lombok.Getter;
import lombok.Setter;


public class Building extends Model {

  @Getter @Setter
  private String function;
  @Getter @Setter
  private String roofType;
  @Getter @Setter
  private URI buildingParentId;
  @Getter @Setter
  private URI buildingRootId;
  @Getter @Setter
  private String classID; //check-type
  @Getter @Setter
  private String classCodespace; //check-type
  @Getter @Setter
  private String functionCodespace; //check-type
  @Getter @Setter
  private URI id;
  @Getter @Setter
  private String lod0FootprintId; //check-type
  @Getter @Setter
  private String lod0RoofprintId; //check-type
  @Getter @Setter
  private String lod1MultiSurfaceId; //check-type
  @Getter @Setter
  private String lod1SolidId; //check-type
  @Getter @Setter
  private String lod1TerrainIntersection; //check-type
  @Getter @Setter
  private String lod2MultiCurve; //check-type
  @Getter @Setter
  private String lod2MultiSurfaceId; //check-type
  @Getter @Setter
  private String lod2SolidId; //check-type
  @Getter @Setter
  private String lod2TerrainIntersection; //check-type
  @Getter @Setter
  private String lod3MultiCurve; //check-type
  @Getter @Setter
  private String lod3MultiSurfaceId; //check-type
  @Getter @Setter
  private String lod3SolidId; //check-type
  @Getter @Setter
  private String lod3TerrainIntersection; //check-type
  @Getter @Setter
  private String lod4MultiCurve; //check-type
  @Getter @Setter
  private String lod4MultiSurfaceId; //check-type
  @Getter @Setter
  private String lod4SolidId; //check-type
  @Getter @Setter
  private String lod4TerrainIntersection; //check-type
  @Getter @Setter
  private double measuredHeigh;
  @Getter @Setter
  private String measuredHeightUnit; //check-type
  @Getter @Setter
  private int objectClassId;
  @Getter @Setter
  private String roofTypeCodespace; //check-type
  @Getter @Setter
  private String storeyHeightsAboveGround; //check-type
  @Getter @Setter
  private String storeyHeightsAgUnit; //check-type
  @Getter @Setter
  private String storeyHeightsBelowGround; //check-type
  @Getter @Setter
  private String storeyHeightsBgUnit; //check-type
  @Getter @Setter
  private String storeysAboveGround; //check-type
  @Getter @Setter
  private String storeysBelowGround; //check-type
  @Getter @Setter
  private String usage; //check-type
  @Getter @Setter
  private String usageCodespace; //check-type
  @Getter @Setter
  private String yearOfConstruction; //check-type
  @Getter @Setter
  private String yearOfDemolition; //check-type

  @Getter @Setter
  private ArrayList<ThematicSurface> thematicSurfaces;
  @Getter @Setter
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
    } else {
      if (predicate.equals(SchemaManagerAdapter.ONTO_CLASS
          .replace(OCGML + ":", ""))) {
        predicate = predicate + "ID";
      }
      fieldMap.get(predicate).set(this, row.getString(VALUE));
    }
  }


  /**
   * fills in generic attributes linked to the city object.
   * @param iriName cityObject IRI
   * @param kgClient sends the query to the right endpoint.
   * @param lazyLoad if true only fills genericAttributesIris field; if false also fills genericAttributes field.
   */
  public void fillThematicSurfaces(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    Query q = getFetchIrisQuery(iriName, SchemaManagerAdapter.ONTO_BUILDING_ID);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      thematicSurfaces = new ArrayList<>();
      thematicSurfacesIris = new ArrayList<>();

      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResult.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        thematicSurfacesIris.add(elementIri);

        if (!lazyLoad) {
          ThematicSurface thematicSurface = new ThematicSurface();
          thematicSurface.fillScalars(elementIri, kgClient);
          thematicSurfaces.add(thematicSurface);
        }
      }
    }
  }


}

