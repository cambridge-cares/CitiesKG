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
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

class MetaModel {

  public final String nativeGraph;
  public final Constructor<?> constructor;
  public final HashMap<String, FieldInterface> fieldMap;

  public MetaModel (Class<?> target) throws NoSuchMethodException, InvalidClassException {
    // Miscellaneous useful data
    constructor = target.getConstructor();
    ModelMetadata metadata = target.getAnnotation(ModelMetadata.class);
    nativeGraph = metadata.nativeGraph();
    // Collect fields
    Class<?> currentClass = target;
    ArrayList<Field> fields = new ArrayList<>();
    do {
      fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    } while (currentClass != null);
    // Build fieldMap
    fieldMap = new HashMap<>();
    for (Field field : fields) {
      ModelField annotation = field.getAnnotation(ModelField.class);
      if (annotation != null) {
        String predicate = PrefixUtils.expandQualifiedName(annotation.value());
        String graph = annotation.graphName().equals("") ? nativeGraph : annotation.graphName();
        boolean backward = annotation.backward();
        // Keys have format e.g.
        // surfacegeometry|http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#|true
        // The predicate is expanded but the graph is not because the full graph iri is target namespace-dependent
        // and will not be the same across different applications, while the full predicate iri does not change.
        fieldMap.put(graph + "|" + predicate + "|" + backward, new FieldInterface(field));
      }
    }
  }

}

