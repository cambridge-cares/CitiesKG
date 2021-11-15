package uk.ac.cam.cares.twa.cities.model.geo;

import java.net.URI;
import java.util.Arrays;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;
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
        .addWhere(NodeFactory.createURI(iriName), "?predicates", "?values");
    SelectBuilder sb = new SelectBuilder()
        .addVar("?predicates")
        .addVar("?values")
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

    attrName = queryResult.getJSONObject(0).getString(ATTR_NAME);
    uriVal = queryResult.getJSONObject(0).getString(URI_VAL);
    strVal = queryResult.getJSONObject(0).getString(STR_VAL);
    unit = queryResult.getJSONObject(0).getString(UNIT);
    rootGenattribId = queryResult.getJSONObject(0).getString(ROOT_GENATTRIB_ID);
    realVal = queryResult.getJSONObject(0).getString(REAL_VAL);
    parentGenattribId = queryResult.getJSONObject(0).getString(PARENT_GENATTRIB_ID);
    intVal = queryResult.getJSONObject(0).getString(INT_VAL);
    dateVal = queryResult.getJSONObject(0).getString(DATE_VAL);
    id = URI.create(queryResult.getJSONObject(0).getString(ID));
    dataType = queryResult.getJSONObject(0).getInt(DATA_TYPE);
    cityObjectId = URI.create(queryResult.getJSONObject(0).getString(CITY_OBJECT_ID));
  }

}
