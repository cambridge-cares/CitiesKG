package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.json.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.Model;

/**
 * GenericAttribute class represent a java model of GenericCityAttribute module of CityGML. It
 * retrieves GenericCityAttribute attributes and fills equivalent fields in the java model.
 */
public class GenericAttribute extends Model {

  @Getter @Setter private String attrName;
  @Getter @Setter private String uriVal;
  @Getter @Setter private String strVal;
  @Getter @Setter private String unit;
  @Getter @Setter private String rootGenattribId;
  @Getter @Setter private String realVal;
  @Getter @Setter private String parentGenattribId;
  @Getter @Setter private String intVal;
  @Getter @Setter private String dateVal;
  @Getter @Setter private URI id;
  @Getter @Setter private int dataType;
  @Getter @Setter private URI cityObjectId;


  private static final ArrayList<String> FIELD_CONSTANTS =
      new ArrayList<>(
          Arrays.asList(
              SchemaManagerAdapter.ONTO_ATTR_NAME,
              SchemaManagerAdapter.ONTO_URI_VAL,
              SchemaManagerAdapter.ONTO_STR_VAL,
              SchemaManagerAdapter.ONTO_UNIT,
              SchemaManagerAdapter.ONTO_ROOT_GENATTRIB_ID,
              SchemaManagerAdapter.ONTO_REAL_VAL,
              SchemaManagerAdapter.ONTO_PARRENT_GENATTRIB_ID,
              SchemaManagerAdapter.ONTO_INT_VAL,
              SchemaManagerAdapter.ONTO_DATE_VAL,
              SchemaManagerAdapter.ONTO_ID,
              SchemaManagerAdapter.ONTO_DATA_TYPE,
              SchemaManagerAdapter.ONTO_CITY_OBJECT_ID));

  //protected HashMap<String, Field> fieldMap = new HashMap<>();

  public GenericAttribute() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
  }

  /**
   * fills in the scalar fields of a generic attribute instance.
   */
  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
      throws IllegalAccessException {

    if (predicate.equals(SchemaManagerAdapter.ONTO_DATA_TYPE)) {
      fieldMap.get(predicate).set(this, row.getInt(VALUE));
    } else if (predicate.equals(SchemaManagerAdapter.ONTO_ID)
        || predicate.equals(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID)) {
      fieldMap.get(predicate).set(this, URI.create(row.getString(VALUE)));
    } else {
      fieldMap.get(predicate).set(this, row.getString(VALUE));
    }

  }
}
