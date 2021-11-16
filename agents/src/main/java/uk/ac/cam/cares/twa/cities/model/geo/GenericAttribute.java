package uk.ac.cam.cares.twa.cities.model.geo;

import java.net.URI;
import java.util.Arrays;
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
  private final String iriName;

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

  private String GENERIC_ATTRIBUTE_GRAPH_URI = "/cityobjectgenericattrib/";

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

  private static final String VALUE = "value";
  private static final String PREDICATE = "predicate";

  /**
   * constructs an empty generic city attribute instance and fills in the attribute IRI field.
   * @param iriName
   */
  public GenericAttribute(String iriName) {
    this.iriName = iriName;
  }

  /**
   * builds a query to get all generic attribute instance scalars.
   */
  private Query getFetchGenAttrScalarsQuery(String iriName){
    String genericAttributeGraphUri = getGenericAttributeGraphUri(iriName);

    WhereBuilder wb = new WhereBuilder()
        .addPrefix("ocgml", "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#")
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
  public String getGenericAttributeGraphUri(String iriName) {
    String[] splitUri = iriName.split("/");
    String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
    return namespace + GENERIC_ATTRIBUTE_GRAPH_URI;
  }

  /**
   * fills in the scalar fields of a generic attribute instance.
   * @param iriName IRI of the generic attribute instance.
   * @param kgClient sends the query to the right endpoint.
   */
  public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient) {

    Query q = getFetchGenAttrScalarsQuery(iriName);
    String queryResultString = kgClient.execute(q.toString());

    JSONArray queryResult = new JSONArray(queryResultString);

    if(!queryResult.isEmpty()){
      for (int index = 0; index < queryResult.length(); index++){
        JSONObject row = queryResult.getJSONObject(index);
        String predicate = row.getString(PREDICATE);
        String[] predicateArray = predicate.split("#");
        predicate = predicateArray[predicateArray.length-1];

        switch (predicate){

          case ATTR_NAME:
            attrName = row.getString(VALUE);
            break;
          case URI_VAL:
            uriVal = row.getString(VALUE);
            break;
          case STR_VAL:
            strVal = row.getString(VALUE);
            break;
          case UNIT:
            unit = row.getString(VALUE);
            break;
          case  ROOT_GENATTRIB_ID:
            rootGenattribId = row.getString(VALUE);
            break;
          case REAL_VAL:
            realVal = row.getString(VALUE);
            break;
          case PARENT_GENATTRIB_ID:
            parentGenattribId = row.getString(VALUE);
            break;
          case INT_VAL:
            intVal = row.getString(VALUE);
            break;
          case DATE_VAL:
            dateVal = row.getString(VALUE);
            break;
          case ID:
            id = URI.create(row.getString(VALUE));
            break;
          case DATA_TYPE:
            dataType = row.getInt(VALUE);
            break;
          case CITY_OBJECT_ID:
            cityObjectId = URI.create(row.getString(VALUE));
            break;
          default:
            break;
        }
      }
    }
  }



}
