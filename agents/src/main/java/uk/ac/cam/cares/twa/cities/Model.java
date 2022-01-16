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

public abstract class Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ID) protected URI iri;

  // SPARQL variable names
  private static final String GRAPH = "graph";
  private static final String PREDICATE = "predicate";
  private static final String VALUE = "value";
  private static final String DATATYPE = "datatype";
  // Helper constants for constructing SPARQL queries
  private static final String NOT_BLANK = "!ISBLANK";
  private static final String DATATYPE_FUN = "DATATYPE";
  private static final String QM = "?";
  private static final String OP = "(";
  private static final String CP = ")";
  // Error text
  private static final String OBJECT_NOT_FOUND_EXCEPTION_TEXT = "Object not found in database.";

  // Resources for batched execution of updates
  private static UpdateRequest updateQueue = new UpdateRequest();
  public static long cumulativeUpdateExecutionNanoseconds = 0;

  // Lookup for previously constructed MetaModel.
  private final static HashMap<Class<?>, MetaModel> metaModelMap = new HashMap<>();
  // The MetaModel of this object, which provides FieldInterfaces for interacting with data.
  private final MetaModel metaModel;

  // Minimised representations of the original values of fields from the last pull from the database.
  // These are ordered in the same order as metaModel.scalarFieldList and metaModel.vectorFieldList.
  private Object[] originalFieldValues;

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
    dirtyAll();
  }

  /**
   * Clears all field of the model instance
   */
  public void clearAll() {
    for (FieldInterface field : metaModel.fieldMap.values())
      field.clear(this);
    originalFieldValues = new Object[metaModel.fieldMap.size()];
  }

  /**
   * Makes all fields of the model instance dirty
   */
  public void dirtyAll() {
    // Object.class is a placeholder which != any valid return of FieldInterface.getMinimised.
    Arrays.fill(originalFieldValues, Object.class);
  }

  /**
   * Makes all fields of the model instance clean
   */
  public void cleanAll() {
    for(FieldInterface field: metaModel.fieldMap.values())
      originalFieldValues[field.index] = field.getMinimised(this);
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

  /**
   * Executes queued updates in the updateQueue
   * @param kgClient the client to execute updates with.
   * @param force    if false, the updates will only be executed if their cumulative length is >250000 characters.
   */
  public static void executeUpdates(StoreClientInterface kgClient, boolean force) {
    String updateString = updateQueue.toString();
    if (force || updateString.length() > 250000) {
      Instant start = Instant.now();
      kgClient.executeUpdate(updateString);
      cumulativeUpdateExecutionNanoseconds += Duration.between(start, Instant.now()).toNanos();
      clearUpdateQueue();
    }
  }

  /**
   * Clears queued updates without executing.
   */
  public static void clearUpdateQueue() {
    updateQueue = new UpdateRequest();
  }

  /**
   * Queues an update to push properties of this model to the database.
   * @param pushForward  whether to push forward properties.
   * @param pushBackward whether to push backward properties.
   */
  public void queuePushUpdate(boolean pushForward, boolean pushBackward) {
    Node self = NodeFactory.createURI(getIri().toString());
    int varCount = 0;
    Node graph = null;
    WhereBuilder currentScalarDeleteSubquery = null;
    WhereBuilder currentInsertSubquery = null;
    UpdateBuilder scalarDeleteQuery = new UpdateBuilder();
    UpdateBuilder insertQuery = new UpdateBuilder();
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.fieldMap.entrySet()) {
      FieldInterface field = entry.getValue();
      FieldKey key = entry.getKey();
      // Filter for direction and whether the field is dirty.
      boolean directionSupported = ((!key.backward && pushForward) || (key.backward && pushBackward));
      boolean clean = Objects.equals(field.getMinimised(this), originalFieldValues[field.index]);
      if (clean || !directionSupported) continue;
      // If the graph changes, break new subgraphs for insert and scalar delete. Sorted keys make this more efficient.
      if (graph == null || !graph.getURI().equals(buildGraphIri(key.graph))) {
        graph = NodeFactory.createURI(buildGraphIri(key.graph));
        scalarDeleteQuery.addGraph(graph, currentScalarDeleteSubquery = new WhereBuilder());
        insertQuery.addInsert(graph, currentInsertSubquery = new WhereBuilder());
      }
      // Add updates to queue
      Node predicate = NodeFactory.createURI(key.predicate);
      if (field.isList) {
        String valueVar = QM + VALUE;
        WhereBuilder where = new WhereBuilder().addWhere(key.backward ? valueVar : self, predicate, key.backward ? self : valueVar);
        updateQueue.add(new UpdateBuilder().addGraph(graph, where).buildDeleteWhere());
        for (Node valueValue : field.getNodes(this))
          currentInsertSubquery.addWhere(key.backward ? valueValue : self, predicate, key.backward ? self : valueValue);
      } else {
        String valueVar = QM + VALUE + (varCount++);
        Node valueValue = field.getNode(this);
        currentScalarDeleteSubquery.addWhere(key.backward ? valueVar : self, predicate, key.backward ? self : valueVar);
        currentInsertSubquery.addWhere(key.backward ? valueValue : self, predicate, key.backward ? self : valueValue);
      }
    }
    if (graph == null) return; // nothing happened
    cleanAll();
    updateQueue.add(scalarDeleteQuery.buildDeleteWhere());
    updateQueue.add(insertQuery.build());
  }

  /**
   * Queues an update to overwrite entirely delete this object from the database, including all triples which have its
   * iri as subject or object.
   */
  public void queueDeletionUpdate() {
    Node self = NodeFactory.createURI(getIri().toString());
    updateQueue.add(new UpdateBuilder().addWhere(QM + VALUE, QM + PREDICATE, self).buildDeleteWhere());
    updateQueue.add(new UpdateBuilder().addWhere(self, QM + PREDICATE, QM + VALUE).buildDeleteWhere());
    dirtyAll();
  }

  /**
   * Populates all fields of the model instance with values from the database.
   * @param kgClient                    the client to execute queries with.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  public void pullAll(StoreClientInterface kgClient, int recursiveInstantiationDepth) {
    URI iri = getIri();
    clearAll();
    setIri(iri);
    pullAllInDirection(kgClient, true, recursiveInstantiationDepth);
    pullAllInDirection(kgClient, false, recursiveInstantiationDepth);
    cleanAll();
  }

  /**
   * Populates all fields in one direction with values from the database.
   * @param kgClient                    the client to execute queries with.
   * @param backward                    whether iriName is the object or subject in the rows retrieved.
   * @param recursiveInstantiationDepth the number of levels into the model hierarchy below this to instantiate.
   */
  private void pullAllInDirection(StoreClientInterface kgClient, boolean backward, int recursiveInstantiationDepth) {
    Query q = buildPullAllInDirectionQuery(getIri().toString(), backward);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);
    for (int index = 0; index < queryResult.length(); index++) {
      JSONObject row = queryResult.getJSONObject(index);
      FieldInterface field = metaModel.fieldMap.get(new FieldKey(row.getString(GRAPH), row.getString(PREDICATE), backward));
      if (field != null) {
        String valueString = row.getString(VALUE);
        String datatypeString = row.optString(DATATYPE);
        field.put(this, valueString, datatypeString, kgClient, recursiveInstantiationDepth);
      }
    }
  }

  /**
   * Composes a query to pull all triples relating to this Model instance
   * @param iriName   the iri from which to pull data.
   * @param backwards whether to query quads with iriName as the object (true) or subject (false).
   * @return the composed query.
   */
  public static Query buildPullAllInDirectionQuery(String iriName, boolean backwards) {
    try {
      WhereBuilder wb = new WhereBuilder()
          .addWhere(backwards ? (QM + VALUE) : NodeFactory.createURI(iriName),
              QM + PREDICATE, backwards ? NodeFactory.createURI(iriName) : (QM + VALUE))
          .addFilter(NOT_BLANK + OP + QM + VALUE + CP);
      SelectBuilder sb = new SelectBuilder()
          .addVar(QM + PREDICATE).addVar(QM + VALUE).addVar(QM + GRAPH).addVar(QM + DATATYPE)
          .addBind(DATATYPE_FUN + OP + QM + VALUE + CP, QM + DATATYPE)
          .addGraph(QM + GRAPH, wb);
      return sb.build();
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Populates all scalar fields with values from the database.
   * @param kgClient the client to execute queries with.
   */
  public void pullScalars(StoreClientInterface kgClient) {
    JSONArray scalarResponse = new JSONArray(kgClient.execute(buildScalarsQuery().buildString()));
    if (scalarResponse.length() == 0) {
      throw new JPSRuntimeException(OBJECT_NOT_FOUND_EXCEPTION_TEXT);
    }
    JSONObject row = scalarResponse.getJSONObject(0);
    for (int i = 0; i < metaModel.scalarFieldList.size(); i++) {
      if (row.has(VALUE + i)) {
        FieldInterface field = metaModel.scalarFieldList.get(i).getValue();
        field.put(this, row.getString(VALUE + i), row.optString(DATATYPE + i), kgClient, 0);
        originalFieldValues[field.index] = field.getMinimised(this);
      }
    }
  }

  /**
   * Composes a query to retrieve the values for all scalar fields of this Model instance.
   * @return the composed query.
   */
  public SelectBuilder buildScalarsQuery() {
    try {
      Node self = NodeFactory.createURI(getIri().toString());
      int varCount = 0;
      Node graph = null;
      WhereBuilder currentGraphSubquery = null;
      SelectBuilder query = new SelectBuilder();
      for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.scalarFieldList) {
        FieldKey key = entry.getKey();
        if (graph == null || !Objects.equals(graph.getURI(), buildGraphIri(key.graph))) {
          graph = NodeFactory.createURI(buildGraphIri(key.graph));
          currentGraphSubquery = new WhereBuilder();
          query.addGraph(graph, currentGraphSubquery);
        }
        Node predicate = NodeFactory.createURI(key.predicate);
        String valueN = QM + VALUE + varCount;
        String datatypeN = QM + DATATYPE + varCount;
        currentGraphSubquery
            .addOptional(new SelectBuilder()
                .addWhere(key.backward ? valueN : self, predicate, key.backward ? self : valueN)
                .addFilter(NOT_BLANK + OP + valueN + CP));
        query.addVar(valueN).addVar(datatypeN).addBind(DATATYPE_FUN + OP + valueN + CP, datatypeN);
        varCount++;
      }
      return query;
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Populates the named vector fields with values from the database.
   * @param fieldNames the list of vector field names to be populated.
   * @param kgClient the client to execute queries with.
   */
  public void pullVector(List<String> fieldNames, StoreClientInterface kgClient) {
    // Populate vectors
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
      if (fieldNames.contains(entry.getValue().field.getName())) {
        FieldInterface field = entry.getValue();
        JSONArray response = new JSONArray(kgClient.execute(buildVectorQuery(entry.getKey()).buildString()));
        for (int i = 0; i < response.length(); i++) {
          JSONObject row = response.getJSONObject(i);
          field.put(this, row.getString(VALUE), row.optString(DATATYPE), kgClient, 0);
        }
        originalFieldValues[field.index] = field.getMinimised(this);
      }
    }
  }

  /**
   * Composes a query for all matches of a property specification described by a FieldKey, for this Model instance.
   * @param key the FieldKey describing the property to be queried.
   * @return the composed query.
   */
  public SelectBuilder buildVectorQuery(FieldKey key) {
    Node predicate = NodeFactory.createURI(key.predicate);
    Node graph = NodeFactory.createURI(buildGraphIri(key.graph));
    Node self = NodeFactory.createURI(getIri().toString());
    try {
      return new SelectBuilder()
          .addVar(QM + VALUE).addVar(QM + DATATYPE)
          .addBind(DATATYPE_FUN + OP + QM + VALUE + CP, QM + DATATYPE)
          .addGraph(graph, new WhereBuilder()
              .addWhere(key.backward ? (QM + VALUE) : self, predicate, key.backward ? self : (QM + VALUE)));
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Equality is defined as all fields matching. The cleanCopy does not have to match.
   * @param o the object to compare against.
   * @return whether the objects are equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    for (FieldInterface field : metaModel.fieldMap.values())
      if (!field.equals(this, o))
        return false;
    return true;
  }

  /**
   * Returns the namespace an iri is in, including a trailing slash.
   * @param iri object iri.
   * @return namespace as string.
   */
  public static String getNamespace(String iri) {
    // Note that a trailing slash is automatically ignored, i.e. .../abc/ will split with a last element "abc".
    String[] splitUri = iri.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2)) + "/";
  }

  /**
   * Returns the namespace of a model, including a trailing slash.
   * @return namespace as string.
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
   * Builds a graph iri, including a trailing slash, from a graph name, using this model's namespace.
   * @param graphName the short name of the graph, e.g. "surfacegeometry".
   * @return graph iri as string
   */
  public String buildGraphIri(String graphName) {
    return buildGraphIri(getNamespace(), graphName);
  }

}
