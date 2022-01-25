package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * Model representing OntoCityGML CityObjectGenericAttribute objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(nativeGraphName = SchemaManagerAdapter.GENERIC_ATTRIB_GARPH)
public class GenericAttribute extends Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ATTR_NAME) protected String attrName;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_URI_VAL) protected String uriVal;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_STR_VAL) protected String strVal;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_UNIT) protected String unit;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ROOT_GENATTRIB_ID) protected String rootGenattribId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_REAL_VAL) protected String realVal;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_PARRENT_GENATTRIB_ID) protected String parentGenattribId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_INT_VAL) protected String intVal;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_DATE_VAL) protected String dateVal;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_DATA_TYPE) protected Integer dataType;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID) protected URI cityObjectId;

  /**
   * fills in the scalar fields of a generic attribute instance.
   */
  public void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
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
