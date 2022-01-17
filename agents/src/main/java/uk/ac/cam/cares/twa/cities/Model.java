package uk.ac.cam.cares.twa.cities;

import java.lang.reflect.Field;
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

public class Model {

  protected static final String PREDICATE = "predicate";
  protected static final String VALUE =  "value";
  protected static final String COLLECTION_ELEMENT_IRI = "CollectionElementIri";
  //protected static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
  protected static final String ONTO_CITY_GML = "http://locahost/ontocitygml/";
  protected static final String OCGML = "ocgml";
  protected static final String QM = "?";
  protected HashMap<String, Field> fieldMap = new HashMap<String, Field>();

  protected void assignFieldValues(ArrayList fieldList, HashMap fieldMap) throws NoSuchFieldException {
    for (Object field: fieldList){
      fieldMap.put(field, this.getClass().getDeclaredField(String.valueOf(field)
          .replace(OCGML + ":", "")));
    }
  }

  /**
   * returns the graph Uri of an object.
   * @param iriName object id
   * @return graph uri.
   */
  protected String getGraphUri(String iriName) {
    String[] splitUri = iriName.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-1)) + "/";
  }

  /**
   * returns the namespace object is in.
   * @param iriName object id
   * @return namespace as string.
   */
  protected String getNamespace(String iriName) {
    String[] splitUri = iriName.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2)) + "/";
  }

  /**
   * builds a query to get all city object's scalars.
   */
  protected Query getFetchScalarsQuery(String iriName){

    WhereBuilder wb = new WhereBuilder()
        .addPrefix(OCGML, ONTO_CITY_GML)
        .addWhere(NodeFactory.createURI(iriName), QM + PREDICATE, QM + VALUE);
    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + PREDICATE)
        .addVar(QM + VALUE)
        .addGraph(NodeFactory.createURI(getGraphUri(iriName)), wb);
    return sb.build();
  }

  /**
   * builds query to retrieve collection IRIs when graph is provided.
   * @param iriName cityObject IRI.
   * @return query
   */
  protected Query getFetchIrisQuery(String iriName, String wherePredicate, String whereGraph){
    WhereBuilder wb = new WhereBuilder()
        .addPrefix(OCGML, ONTO_CITY_GML)
        .addWhere(QM + COLLECTION_ELEMENT_IRI, wherePredicate, NodeFactory.createURI(iriName));
    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + COLLECTION_ELEMENT_IRI)
        .addGraph(NodeFactory.createURI(whereGraph), wb);

    return sb.build();
  }

  /**
   * builds query to retrieve  collection IRIs when graph is not provided.
   * @param iriName cityObject IRI.
   * @return query
   */
  protected Query getFetchIrisQuery(String iriName, String wherePredicate){

    return getFetchIrisQuery(iriName, wherePredicate,getGraphUri(iriName));
  }

  /**
   * fills in the scalar fields of a CityGML model instance.
   * @param iriName IRI of the model instance.
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
        predicate = predicate.replace(ONTO_CITY_GML,OCGML + ":");
        assignScalarValueByRow(row, fieldMap, predicate);
      }
    }
  }

  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
          throws IllegalAccessException {
    //to implement in subclasses
  }



}