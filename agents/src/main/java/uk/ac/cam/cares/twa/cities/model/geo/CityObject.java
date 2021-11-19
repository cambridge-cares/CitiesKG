package uk.ac.cam.cares.twa.cities.model.geo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;


/**
 * CityObject class represent a java model of CityObject module of CityGML.
 * It retrieves CityObject attributes and fills equivalent fields in the java model.
 */
public class CityObject {
  private final String iriName;

  private String creationDate;
  private String description;
  private String EnvelopeType;
  private String gmlId;
  private URI id;
  private String name;
  private String lineage;
  private String NameCodespace;
  private int objectClassId;
  private String reasonForUpdate;
  private String relativeToTerrain;
  private String relativeToWater;
  private String terminationDate;
  private String updatingPerson;

  private ArrayList<GenericAttribute> genericAttributes;
  //private ArrayList<ExternalReference>;

  private String CITY_OBJECT_GRAPH_URI = "/cityobject/";
  private static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";

  private static final String CREATION_DATE = "creationDate";
  private static final String DESCRIPTION = "description";
  private static final String ENVELOPE_TYPE = "EnvelopeType";
  private static final String GMLID = "gmlId";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String LINEAGE = "lineage";
  private static final String NAME_CODESPACE = "NameCodespace";
  private static final String OBJECT_CLASS_ID = "objectClassId";
  private static final String REASON_FOR_UPDATE = "reasonForUpdate";
  private static final String RELATIVE_TO_TERRAIN = "relativeToTerrain";
  private static final String RELATIVE_TO_WATER = "relativeToWater";
  private static final String TERMINATION_DATE = "terminationDate";
  private static final String UPDATING_PERSON = "updatingPerson";

  private static final String PREDICATE = "predicate";
  private static final String VALUE =  "value";
  private static final String COLLECTION_ELEMENT_IRI = "CollectionElementIri";

  private boolean fieldsFilled = false;

  /**
   * constructs an empty city object instance and fills in the attribute IRI field.
   * @param iriName
   */
  public CityObject(String iriName) {
    this.iriName = iriName;
  }

  /**
   * marks that scalar fields of the object has been set with the information from the KG.
   */
  private void setIsFilled(){
    fieldsFilled = true;
  }

  /**
   * @return status whether scalar fields of the object has been set with the information from the KG.
   */
  public boolean isFilled(){
    return fieldsFilled;
  }

  /**
   * builds a query to get all city object's scalars.
   */
  private Query getFetchScalarsQuery(String iriName){
    String cityObjectGraphUri = getCityObjectGraphUri(iriName);

    WhereBuilder wb = new WhereBuilder()
            .addPrefix("ocgml", ONTO_CITY_GML)
            .addWhere(NodeFactory.createURI(iriName), "?" + PREDICATE, "?" + VALUE);
    SelectBuilder sb = new SelectBuilder()
        .addVar("?" + PREDICATE)
        .addVar("?" + VALUE)
        .addGraph(NodeFactory.createURI(cityObjectGraphUri), wb);
    return sb.build();
  }

  /**
   * returns the graph Uri of the city object.
   * @param iriName city object id
   * @return graph uri of the city object.
   */
  private String getCityObjectGraphUri(String iriName) {
    String[] splitUri = iriName.split("/");
    String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
    return namespace + CITY_OBJECT_GRAPH_URI;
  }

