package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.ArrayList;

import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.ModelField;
import lombok.Getter;
import lombok.Setter;
import uk.ac.cam.cares.twa.cities.ModelMetadata;

@ModelMetadata(defaultGraph = SchemaManagerAdapter.BUILDING_GRAPH)
public class Building extends Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_FUNCTION) protected String function;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ROOF_TYPE) protected String roofType;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID) protected URI buildingParentId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_BUILDING_ROOT_ID) protected URI buildingRootId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CLASS) protected String classID; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CLASS_CODESPACE) protected String classCodespace; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE) protected String functionCodespace; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_FOOTPRINT_ID) protected String lod0FootprintId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ROOFPRINT_ID) protected String lod0RoofprintId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD1_MULTI_SURFACE_ID) protected SurfaceGeometry lod1MultiSurfaceId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID) protected SurfaceGeometry lod2MultiSurfaceId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID) protected SurfaceGeometry lod3MultiSurfaceId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID) protected SurfaceGeometry lod4MultiSurfaceId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD1_SOLID_ID) protected SurfaceGeometry lod1SolidId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD2_SOLID_ID) protected SurfaceGeometry lod2SolidId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD3_SOLID_ID) protected SurfaceGeometry lod3SolidId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD4_SOLID_ID) protected SurfaceGeometry lod4SolidId; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD1_TERRAIN_INTERSECTION) protected URI lod1TerrainIntersection; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD2_TERRAIN_INTERSECTION) protected URI lod2TerrainIntersection; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD3_TERRAIN_INTERSECTION) protected URI lod3TerrainIntersection; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD4_TERRAIN_INTERSECTION) protected URI lod4TerrainIntersection; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD2_MULTI_CURVE) protected URI lod2MultiCurve; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD3_MULTI_CURVE) protected URI lod3MultiCurve; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD4_MULTI_CURVE) protected URI lod4MultiCurve; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_MEASURED_HEIGHT) protected Double measuredHeight;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_MEASURED_HEIGHT_UNIT) protected String measuredHeightUnit; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID) protected Integer objectClassId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ROOF_TYPE_CODESPACE) protected String roofTypeCodespace; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_ABOVE_GROUND) protected String storeyHeightsAboveGround; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_BELLOW_GROUND) protected String storeyHeightsBelowGround; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_AG_UNIT) protected String storeyHeightsAgUnit; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STOREY_HEIGHTS_BG_UNIT) protected String storeyHeightsBgUnit; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STOREYS_ABOVE_GROUND) protected String storeysAboveGround; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STOREYS_BELLOW_GROUND) protected String storeysBelowGround; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_USAGE) protected String usage; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_USAGE_CODESPACE) protected String usageCodespace; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_YEAR_CONSTRUCTION) protected String yearOfConstruction; // check-type
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_YEAR_DEMOLITION) protected String yearOfDemolition; // check-type

  @Getter @Setter @ModelField(
      value = SchemaManagerAdapter.ONTO_BUILDING_ID,
      graphName = SchemaManagerAdapter.THEMATIC_SURFACE_GRAPH,
      innerType = ThematicSurface.class,
      backward = true)
  private ArrayList<ThematicSurface> thematicSurfaces;

}
