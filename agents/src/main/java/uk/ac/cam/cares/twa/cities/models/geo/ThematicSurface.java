package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.ModelAnnotation;

@ModelAnnotation(nativeGraph = SchemaManagerAdapter.THEMATIC_SURFACE_GRAPH)
public class ThematicSurface extends Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_BUILDING_ID)  private URI buildingId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_BUILDING_INSTALLATION_ID)  private URI buildingInstallationId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID)  private SurfaceGeometry lod2MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID)  private SurfaceGeometry lod3MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID)  private SurfaceGeometry lod4MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID)  private Integer objectClassId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ROOM_ID)  private URI roomId;

}
