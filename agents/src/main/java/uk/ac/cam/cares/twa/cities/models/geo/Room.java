package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.ArrayList;

import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import lombok.Getter;
import lombok.Setter;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * Model representing OntoCityGML Room objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(nativeGraphName = SchemaManagerAdapter.ROOM_GRAPH)
public class Room extends Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID) protected Integer objectClassId = 41;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_USAGE) protected String usage;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_USAGE_CODESPACE) protected String usageCodespace;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_FUNCTION) protected String function;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_FUNCTION_CODESPACE) protected String functionCodespace;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_CLASS) protected String classID;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_CLASS_CODESPACE) protected String classCodespace;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_BUILDING_ID) protected URI buildingId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID) protected SurfaceGeometry lod4MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_SOLID_ID) protected SurfaceGeometry lod4SolidId;

}
