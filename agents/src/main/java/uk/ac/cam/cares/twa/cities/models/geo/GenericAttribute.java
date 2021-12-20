package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.ModelField;
import uk.ac.cam.cares.twa.cities.ModelMetadata;

/**
 * GenericAttribute class represent a java model of GenericCityAttribute module of CityGML. It
 * retrieves GenericCityAttribute attributes and fills equivalent fields in the java model.
 */
@ModelMetadata(nativeGraph = SchemaManagerAdapter.GENERIC_ATTRIB_GARPH)
public class GenericAttribute extends Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ATTR_NAME) protected String attrName;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_URI_VAL) protected String uriVal;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_STR_VAL) protected String strVal;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_UNIT) protected String unit;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ROOT_GENATTRIB_ID) protected String rootGenattribId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_REAL_VAL) protected String realVal;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_PARRENT_GENATTRIB_ID) protected String parentGenattribId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_INT_VAL) protected String intVal;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_DATE_VAL) protected String dateVal;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_DATA_TYPE) protected int dataType;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID) protected URI cityObjectId;

}
