package uk.ac.cam.cares.twa.cities.models.geo;

import java.io.InvalidClassException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.ModelField;
import uk.ac.cam.cares.twa.cities.ModelMetadata;

@ModelMetadata(defaultGraph = SchemaManagerAdapter.THEMATIC_SURFACE_GRAPH)
public class ThematicSurface extends Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_BUILDING_ID)  private URI buildingId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_BUILDING_INSTALLATION_ID)  private URI buildingInstallationId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID)  private SurfaceGeometry lod2MultiSurfaceId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID)  private SurfaceGeometry lod3MultiSurfaceId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID)  private SurfaceGeometry lod4MultiSurfaceId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID)  private Integer objectClassId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ROOM_ID)  private URI roomId;

}
