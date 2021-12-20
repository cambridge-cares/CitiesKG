package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.ModelField;
import uk.ac.cam.cares.twa.cities.ModelMetadata;

@ModelMetadata(nativeGraph = SchemaManagerAdapter.SURFACE_GEOMETRY_GRAPH)
public class SurfaceGeometry extends Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID) protected URI cityObjectId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_GEOMETRY_IMPLICIT) protected String implicitGeometryType;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_IS_COMPOSITE) protected Integer isComposite;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_IS_REVERSE) protected Integer isReverse;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_IS_SOLID) protected Integer isSolid;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_IS_TRIANGULATED) protected Integer isTriangulated;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_IS_XLINK) protected Integer isXlink;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_PARENT_ID) protected URI parentId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ROOT_ID) protected URI rootId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_GEOMETRY_SOLID) protected String solidType;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_GEOMETRY) protected GeometryType geometryType;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_GML_ID) protected String gmlId;

  @Getter @Setter @ModelField(
      value = SchemaManagerAdapter.ONTO_PARENT_ID,
      innerType = SurfaceGeometry.class,
      backward = true)
  private ArrayList<SurfaceGeometry> surfaceGeometries;

  public List<SurfaceGeometry> getFlattenedSubtree(boolean ignoreNonGeometric) {
    List<SurfaceGeometry> outputList = new ArrayList<>();
    getFlattenedSubtree(outputList, ignoreNonGeometric);
    return outputList;
  }

  public void getFlattenedSubtree(List<SurfaceGeometry> outputList, boolean ignoreNonGeometric) {
    if (!ignoreNonGeometric || getGeometryType() != null)
      outputList.add(this);
    for (SurfaceGeometry child : getSurfaceGeometries())
      child.getFlattenedSubtree(outputList, ignoreNonGeometric);
  }

}
