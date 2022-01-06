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
  public final HashMap<FieldKey, FieldInterface> fieldMap;

  public MetaModel (Class<?> target) throws NoSuchMethodException, InvalidClassException {
    // Miscellaneous useful data
    constructor = target.getConstructor();
    ModelAnnotation metadata = target.getAnnotation(ModelAnnotation.class);
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
      FieldAnnotation annotation = field.getAnnotation(FieldAnnotation.class);
      if (annotation != null) fieldMap.put(new FieldKey(annotation, nativeGraph), new FieldInterface(field));
    }
  }

}

public abstract class Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ID) protected URI iri;

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
    ModelAnnotation metadata = this.getClass().getAnnotation(ModelAnnotation.class);
    if (!namespace.endsWith("/")) namespace = namespace + "/";
    setIri(URI.create(namespace + metadata.nativeGraph() + "/" + UUID));
  }

  public static void executeQueuedUpdates(KnowledgeBaseClientInterface kgClient) {
    kgClient.executeUpdate(PrefixUtils.insertPrefixStatements(updateQueue.toString()));
    updateQueue = new StringBuilder();
  }

  /**
   * Queues an update to overwrite all forward properties of this model in the database.
   */
  public void queuePushForward(KnowledgeBaseClientInterface kgClient) {
    String self = getIri().toString();
    // scalars
    int varCount = 0;
    StringBuilder scalarDeleteString = new StringBuilder("DELETE WHERE { \n");
    StringBuilder vectorDeleteString = new StringBuilder("");
    StringBuilder insertString = new StringBuilder("INSERT DATA {  \n");
    String currentGraph = null;
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.fieldMap.entrySet()) {
      FieldInterface field = entry.getValue();
      FieldKey key = entry.getKey();
      String predicate = key.predicate.toString();
      String graph = buildGraphIri(getIri(), key.graph);
      boolean dirty = cleanCopy == null || !field.equals(this, cleanCopy);
      if (key.backward || !dirty)
        continue;
      if (currentGraph == null || !currentGraph.equals(graph)) {
        if (currentGraph != null) {
          scalarDeleteString.append("    }\n");
          insertString.append("    }\n");
        }
        currentGraph = graph;
        scalarDeleteString.append(String.format("    GRAPH <%s> { <%s> \n", graph, self));
        insertString.append(String.format("    GRAPH <%s> { <%s> \n", graph, self));
      }
      if (field.isList) {
        vectorDeleteString.append(String.format("DELETE WHERE { GRAPH <%s> { <%s> <%s> ?v} } \n", graph, self, predicate));
        for(String literalString: field.getLiterals(this))
          insertString.append(String.format("        <%s> %s;   \n", predicate, literalString));
      } else {
        scalarDeleteString.append(String.format("        <%s> ?v%d; \n", predicate, varCount));
        insertString.append(String.format("        <%s> %s;   \n", predicate, field.getLiteral(this)));
        varCount++;
      }
    }
    scalarDeleteString.append("} };\n");
    insertString.append("} };\n");
    updateQueue.append(scalarDeleteString).append(vectorDeleteString).append(insertString);
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
      URI predicate = URI.create(row.getString(PREDICATE));
      URI graph = URI.create(row.getString(GRAPH));
      FieldInterface field = metaModel.fieldMap.get(new FieldKey(graph, predicate, backward));
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
