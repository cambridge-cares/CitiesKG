package uk.ac.cam.cares.twa.cities.model.geo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;


/**
 * CityObject class represent a java model of CityObject module of CityGML.
 * It retrieves CityObject attributes and fills equivalent fields in the java model.
 */
public class CityObject {
  private final String iriName;

  private String creatingDate;
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
  private static final String COLLECTION_ELEMENT_IRI = "CollectionElementIri";

  private static final String CREATION_DATE = "creatingDate";
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

  /**
   * constructs an empty city object instance and fills in the attribute IRI field.
   * @param iriName
   */
  public CityObject(String iriName) {
    this.iriName = iriName;
  }

  /**
   * builds a query to get all city object's scalars.
   */
  private Query getFetchScalarsQuery(String iriName){
    String cityObjectGraphUri = getCityObjectGraphUri(iriName);

    //placeholder to not throw an error but query building needs to be written here//

    Query query = new Query();
    return query;
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

    creatingDate = queryResult.getJSONObject(0).getString(CREATION_DATE);
    description = queryResult.getJSONObject(0).getString(DESCRIPTION);
    EnvelopeType = queryResult.getJSONObject(0).getString(ENVELOPE_TYPE);
    gmlId = queryResult.getJSONObject(0).getString(GMLID);
    id = URI.create(queryResult.getJSONObject(0).getString(ID));
    name = queryResult.getJSONObject(0).getString(NAME);
    lineage = queryResult.getJSONObject(0).getString(LINEAGE);
    NameCodespace = queryResult.getJSONObject(0).getString(NAME_CODESPACE);
    objectClassId = queryResult.getJSONObject(0).getInt(OBJECT_CLASS_ID);
    reasonForUpdate = queryResult.getJSONObject(0).getString(REASON_FOR_UPDATE);
    relativeToTerrain = queryResult.getJSONObject(0).getString(RELATIVE_TO_TERRAIN);
    relativeToWater = queryResult.getJSONObject(0).getString(RELATIVE_TO_WATER);
    terminationDate = queryResult.getJSONObject(0).getString(TERMINATION_DATE);
    updatingPerson = queryResult.getJSONObject(0).getString(UPDATING_PERSON);
  }

  /**
   * enumerator for distinguishing different collection queries.
   */
  private enum QueryType {
    GENERIC_ATTR,
    EXTERNAL_REF
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
   * a general query that based on the query type builds queries either for generic attribute or external reference IRIs.
   * @param iriName cityObject IRI.
   * @return query
   */
  private Query getFetchIrisQuery(String iriName, QueryType queryType){

    if(queryType==QueryType.GENERIC_ATTR){

      //placeholder to not throw an error but query building needs to be written here//
    }
    else if(queryType==QueryType.EXTERNAL_REF){

      //placeholder to not throw an error but query building needs to be written here//

    }

    Query query = new Query();
    return query;
  }


}