public abstract class Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ID) protected URI iri;

  protected static final String GRAPH = "graph";
  protected static final String PREDICATE = "predicate";
  protected static final String VALUE = "value";
  protected static final String DATATYPE = "datatype";
  private static StringBuilder updateQueue = new StringBuilder();
  private final static HashMap<Class<?>, MetaModel> metaModelMap = new HashMap<>();

  private Model cleanCopy;
  protected final MetaModel metaModel;

  public Model() {
    Class<?> thisClass = this.getClass();
    if (metaModelMap.containsKey(thisClass)) {
      metaModel = metaModelMap.get(thisClass);
    } else {
      try {
        metaModel = new MetaModel(thisClass);
      } catch (NoSuchMethodException | InvalidClassException e){
        throw new JPSRuntimeException(e);
      }
      metaModelMap.put(thisClass, metaModel);
    }
  }

  /**
   * Sets the object's iri from the specified parameters, retrieving the appropriate graph name from class metadata.
   * The IRI will have the form [namespace]/[graph]/[UUID]
   * @param UUID      the UUID of the object, which will become the last part of the iri; typically the same as gmlId.
   * @param namespace the namespace of the iri.
   */
  public void setIri(String UUID, String namespace) {
    ModelMetadata metadata = this.getClass().getAnnotation(ModelMetadata.class);
    if (!namespace.endsWith("/")) namespace = namespace + "/";
    setIri(URI.create(namespace + metadata.nativeGraph() + "/" + UUID));
  }

  public static void executeQueuedUpdates(KnowledgeBaseClientInterface kgClient) {
    kgClient.executeUpdate(PrefixUtils.insertPrefixStatements(updateQueue.toString()));
    updateQueue = new StringBuilder();
  }

  /**
   * Queues an update to overwrite all forward, scalar properties of this model in the database. I am pretty sure that
   * all triples are (forward) scalar from one of the two directions, but you may want to check before relying on that.
   */
  public void queuePushForwardScalars(KnowledgeBaseClientInterface kgClient) {
    String self = getIri().toString();
    StringBuilder deleteString = new StringBuilder("DELETE WHERE { \n");
    StringBuilder insertString = new StringBuilder("INSERT DATA {  \n");
    int varCount = 0;
    String currentGraph = null;
    for (Map.Entry<String, FieldInterface> entry : metaModel.fieldMap.entrySet()) {
      FieldInterface field = entry.getValue();
      String[] key = entry.getKey().split("\\|"); // this is "|" but escaping the | because it's parsed as regex
      String graph = buildGraphIri(getIri(), key[0]);
      String predicate = key[1];
      boolean backward = Boolean.parseBoolean(key[2]);
      boolean dirty = cleanCopy == null || !field.equals(this, cleanCopy);
      if (field.isList || backward || !dirty)
        continue;
      if (currentGraph == null || !currentGraph.equals(graph)) {
        if (currentGraph != null) {
          deleteString.append("    }\n");
          insertString.append("    }\n");
        }
        currentGraph = graph;
        deleteString.append(String.format("    GRAPH <%s> { <%s> \n", graph, self));
        insertString.append(String.format("    GRAPH <%s> { <%s> \n", graph, self));
      }
      deleteString.append(String.format("        <%s> ?v%d; \n", predicate, varCount));
      insertString.append(String.format("        <%s> %s;   \n", predicate, field.getLiteral(this)));
      varCount++;
    }
    deleteString.append("} };\n");
    insertString.append("} };\n");
    updateQueue.append(deleteString).append(insertString);
    if (updateQueue.length() > 100000) executeQueuedUpdates(kgClient);
  }

  /**
   * Queues an update to overwrite entirely delete this object from the database, including all triples which have its
   * iri as subject or object.
   */
  public void queueDeleteInstantiation(KnowledgeBaseClientInterface kgClient) {
    String self = getIri().toString();
    updateQueue.append(String.format("DELETE WHERE { ?a ?b <%s> }; DELETE WHERE { <%s> ?a ?b };\n", self, self));
    if (updateQueue.length() > 100000) executeQueuedUpdates(kgClient);
  }

  /**
   * Populates all fields of a CityGML model instance.
   * @param kgClient                    sends the query to the right endpoint.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  public void pullAll(String iriName, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) {
    for (FieldInterface field : metaModel.fieldMap.values()) field.clear(this);
    try {
      cleanCopy = (Model) metaModel.constructor.newInstance();
      populateFieldsInDirection(iriName, kgClient, true, recursiveInstantiationDepth);
      populateFieldsInDirection(iriName, kgClient, false, recursiveInstantiationDepth);
    } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
      e.printStackTrace();
    }
  }

  /**
   * Populates all fields of a CityGML model instance in one direction
   * @param kgClient                    sends the query to the right endpoint.
   * @param backward                    whether iriName is the object or subject in the rows retrieved.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  private void populateFieldsInDirection(String iriName, KnowledgeBaseClientInterface kgClient, boolean backward, int recursiveInstantiationDepth)
      throws InvocationTargetException, IllegalAccessException {
    Query q = getPopulateQuery(iriName, backward);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);
    for (int index = 0; index < queryResult.length(); index++) {
      JSONObject row = queryResult.getJSONObject(index);
      String predicate = row.getString(PREDICATE);
      String[] splitGraph = row.getString(GRAPH).split("/");
      String graph = splitGraph[splitGraph.length-1];
      String key = graph + "|" + predicate + "|" + backward;
      FieldInterface field = metaModel.fieldMap.get(key);
      if (field != null) {
        field.pull(this, row, kgClient, recursiveInstantiationDepth);
        field.copy(this, cleanCopy);
      }
    }
  }

  /**
   * Returns the namespace an iri is in.
   * @param iriName object id
   * @return namespace as string
   */
  public static String getNamespace(String iriName) {
    // Note that a trailing slash is automatically ignored, i.e. .../abc/ will split with a last element "abc".
    String[] splitUri = iriName.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2)) + "/";
  }

  /**
   * Builds the iri of a named graph, taking another existing iri as a reference for the namespace.
   * @param namespaceReferenceIri a URI from which to take the namespace
   * @param graphName             the short name of the graph, e.g. "surfacegeometry"
   * @return graph iri as string
   */
  public static String buildGraphIri(URI namespaceReferenceIri, String graphName) {
    return buildGraphIri(namespaceReferenceIri.toString(), graphName);
  }

  public static String buildGraphIri(String namespaceReferenceIriName, String graphName) {
    return getNamespace(namespaceReferenceIriName) + graphName + "/";
  }

  /**
   * Builds query to get triples linked to a given iri.
   */
  private static Query getPopulateQuery(String iriName, boolean backwards) {
    SelectBuilder sb = null;
    Node self = NodeFactory.createURI(iriName);
    Node value = NodeFactory.createVariable(VALUE);
    Node predicate = NodeFactory.createVariable(PREDICATE);
    Node graph = NodeFactory.createVariable(GRAPH);
    Node datatype = NodeFactory.createVariable(DATATYPE);
    try {
      WhereBuilder wb = new WhereBuilder()
          .addWhere(backwards ? value : self, predicate, backwards ? self : value)
          .addFilter("!ISBLANK(?" + VALUE + ")");
      sb = new SelectBuilder()
          .addVar(predicate).addVar(value).addVar(graph).addVar(datatype)
          .addBind("DATATYPE(?" + VALUE + ")", datatype)
          .addGraph(graph, wb);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return sb.build();
  }

}
