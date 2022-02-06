package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * Model representing OntoCityGML thematic surface (_BoundarySurface) objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(defaultGraphName = SchemaManagerAdapter.THEMATIC_SURFACE_GRAPH + "/")
public class ThematicSurface extends OntoCityGMLModel {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_BUILDING_ID)  private URI buildingId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_BUILDING_INSTALLATION_ID)  private URI buildingInstallationId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID)  private SurfaceGeometry lod2MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID)  private SurfaceGeometry lod3MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID)  private SurfaceGeometry lod4MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID)  private Integer objectClassId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ROOM_ID)  private URI roomId;

  @Getter @Setter @FieldAnnotation(
      value = SchemaManagerAdapter.ONTO_OPENING_ID,
      graphName = SchemaManagerAdapter.OPENING_TO_THEM_SURFACE_GRAPH + "/",
      innerType = Opening.class) private ArrayList<Opening> openingId;

}
