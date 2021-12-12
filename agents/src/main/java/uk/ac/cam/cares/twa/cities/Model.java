package uk.ac.cam.cares.twa.cities;

import java.io.InvalidClassException;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;


public abstract class Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ID) protected URI iri;

  protected static final String GRAPH = "graph";
  protected static final String PREDICATE = "predicate";
  protected static final String VALUE = "value";
  protected static final String ONTO_CITY_GML = ResourceBundle.getBundle("config").getString("uri.ontology.ontocitygml");
  protected static final String OCGML = "ocgml";
  protected static final String QM = "?";

  // Probably there could be a better way to do this?
  // The key of fieldMap is e.g. "surfaceGeometry/cityObjectId/false"
  protected final HashMap<String, FieldPopulator> fieldMap;
  private static HashMap<Class<?>, HashMap<String, FieldPopulator>> fieldMapMap = new HashMap<>();

  public Model() {
    if (fieldMapMap.containsKey(this.getClass())) {
      // If we have mapped this class before, use the cached map.
      fieldMap = fieldMapMap.get(this.getClass());
    } else {
      // Otherwise, build one and cache it.
      fieldMap = new HashMap<>();
      fieldMapMap.put(this.getClass(), fieldMap);
      Class<?> currentClass = this.getClass();
      // Collect fields
      ArrayList<Field> fields = new ArrayList<>();
      do {
        fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
        currentClass = currentClass.getSuperclass();
      } while(currentClass != null);
      // Build fieldMap
      ModelMetadata metadata = this.getClass().getAnnotation(ModelMetadata.class);
      for (Field field : fields) {
        ModelField annotation = field.getAnnotation(ModelField.class);
        if (annotation != null) {
          String predicate = annotation.value().replace(OCGML + ":", "");
          String graph = annotation.graphName();
          if(graph.equals("")) graph = metadata.defaultGraph();
          boolean backward = annotation.backward();
          try {
            fieldMap.put(graph + "/" + predicate + "/" + backward, new FieldPopulator(field));
          } catch (NoSuchMethodException | InvalidClassException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * populates all fields of a CityGML model instance.
   * @param kgClient sends the query to the right endpoint.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  public void populateAll(String iriName, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) {
    for(FieldPopulator populator: fieldMap.values()) populator.clear(this);
    populateFieldsInDirection(iriName, kgClient, true, recursiveInstantiationDepth);
    populateFieldsInDirection(iriName, kgClient, false, recursiveInstantiationDepth);
  }

  /**
   * populates all fields of a CityGML model instance in one direction
   * @param kgClient  sends the query to the right endpoint.
   * @param backward whether iriName is the object or subject in the rows retrieved.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  private void populateFieldsInDirection(String iriName, KnowledgeBaseClientInterface kgClient, boolean backward, int recursiveInstantiationDepth) {
    Query q = getPopulateQuery(iriName, backward);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);
    for (int index = 0; index < queryResult.length(); index++) {
      JSONObject row = queryResult.getJSONObject(index);
      String predicate = row.getString(PREDICATE).replace(ONTO_CITY_GML, "");
      String graph = pop(row.getString(GRAPH), 1);
      String key = graph + "/" + predicate + "/" + backward;
      System.err.println("Reading row: " + graph + "/" + predicate + "/" + backward);
      if (fieldMap.containsKey(key)){
        fieldMap.get(key).populate(this, row, kgClient, recursiveInstantiationDepth);
        try {
          System.err.println(fieldMap.get(key).getter.invoke(this));
        } catch (IllegalAccessException | InvocationTargetException e) {
          e.printStackTrace();
        }
      } else {
        System.err.println("Key not found: " + key);
      }
    }
  }

  /**
   * returns the nth from the last part of the iri.
   * @param iriName object id
   * @param n the index counting from the back to return
   * @return the nth-from-last part of the provided iri.
   */
  protected static String pop(String iriName, int n) {
    String[] splitIri = iriName.split("/");
    if(splitIri[splitIri.length-1].equals("")) n++;
    return splitIri[splitIri.length - n];
  }

  /**
   * returns the namespace an iri is in.
   * @param iriName object id
   * @return namespace as string.
   */
  protected static String getNamespace(String iriName) {
    String[] splitUri = iriName.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2)) + "/";
  }

  /**
   * builds query to get triples linked to a given iri.
   */
  private static Query getPopulateQuery(String iriName, boolean backwards) {
    WhereBuilder wb = null;
    Object self = NodeFactory.createURI(iriName);
    Object value = QM + VALUE;
    Object predicate = QM + PREDICATE;
    Object graph = QM + GRAPH;
    try {
      wb = new WhereBuilder()
          .addPrefix(OCGML, ONTO_CITY_GML)
          .addWhere(backwards ? value : self, predicate, backwards ? self : value)
          .addFilter("!ISBLANK(" + value + ")");
    } catch (ParseException e) {
      e.printStackTrace();
    }
    SelectBuilder sb = new SelectBuilder().addVar(predicate).addVar(value).addVar(graph)
        .addGraph(graph, wb);
    return sb.build();
  }

}
