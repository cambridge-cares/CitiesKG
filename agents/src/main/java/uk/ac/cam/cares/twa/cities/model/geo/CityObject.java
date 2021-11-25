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
 * CityObject class represent a java model of CityObject module of CityGML.
 * It retrieves CityObject attributes and fills equivalent fields in the java model.
 */
public class CityObject {

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

  private static final String PREDICATE = "predicate";
  private static final String VALUE =  "value";
  private static final String COLLECTION_ELEMENT_IRI = "CollectionElementIri";
  private static final String CITY_OBJECT_GRAPH_URI = "/cityobject/";
  private static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
  private static final String OCGML = "ocgml";
  private static final String QM = "?";

  private ArrayList<GenericAttribute> genericAttributes;
  private ArrayList<String> genericAttributeIris;
  private ArrayList<ExternalReference> externalReferences;
  private ArrayList<String> externalReferencesIris;

  private static final String CREATION_DATE = "creationDate";
  private static final String DESCRIPTION = "description";
  private static final String ENVELOPE_TYPE = "EnvelopeType";
  private static final String GML_ID = "gmlId";
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

  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(CREATION_DATE, DESCRIPTION, ENVELOPE_TYPE, GML_ID, ID, NAME, LINEAGE, NAME_CODESPACE, OBJECT_CLASS_ID,
          REASON_FOR_UPDATE, RELATIVE_TO_TERRAIN, RELATIVE_TO_WATER, TERMINATION_DATE, UPDATING_PERSON));
  public HashMap<String, Field> fieldMap = new HashMap<String, Field>();

  public CityObject() throws NoSuchFieldException {
    for (String field: FIELD_CONSTANTS){
      fieldMap.put(field, CityObject.class.getDeclaredField(field));
    }
  }


  /**
   * builds a query to get all city object's scalars.
   */
  private Query getFetchScalarsQuery(String iriName){
    String cityObjectGraphUri = getCityObjectGraphUri(iriName);

    WhereBuilder wb = new WhereBuilder()
            .addPrefix(OCGML, ONTO_CITY_GML)
            .addWhere(NodeFactory.createURI(iriName), QM + PREDICATE, QM + VALUE);
    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + PREDICATE)
        .addVar(QM + VALUE)
        .addGraph(NodeFactory.createURI(cityObjectGraphUri), wb);
    return sb.build();
  }


  /**
   * returns the graph Uri of the city object.
   * @param iriName city object id
   * @return graph uri of the city object.
   */
  public String getCityObjectGraphUri(String iriName) {
    String[] splitUri = iriName.split("/");
    String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
    return namespace + CITY_OBJECT_GRAPH_URI;
  }


  /**
   * fills in the scalar fields of a generic attribute instance.
   * @param iriName IRI of the generic attribute instance.
   * @param kgClient sends the query to the right endpoint.
   */
  public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient)
      throws IllegalAccessException {

    Query q = getFetchScalarsQuery(iriName);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if(!queryResult.isEmpty()){
      for (int index = 0; index < queryResult.length(); index++){
        JSONObject row = queryResult.getJSONObject(index);
        String predicate = row.getString(PREDICATE);
        String[] predicateArray = predicate.split("#");
        predicate = predicateArray[predicateArray.length-1];

       if (predicate.equals(OBJECT_CLASS_ID)){
          fieldMap.get(predicate).set(this, row.getInt(VALUE));
        }
        else if (predicate.equals(ID)){
          fieldMap.get(predicate).set(this, URI.create(row.getString(VALUE)));
        }
        else {
          fieldMap.get(predicate).set(this, row.getString(VALUE));
        }
      }
    }
  }


  /**
   * enumerator for distinguishing different collection queries.
   */
  private enum QueryType {
    GENERIC_ATTR,
    EXTERNAL_REF
  }


  /**
   * builds query to retrieve  either genericAttribute or externalReference IRIs.
   * @param iriName cityObject IRI.
   * @return query
   */
  private Query getFetchIrisQuery(String iriName, QueryType queryType){
    String graphUri = new String();

    if(queryType==QueryType.GENERIC_ATTR){
      graphUri = GenericAttribute.getGenericAttributeGraphUri(iriName);
    }
    else if(queryType==QueryType.EXTERNAL_REF){
      //graphUri = ExternalReference.getGetExternalReferenceGraphUri(iriName); --> code does not exist yet
    }
    WhereBuilder wb = new WhereBuilder()
        .addPrefix(OCGML, ONTO_CITY_GML)
        .addWhere(QM + COLLECTION_ELEMENT_IRI, OCGML + ":cityObjectId", NodeFactory.createURI(iriName));
    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + COLLECTION_ELEMENT_IRI)
        .addGraph(NodeFactory.createURI(graphUri), wb);

    return sb.build();
  }


  /**
   * fills in generic attributes linked to the city object.
   * @param iriName cityObject IRI
   * @param kgClient sends the query to the right endpoint.
   * @param lazyLoad if true only fills genericAttributesIris field; if false also fills genericAttributes field.
   */
  public void fillGenericAttributes(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    Query q = getFetchIrisQuery(iriName, QueryType.GENERIC_ATTR);
    genericAttributes = new ArrayList<>();
    genericAttributeIris = new ArrayList<>();

    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResult.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        genericAttributeIris.add(elementIri);

        if (!lazyLoad) {
          GenericAttribute genericAttribute = new GenericAttribute();
          genericAttribute.fillScalars(elementIri, kgClient);
          genericAttributes.add(genericAttribute);
        }
      }
    }
  }


  //here comes fillExternalReferences method.-->


  /**
   * gets the value of city object field creationDate.
   * @return value of city object field creationDate.
   */
  public String getCreationDate(){
      return creationDate;
  }


  /**
   * gets the value of city object field description.
   * @return value of city object field description.
   */
  public String getDescription(){
    return description;
  }


  /**
   * gets the value of city object field EnvelopeType.
   * @return value of city object field EnvelopeType.
   */
  public String getEnvelopeType(){
    return EnvelopeType;
  }


  /**
   * gets the value of city object field gmlId.
   * @return value of city object field gmlId.
   */
  public String getGmlId(){
    return gmlId;
  }


  /**
   * gets the value of city object field id.
   * @return value of city object field id.
   */
  public URI getId(){
    return id;
  }


  /**
   * gets the value of city object field name.
   * @return value of city object field name.
   */
  public String getName(){
    return name;
  }


  /**
   * gets the value of city object field lineage.
   * @return value of city object field lineage.
   */
  public String getLineage(){
    return lineage;
  }


  /**
   * gets the value of city object field NameCodespace.
   * @return value of city object field NameCodespace.
   */
  public String getNameCodespace(){
    return NameCodespace;
  }


  /**
   * gets the value of city object field objectClassId.
   * @return value of city object field objectClassId.
   */
  public int getObjectClassId(){
    return objectClassId;
  }


  /**
   * gets the value of city object field reasonForUpdate.
   * @return value of city object field reasonForUpdate.
   */
  public String getReasonForUpdate(){
    return reasonForUpdate;
  }


  /**
   * gets the value of city object field relativeToTerrain.
   * @return value of city object field relativeToTerrain.
   */
  public String getRelativeToTerrain(){
    return relativeToTerrain;
  }


  /**
   * gets the value of city object field relativeToWater.
   * @return value of city object field relativeToWater.
   */
  public String getRelativeToWater(){
    return relativeToWater;
  }


  /**
   * gets the value of city object field terminationDate.
   * @return value of city object field terminationDate.
   */
  public String getTerminationDate(){
    return terminationDate;
  }


  /**
   * gets the value of city object field updatingPerson.
   * @return value of city object field updatingPerson.
   */
  public String getUpdatingPerson(){
    return updatingPerson;
  }


  /**
   * gets the value of city object field genericAttributesIris.
   * @return value of city object field genericAttributesIris.
   */
  public ArrayList<String> getGenericAttributesIris(){
    return genericAttributeIris;
  }


  /**
   * gets the value of city object field genericAttributes.
   * @return value of city object field genericAttributes.
   */
  public ArrayList<GenericAttribute> getGenericAttributes(){
    return genericAttributes;
  }


  //here comes getters for external reference fields -->

}
