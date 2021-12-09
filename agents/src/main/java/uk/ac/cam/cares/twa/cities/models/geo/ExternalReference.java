package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import org.apache.jena.query.Query;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.twa.cities.Model;

/**
 * ExternalReference class represent a java model of ExternalReference module of CityGML.
 * It retrieves ExternalReference values and fills equivalent fields in the java model.
 */

public class ExternalReference extends Model {

    @Getter @Setter private String infoSys;
    @Getter @Setter private String name;
    @Getter @Setter private String URI;
    @Getter @Setter private URI id;
    @Getter @Setter private URI cityObjectId;


private static final ArrayList<String> FIELD_CONSTANTS =
        new ArrayList<>(
                Arrays.asList(
                        SchemaManagerAdapter.ONTO_INFO_SYS,
                        SchemaManagerAdapter.ONTO_NAME,
                        SchemaManagerAdapter.ONTO_URI,
                        SchemaManagerAdapter.ONTO_ID,
                        SchemaManagerAdapter.ONTO_CITY_OBJECT_ID));

//protected HashMap<String, Field> fieldMap = new HashMap<>();

public ExternalReference() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
}

    /**
     * fills in the scalar fields of an external reference instance.
     */
protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
        throws IllegalAccessException {


      if (predicate.equals(SchemaManagerAdapter.ONTO_ID)
            || predicate.equals(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID)) {
        fieldMap.get(predicate).set(this, java.net.URI.create(row.getString(VALUE)));
    } else {
        fieldMap.get(predicate).set(this, row.getString(VALUE));
    }

}
}