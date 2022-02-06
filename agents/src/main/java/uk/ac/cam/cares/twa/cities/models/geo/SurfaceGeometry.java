package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * Model representing OntoCityGML SurfaceGeometry objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(defaultGraphName = SchemaManagerAdapter.SURFACE_GEOMETRY_GRAPH + "/")
public class SurfaceGeometry extends OntoCityGMLModel {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID) protected URI cityObjectId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_GEOMETRY_IMPLICIT) protected String implicitGeometryType;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_IS_COMPOSITE) protected Integer isComposite;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_IS_REVERSE) protected Integer isReverse;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_IS_SOLID) protected Integer isSolid;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_IS_TRIANGULATED) protected Integer isTriangulated;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_IS_XLINK) protected Integer isXlink;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_PARENT_ID) protected SurfaceGeometry parentId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ROOT_ID) protected URI rootId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_GEOMETRY_SOLID) protected String solidType;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_GEOMETRY) protected GeometryType geometryType;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_GML_ID) protected String gmlId;

  @Getter @Setter @FieldAnnotation(
      value = SchemaManagerAdapter.ONTO_PARENT_ID,
      innerType = SurfaceGeometry.class,
      backward = true)
  private ArrayList<SurfaceGeometry> children;

  public List<SurfaceGeometry> getFlattenedSubtree(boolean ignoreNonGeometric) {
    List<SurfaceGeometry> outputList = new ArrayList<>();
    getFlattenedSubtree(outputList, ignoreNonGeometric);
    return outputList;
  }

  public void getFlattenedSubtree(List<SurfaceGeometry> outputList, boolean ignoreNonGeometric) {
    if (!ignoreNonGeometric || getGeometryType() != null)
      outputList.add(this);
    for (SurfaceGeometry child : getChildren())
      child.getFlattenedSubtree(outputList, ignoreNonGeometric);
  }

}