  /**
   * fills in the scalar fields of a generic attribute instance.
   * @param iriName IRI of the generic attribute instance.
   * @param kgClient sends the query to the right endpoint.
   */
  public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient) {

    Query q = getFetchScalarsQuery(iriName);
    String queryResultString = kgClient.execute(q.toString());

    JSONArray queryResult = new JSONArray(queryResultString);

    if(!queryResult.isEmpty()){
      for (int index = 0; index < queryResult.length(); index++){
        JSONObject row = queryResult.getJSONObject(index);
        String predicate = row.getString(PREDICATE);
        String[] predicateArray = predicate.split("#");
        predicate = predicateArray[predicateArray.length-1];

        switch (predicate){

          case CREATION_DATE:
            creationDate = row.getString(VALUE);
            break;
          case DESCRIPTION:
            description = row.getString(VALUE);
            break;
          case ENVELOPE_TYPE:
            EnvelopeType = row.getString(VALUE);
            break;
          case GMLID:
            gmlId = row.getString(VALUE);
            break;
          case ID:
            id = URI.create(row.getString(VALUE));
            break;
          case NAME:
            name = row.getString(VALUE);
            break;
          case LINEAGE:
            lineage = row.getString(VALUE);
            break;
          case NAME_CODESPACE:
            NameCodespace = row.getString(VALUE);
            break;
          case OBJECT_CLASS_ID:
            objectClassId = row.getInt(VALUE);
            break;
          case REASON_FOR_UPDATE:
            reasonForUpdate = row.getString(VALUE);
            break;
          case RELATIVE_TO_TERRAIN:
            relativeToTerrain = row.getString(VALUE);
            break;
          case RELATIVE_TO_WATER:
            relativeToWater = row.getString(VALUE);
            break;
          case TERMINATION_DATE:
            terminationDate = row.getString(VALUE);
            break;
          case UPDATING_PERSON:
            updatingPerson = row.getString(VALUE);
            break;
          default:
            break;
        }
      }
    }
    setIsFilled();
  }

  /**
   * enumerator for distinguishing different collection queries.
   */
  private enum QueryType {
    GENERIC_ATTR,
    EXTERNAL_REF
  }

  /**
   * a general query that based on the query type builds queries either for generic attribute or external reference IRIs.
   * @param iriName cityObject IRI.
   * @return query
   */
  private Query getFetchIrisQuery(String iriName, QueryType queryType){

    if(queryType==QueryType.GENERIC_ATTR){

      String genericAttrGraphUri = GenericAttribute.getGenericAttributeGraphUri(iriName);

      WhereBuilder wb = new WhereBuilder()
          .addPrefix("ocgml", ONTO_CITY_GML)
          .addWhere("?" + COLLECTION_ELEMENT_IRI, "ocgml:cityObjectId", NodeFactory.createURI(iriName));
      SelectBuilder sb = new SelectBuilder()
          .addVar("?" + COLLECTION_ELEMENT_IRI)
          .addGraph(NodeFactory.createURI(genericAttrGraphUri), wb);
      return sb.build();
    }
    else if(queryType==QueryType.EXTERNAL_REF){

      //placeholder to not throw an error but query building needs to be written here//
    }
    Query query = new Query();
    return query;
  }

  /**
   * fills cityObject fields that are collections either with lazyload or fully.
   * @param iriName CityObject IRI.
   * @param kgClient connection to specific KG endpoint.
   * @param lazyload defines whether models have full representations or only iri.
   */
  public void fillCollections(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyload){

    //genericAttribute collection

    Query q = getFetchIrisQuery(iriName, QueryType.GENERIC_ATTR);
    genericAttributes = new ArrayList<>();

    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);
    if(!queryResult.isEmpty()){
      for (int index = 0; index < queryResult.length(); index++){
        String elementIri = queryResult.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);

        GenericAttribute genericAttribute = new GenericAttribute(elementIri);
        if(!lazyload){
          genericAttribute.fillScalars(elementIri, kgClient);
        }
        genericAttributes.add(genericAttribute);
      }

      //externalReference collection

      // here comes the same code for external references
    }
  }

  /**
   * gets the value of city object field creationDate after checking if scalars are filled.
   * @return value of city object field creationDate after checking if scalars are filled.
   * @throws IllegalAccessException
   */
  public String getCreationDate() throws IllegalAccessException {
    if(isFilled()){
      return creationDate;
    }
    else {
      throw new IllegalAccessException("Scalars not filled");
    }
  }

}
