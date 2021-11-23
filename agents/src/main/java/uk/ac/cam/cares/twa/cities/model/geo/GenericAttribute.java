package uk.ac.cam.cares.twa.cities.model.geo;

import java.lang.reflect.Field;
import java.net.URI;
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
  private int intVal;
  private String dateVal;
  private URI id;
  private int dataType;
  private URI cityObjectId;

  // field names.
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

  private final HashMap<String, Object> fieldName = new HashMap<String, Object>(){{
    put(ATTR_NAME, attrName);
    put(URI_VAL, uriVal);
    put(STR_VAL, strVal);
    put(UNIT, unit);
    put(ROOT_GENATTRIB_ID, rootGenattribId);
    put(REAL_VAL, realVal);
    put(PARENT_GENATTRIB_ID, parentGenattribId);
    put(INT_VAL, intVal);
    put(DATE_VAL, dateVal);
    put(ID, id);
    put(DATA_TYPE, dataType);
    put(CITY_OBJECT_ID, cityObjectId);
  }};

  private static String GENERIC_ATTRIBUTE_GRAPH_URI = "/cityobjectgenericattrib/";
  private static final String VALUE = "value";
  private static final String PREDICATE = "predicate";
  private static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";

   /**
   * builds a query to get all generic attribute instance scalars.
   */
  private Query getFetchGenAttrScalarsQuery(String iriName){
    String genericAttributeGraphUri = getGenericAttributeGraphUri(iriName);

    WhereBuilder wb = new WhereBuilder()
        .addPrefix("ocgml", ONTO_CITY_GML)
        .addWhere(NodeFactory.createURI(iriName), "?" + PREDICATE, "?" + VALUE);
    SelectBuilder sb = new SelectBuilder()
        .addVar("?" + PREDICATE)
        .addVar("?" + VALUE)
        .addGraph(NodeFactory.createURI(genericAttributeGraphUri), wb);
    return sb.build();
  }

  /**
   * returns the graph Uri of the generic city object attributes.
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
  public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient)
      throws NoSuchFieldException, IllegalAccessException {

    Query q = getFetchGenAttrScalarsQuery(iriName);
    String queryResultString = kgClient.execute(q.toString());

    JSONArray queryResult = new JSONArray(queryResultString);

    if(!queryResult.isEmpty()){
      for (int index = 0; index < queryResult.length(); index++){
        JSONObject row = queryResult.getJSONObject(index);
        String predicate = row.getString(PREDICATE);
        // is whole uri http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#attrName
        String[] predicateArray = predicate.split("#");
        predicate = predicateArray[predicateArray.length-1];
        //at this point predicate is only attrName

        if (predicate.equals(INT_VAL) || predicate.equals(DATA_TYPE)){
          //fieldName.get(predicate) = row.getInt(VALUE);
          GenericAttribute.class.getField(predicate).set(this, row.getInt(VALUE));
        }
        else if (predicate.equals(ID) || predicate.equals(CITY_OBJECT_ID)){
          //fieldName.get(predicate) = URI.create(row.getString(VALUE));
          GenericAttribute.class.getField(predicate).set(this, URI.create(row.getString(VALUE)));
        }
        else {
          //fieldName.get(predicate) = row.getString(VALUE);
          GenericAttribute.class.getField(predicate).set(this, row.getString(VALUE));
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
  public int getIntVal() {
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
