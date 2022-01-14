package uk.ac.cam.cares.twa.cities;

import java.io.InvalidClassException;
import java.lang.reflect.*;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.update.UpdateRequest;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;

class MetaModel {

  public final String nativeGraph;
  public final Constructor<?> constructor;
  public final Map<FieldKey, FieldInterface> fieldMap;

  public MetaModel(Class<?> target) throws NoSuchMethodException, InvalidClassException {
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
    fieldMap = new TreeMap<>();
    for (Field field : fields) {
      FieldAnnotation annotation = field.getAnnotation(FieldAnnotation.class);
      if (annotation != null) fieldMap.put(new FieldKey(annotation, nativeGraph), new FieldInterface(field));
    }
  }

}

public abstract class Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ID) protected URI iri;

  // SPARQL variable names, exposed so FieldInterface can parse the result rows from our queries
  public static final String GRAPH = "graph";
  public static final String PREDICATE = "predicate";
  public static final String VALUE = "value";
  public static final String DATATYPE = "dtype";
  // Helper constants for constructing SPARQL queries
  private static final String NOT_BLANK = "!ISBLANK";
  private static final String DATATYPE_FUN = "DATATYPE";
  private static final String QM = "?";
  private static final String OPENP = "(";
  private static final String CLOSEP = ")";

  public static UpdateRequest updateQueue = new UpdateRequest();
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
      } catch (NoSuchMethodException | InvalidClassException e) {
        throw new JPSRuntimeException(e);
      }
      metaModelMap.put(thisClass, metaModel);
    }
    clearAll();
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

  public static long totalNanos = 0;
  public static void executeUpdates(StoreClientInterface kgClient, boolean force) {
    String updateString = updateQueue.toString();
    if(force || updateString.length() > 100000) {
      Instant start = Instant.now();
      kgClient.executeUpdate(updateString);
      totalNanos += Duration.between(start, Instant.now()).toNanos();
    }
  }

  /**
   * Queues an update to overwrite all forward properties of this model in the database.
   */
  public void queueAndExecutePushForwardUpdate(StoreClientInterface kgClient) {
    queuePushForwardUpdate();
    executeUpdates(kgClient, false);
  }

  public void queuePushForwardUpdate() {
    Node self = NodeFactory.createURI(getIri().toString());
    int varCount = 0;
    String currentGraphIri = null;
    WhereBuilder currentScalarDeleteSubquery = null;
    WhereBuilder currentInsertSubquery = null;
    UpdateBuilder scalarDeleteQuery = new UpdateBuilder();
    UpdateBuilder insertQuery = new UpdateBuilder();
    Stack<UpdateBuilder> vectorDeleteQueries = new Stack<>();
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.fieldMap.entrySet()) {
      FieldInterface field = entry.getValue();
      FieldKey key = entry.getKey();
      Node predicate = NodeFactory.createURI(key.predicate.toString());
      String graphIri = buildGraphIri(getNamespace(), key.graph);
      Node graph = NodeFactory.createURI(graphIri);
      boolean dirty = cleanCopy == null || !field.equals(this, cleanCopy);
      if (key.backward || !dirty)
        continue;
      if (currentGraphIri == null || !currentGraphIri.equals(graphIri)) {
        currentGraphIri = graphIri;
        currentScalarDeleteSubquery = new WhereBuilder();
        currentInsertSubquery = new WhereBuilder();
        scalarDeleteQuery.addGraph(graph, currentScalarDeleteSubquery);
        insertQuery.addInsert(graph, currentInsertSubquery);
      }
      if (field.isList) {
        UpdateBuilder vectorDeleteQuery = new UpdateBuilder().addGraph(graph,
            new WhereBuilder().addWhere(self, predicate, QM + VALUE));
        vectorDeleteQueries.push( vectorDeleteQuery);
        for (Node value : field.getNodes(this))
          currentInsertSubquery.addWhere(self, predicate, value);
      } else {
        currentScalarDeleteSubquery.addWhere(self, predicate, QM + VALUE + (varCount++));
        currentInsertSubquery.addWhere(self, predicate, field.getNode(this));
      }
    }
    if (currentGraphIri == null) return; // nothing happened
    updateQueue.add(scalarDeleteQuery.buildDeleteWhere());
    while(!vectorDeleteQueries.isEmpty())
      updateQueue.add(vectorDeleteQueries.pop().buildDeleteWhere());
    updateQueue.add(insertQuery.build());
  }

  /**
   * Queues an update to overwrite entirely delete this object from the database, including all triples which have its
   * iri as subject or object.
   */
  public void queueAndExecuteDeletionUpdate(StoreClientInterface kgClient) {
    queueDeletionUpdate();
    executeUpdates(kgClient, false);
  }

  public void queueDeletionUpdate() {
    Node self = NodeFactory.createURI(getIri().toString());
    updateQueue.add(new UpdateBuilder().addWhere(QM + VALUE, QM + PREDICATE, self).buildDeleteWhere());
    updateQueue.add(new UpdateBuilder().addWhere(self, QM + PREDICATE, QM + VALUE).buildDeleteWhere());
  }

  /**
   * Clears all field of the model instance
   */
  public void clearAll() {
    for (FieldInterface field : metaModel.fieldMap.values())
      field.clear(this);
  }

  /**
   * Populates all fields of the model instance.
   * @param kgClient                    sends the query to the right endpoint.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  public void pullAll(String iriName, StoreClientInterface kgClient, int recursiveInstantiationDepth) {
    clearAll();
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
  private void populateFieldsInDirection(String iriName, StoreClientInterface kgClient, boolean backward, int recursiveInstantiationDepth)
      throws InvocationTargetException, IllegalAccessException {
    Query q = buildPopulateQuery(iriName, backward);
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
   * Returns the namespace an iri is in, including a trailing slash
   * @param iriName object id
   * @return namespace as string
   */
  public static String getNamespace(String iriName) {
    // Note that a trailing slash is automatically ignored, i.e. .../abc/ will split with a last element "abc".
    String[] splitUri = iriName.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2)) + "/";
  }

  /**
   * Returns the namespace of a model, including a trailing slash
   * @return namespace as string
   */
  public String getNamespace() {
    return getNamespace(getIri().toString());
  }

  /**
   * Builds a graph iri, including a trailing slash, from a given namespace and graph name.
   * Equivalent to namespace + graphName + "/".
   * @param namespace the namespace iri.
   * @param graphName the short name of the graph, e.g. "surfacegeometry".
   * @return graph iri as string
   */
  public static String buildGraphIri(String namespace, String graphName) {
    return namespace + graphName + "/";
  }

  /**
   * Builds query to get triples linked to a given iri.
   */
  public static Query buildPopulateQuery(String iriName, boolean backwards) {
    SelectBuilder sb = null;
    try {
      WhereBuilder wb = new WhereBuilder()
          .addWhere(backwards ? (QM + VALUE) : NodeFactory.createURI(iriName),
              QM + PREDICATE, backwards ? NodeFactory.createURI(iriName) : (QM + VALUE))
          .addFilter(NOT_BLANK + OPENP + QM + VALUE + CLOSEP);
      sb = new SelectBuilder()
          .addVar(QM + PREDICATE).addVar(QM + VALUE).addVar(QM + GRAPH).addVar(QM + DATATYPE)
          .addBind(DATATYPE_FUN + OPENP + QM + VALUE + CLOSEP, QM + DATATYPE)
          .addGraph(QM + GRAPH, wb);
      return sb.build();
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

}
