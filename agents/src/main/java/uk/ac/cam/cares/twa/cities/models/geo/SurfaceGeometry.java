package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONObject;

public class SurfaceGeometry  extends Surface {

  @Getter @Setter
  private URI cityObjectId;
  @Getter @Setter
  private URI id;
  @Getter @Setter
  private String ImplicitGeometryType;
  @Getter @Setter
  private int isComposite;
  @Getter @Setter
  private int isReverse;
  @Getter @Setter
  private int isSolid;
  @Getter @Setter
  private int isTriangulated;
  @Getter @Setter
  private int isXlink;
  @Getter @Setter
  private URI parentId;
  @Getter @Setter
  private URI rootId;
  @Getter @Setter
  private String SolidType;
  @Getter @Setter
  private String GeometryType;
  @Getter @Setter
  private String gmlId;



  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, SchemaManagerAdapter.ONTO_ID,
          SchemaManagerAdapter.ONTO_GEOMETRY_IMPLICIT, SchemaManagerAdapter.ONTO_IS_COMPOSITE,
          SchemaManagerAdapter.ONTO_IS_REVERSE, SchemaManagerAdapter.ONTO_IS_SOLID,
          SchemaManagerAdapter.ONTO_IS_TRIANGULATED, SchemaManagerAdapter.ONTO_IS_XLINK,
          SchemaManagerAdapter.ONTO_PARENT_ID, SchemaManagerAdapter.ONTO_ROOT_ID,
          SchemaManagerAdapter.ONTO_GEOMETRY_SOLID, SchemaManagerAdapter.ONTO_GEOMETRY,
          SchemaManagerAdapter.ONTO_GML_ID));

  protected HashMap<String, Field> fieldMap = new HashMap<>();

  public SurfaceGeometry() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
  }

  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
      throws IllegalAccessException {
    if (predicate.equals(SchemaManagerAdapter.ONTO_IS_COMPOSITE
        .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_REVERSE
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_SOLID
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_TRIANGULATED
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_XLINK
        .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this, row.getInt(VALUE));
    } else if (predicate.equals(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID
        .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_ID
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_PARENT_ID
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_ROOT_ID
            .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this,  URI.create(row.getString(VALUE)));
    } else {
      fieldMap.get(predicate).set(this, row.getString(VALUE));
    }
  }



}
