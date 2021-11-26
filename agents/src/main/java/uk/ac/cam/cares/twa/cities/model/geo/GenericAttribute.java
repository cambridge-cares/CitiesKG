package uk.ac.cam.cares.twa.cities.model.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

/**
 * GenericAttribute class represent a java model of GenericCityAttribute module of CityGML.
 * It retrieves GenericCityAttribute attributes and fills equivalent fields in the java model.
 */
public class GenericAttribute {

  private String attrName;
  private String uriVal;
  private String strVal;
  private String unit;
  private String rootGenattribId;
  private String realVal;
  private String parentGenattribId;
  private String intVal;
  private String dateVal;
  private URI id;
  private int dataType;
  private URI cityObjectId;

  private static final String GENERIC_ATTRIBUTE_GRAPH_URI = "/cityobjectgenericattrib/";
  private static final String VALUE = "value";
  private static final String PREDICATE = "predicate";
  private static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
  private static final String OCGML = "ocgml";
  private static final String QM = "?";

  private static final String ATTR_NAME = "attrName";
  private static final String URI_VAL = "uriVal";
  private static final String STR_VAL = "strVal";
  private static final String UNIT = "unit";
  private static final String ROOT_GENATTRIB_ID = "rootGenattribId";
  private static final String REAL_VAL = "realVal";
  private static final String PARENT_GENATTRIB_ID = "parentGenattribId";
  private static final String INT_VAL = "intVal";
  private static final String DATE_VAL = "dateVal";
  private static final String ID = "id";
  private static final String DATA_TYPE = "dataType";
  private static final String CITY_OBJECT_ID = "cityObjectId";

  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(ATTR_NAME, URI_VAL, STR_VAL, UNIT, ROOT_GENATTRIB_ID, REAL_VAL, PARENT_GENATTRIB_ID, INT_VAL, DATE_VAL, ID, DATA_TYPE, CITY_OBJECT_ID ));
  private HashMap<String, Field> fieldMap = new HashMap<String, Field>();


  public GenericAttribute() throws NoSuchFieldException {
    for (String field: FIELD_CONSTANTS){
      fieldMap.put(field, GenericAttribute.class.getDeclaredField(field));
    }
  }


  /**
   * * builds a query to get genericAttribute instance scalars.
   */
  private Query getFetchGenAttrScalarsQuery(String iriName){
    String genericAttributeGraphUri = getGenericAttributeGraphUri(iriName);

    WhereBuilder wb = new WhereBuilder()
        .addPrefix(OCGML, ONTO_CITY_GML)
        .addWhere(NodeFactory.createURI(iriName), QM + PREDICATE, QM + VALUE);
    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + PREDICATE)
        .addVar(QM + VALUE)
        .addGraph(NodeFactory.createURI(genericAttributeGraphUri), wb);
    return sb.build();
  }


  /**
   * builds graph Uri of the genericCityObjectAttributes.
   * @param iriName IRI of the generic attribute instance.
   * @return uri of the generic attributes graph.
   */
  public static String getGenericAttributeGraphUri(String iriName) {
    String[] splitUri = iriName.split("/");
    String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
    return namespace + GENERIC_ATTRIBUTE_GRAPH_URI;
  }


  /**
   * fills in the scalar fields of a generic attribute instance.
   * @param iriName IRI of the generic attribute instance.
   * @param kgClient sends the query to the right endpoint.
   */
  public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient) throws IllegalAccessException {

    Query q = getFetchGenAttrScalarsQuery(iriName);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if(!queryResult.isEmpty()){
      for (int index = 0; index < queryResult.length(); index++){
        JSONObject row = queryResult.getJSONObject(index);
        String predicate = row.getString(PREDICATE);
        String[] predicateArray = predicate.split("#");
        predicate = predicateArray[predicateArray.length-1];

        if (predicate.equals(DATA_TYPE)){
          fieldMap.get(predicate).set(this, row.getInt(VALUE));
        }
        else if (predicate.equals(ID) || predicate.equals(CITY_OBJECT_ID)){
          fieldMap.get(predicate).set(this, URI.create(row.getString(VALUE)));
        }
        else {
          fieldMap.get(predicate).set(this, row.getString(VALUE));
        }
      }
    }
  }


  /**
   * gets the value of generic attribute field attrName.
   * @return value of attrName field
   */
  public String getAttrName() {
      return attrName;
  }


  /**
   * gets the value of generic attribute field uriVal.
   * @return value of uriVal field
   */
  public String getUriVal() {
    return uriVal;
  }


  /**
   * gets the value of generic attribute field strVal.
   * @return value of strVal field
   */
  public String getStrVal() {
    return strVal;
  }


  /**
   * gets the value of generic attribute field unit.
   * @return value of unit field
   */
  public String getUnit() {
    return unit;
  }


  /**
   * gets the value of generic attribute field rootGenattribId.
   * @return value of rootGenattribId field
   */
  public String getRootGenattribId() {
    return rootGenattribId;
  }


  /**
   * gets the value of generic attribute field realVal.
   * @return value of realVal field
   */
  public String getRealVal() {
    return realVal;
  }


  /**
   * gets the value of generic attribute field parentGenattribId.
   * @return value of parentGenattribId field
   */
  public String getParentGenattribId() {
    return parentGenattribId;
  }


  /**
   * gets the value of generic attribute field intVal.
   * @return value of intVal field
   */
  public String getIntVal() {
    return intVal;
  }


  /**
   * gets the value of generic attribute field dateVal.
   * @return value of dateVal field
   */
  public String getDateVal() {
    return dateVal;
  }


  /**
   * gets the value of generic attribute field id.
   * @return value of id field
   */
  public URI getId() {
    return id;
  }


  /**
   * gets the value of generic attribute field dataType.
   * @return value of dataType field
   */
  public int getDataType() {
    return dataType;
  }


  /**
   * gets the value of generic attribute field cityObjectId.
   * @return value of cityObjectId field
   */
  public URI getCityObjectId() {
    return cityObjectId;
  }
}
