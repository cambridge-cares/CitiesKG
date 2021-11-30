package uk.ac.cam.cares.twa.cities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;

public class Model {

  protected static final String PREDICATE = "predicate";
  protected static final String VALUE =  "value";
  protected static final String COLLECTION_ELEMENT_IRI = "CollectionElementIri";
  protected static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
  protected static final String OCGML = "ocgml";
  protected static final String QM = "?";

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
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-1));
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


}
