package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.ArrayList;

import com.hp.hpl.jena.rdf.model.ModelCon;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import lombok.Getter;
import lombok.Setter;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelContext;

/**
 * Model representing OntoCityGML Room objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(nativeGraphName = SchemaManagerAdapter.OPENING_GRAPH + "/")
public class Opening extends OntoCityGMLModel {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID) protected Integer objectClassId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ADDRESS_ID) protected URI addressId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REF_POINT) protected URI lod3ImplicitRefPoint;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_REP_ID) protected String lod3ImplicitRepId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD3_IMPLICIT_TRANSFORMATION) protected String lod3ImplicitTransformation;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID) protected SurfaceGeometry lod3MultiSurfaceId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REF_POINT) protected URI lod4ImplicitRefPoint;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_REP_ID) protected String lod4ImplicitRepId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_IMPLICIT_TRANSFORMATION) protected String lod4ImplicitTransformation;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID) protected SurfaceGeometry lod4MultiSurfaceId;

  @Getter @Setter @FieldAnnotation(
      value = SchemaManagerAdapter.ONTO_THEMSURFACE_ID,
      graphName = SchemaManagerAdapter.OPENING_TO_THEM_SURFACE_GRAPH + "/")
  protected URI themSurfaceId;

  public static final Integer WINDOW_CLASS_ID = 38;
  public static final Integer DOOR_CLASS_ID = 39;

  public static Opening newWindow(ModelContext context, String iri) {
    Opening window = context.createNewModel(Opening.class, iri);
    window.setObjectClassId(WINDOW_CLASS_ID);
    return window;
  }

  public static Opening newDoor(ModelContext context, String iri) {
    Opening window = context.createNewModel(Opening.class, iri);
    window.setObjectClassId(DOOR_CLASS_ID);
    return window;
  }

}
