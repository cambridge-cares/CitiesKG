package uk.ac.cam.cares.twa.cities.models;

import java.io.InvalidClassException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;

/**
 * {@link Model} is the abstract base for classes which represent objects in Blazegraph. It implements a number of
 * methods which push data to and pull data from Blazegraph, and also maintains automatic dirty tracking of field values
 * to only write updates for modified fields on push. Also see {@link MetaModel}.
 * <p>
 * Subclasses of {@link Model} should annotate fields meant for database read-write with {@link Getter},
 * {@link Setter} and {@link FieldAnnotation}.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
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
  private static final ThreadLocal<UpdateRequest> updateQueue = ThreadLocal.withInitial(UpdateRequest::new);

  // Lookup for previously constructed MetaModel.
  private final static ConcurrentHashMap<Class<?>, MetaModel> metaModelMap = new ConcurrentHashMap<>();
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
   * Clears all field of the instance
   */
  public void clearAll() {
    for (FieldInterface field : metaModel.fieldMap.values())
      field.clear(this);
    originalFieldValues = new Object[metaModel.fieldMap.size()];
  }

  /**
   * Makes all fields of the instance dirty
   */
  public void dirtyAll() {
    // Object.class is a placeholder which != any valid return of FieldInterface.getMinimised.
    Arrays.fill(originalFieldValues, Object.class);
  }

  /**
   * Makes all fields of the instance clean
   */
  public void cleanAll() {
    for (FieldInterface field : metaModel.fieldMap.values())
      originalFieldValues[field.index] = field.getMinimised(this);
  }

  /**
   * Sets the object's IRI from the specified parameters, retrieving the appropriate graph name from class metadata.
   * The IRI will have the form <code>namespace</code>/<code>namespace</code>/<code>UUID</code>.
   * @param UUID      the UUID of the object, which will become the last part of the IRI; typically the same as gmlId.
   * @param namespace the namespace of the IRI.
   */
  public void setIri(String UUID, String namespace) {
    ModelAnnotation metadata = this.getClass().getAnnotation(ModelAnnotation.class);
    if (!namespace.endsWith("/")) namespace = namespace + "/";
    setIri(URI.create(namespace + metadata.nativeGraphName() + "/" + UUID));
  }

  /**
   * Executes queued updates in the update queue
   * @param kgId  the resource ID of the target knowledge graph to query.
   * @param force if false, the updates will only be executed if their cumulative length is >250000 characters.
   */
  public static void executeUpdates(String kgId, boolean force) {
    String updateString = updateQueue.get().toString();
    if (force || updateString.length() > 250000) {
      AccessAgentCaller.update(kgId, updateString);
      clearUpdateQueue();
    }
  }

  /**
   * Clears queued updates without executing.
   */
  public static void clearUpdateQueue() {
    updateQueue.set(new UpdateRequest());
  }

  /**
   * Queues an update to push field values to the database.
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
      if (graph == null || !graph.getURI().equals(buildGraphIri(key.graphName))) {
        graph = NodeFactory.createURI(buildGraphIri(key.graphName));
        scalarDeleteQuery.addGraph(graph, currentScalarDeleteSubquery = new WhereBuilder());
        insertQuery.addInsert(graph, currentInsertSubquery = new WhereBuilder());
      }
      // Add updates to queue
      Node predicate = NodeFactory.createURI(key.predicate);
      if (field.isList) {
        String valueVar = QM + VALUE;
        WhereBuilder where = new WhereBuilder().addWhere(key.backward ? valueVar : self, predicate, key.backward ? self : valueVar);
        updateQueue.get().add(new UpdateBuilder().addGraph(graph, where).buildDeleteWhere());
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
    updateQueue.get().add(scalarDeleteQuery.buildDeleteWhere());
    updateQueue.get().add(insertQuery.build());
  }

  /**
   * Queues an update to overwrite entirely delete this object from the database, including all triples which have its
   * IRI as subject or object, not only those described by the fields of the class.
   */
  public void queueDeletionUpdate() {
    Node self = NodeFactory.createURI(getIri().toString());
    updateQueue.get().add(new UpdateBuilder().addWhere(QM + VALUE, QM + PREDICATE, self).buildDeleteWhere());
    updateQueue.get().add(new UpdateBuilder().addWhere(self, QM + PREDICATE, QM + VALUE).buildDeleteWhere());
    dirtyAll();
  }

  /**
   * Populates all fields of the model instance with values from the database. In general, this performs better than
   * <code>pullScalars</code> and <code>pullVector</code>, and should be used for most use cases. The use cases for
   * <code>pullScalars</code> and <code>pullVector</code> outlined in their respective descriptions.
   * @param kgId  the resource ID of the target knowledge graph to query.
   * @param recursiveInstantiationDepth the number of nested levels of {@link Model}s within this to instantiate.
   */
  public void pullAll(String kgId, int recursiveInstantiationDepth) {
    URI iri = getIri();
    clearAll();
    setIri(iri);
    pullAllInDirection(kgId, true, recursiveInstantiationDepth);
    pullAllInDirection(kgId, false, recursiveInstantiationDepth);
    cleanAll();
  }

  /**
   * Populates all fields in one direction with values from the database.
   * @param kgId  the resource ID of the target knowledge graph to query.
   * @param backward                    whether iriName is the object or subject in the rows retrieved.
   * @param recursiveInstantiationDepth the number of nested levels of {@link Model}s within this to instantiate.
   */
  private void pullAllInDirection(String kgId, boolean backward, int recursiveInstantiationDepth) {
    Query query = buildPullAllInDirectionQuery(getIri().toString(), backward);
    String queryResultString = AccessAgentCaller.query(kgId, query.toString());
    JSONArray queryResult = SPARQLUtils.unpackQueryResponse(queryResultString);
    for (int index = 0; index < queryResult.length(); index++) {
      JSONObject row = queryResult.getJSONObject(index);
      FieldInterface field = metaModel.fieldMap.get(new FieldKey(row.getString(GRAPH), row.getString(PREDICATE), backward));
      if (field != null) {
        String valueString = row.getString(VALUE);
        String datatypeString = row.optString(DATATYPE);
        field.put(this, valueString, datatypeString, kgId, recursiveInstantiationDepth);
      }
    }
  }

  /**
   * Composes a query to pull all triples relating to this instance
   * @param iriName   the IRI from which to pull data.
   * @param backwards whether to query quads with <code>iriName</code> as the object (true) or subject (false).
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
   * Populates all scalar fields with values from the database. Usually less performant than <code>pullAll</code> due to
   * the additional complexity of the query required to achieve specificity. It should only be used for objects with a
   * very large number of vector triples. <b>Warning: this can fail due to "Request header is too large" in the current
   * AccessAgent implementation.</b>
   * @param kgId  the resource ID of the target knowledge graph to query.
   */
  @Deprecated
  public void pullScalars(String kgId) {
    String responseString = AccessAgentCaller.query(kgId, buildScalarsQuery().buildString());
    JSONArray scalarResponse = SPARQLUtils.unpackQueryResponse(responseString);
    if (scalarResponse.length() == 0) {
      throw new JPSRuntimeException(OBJECT_NOT_FOUND_EXCEPTION_TEXT);
    }
    JSONObject row = scalarResponse.getJSONObject(0);
    for (int i = 0; i < metaModel.scalarFieldList.size(); i++) {
      if (row.has(VALUE + i)) {
        FieldInterface field = metaModel.scalarFieldList.get(i).getValue();
        field.put(this, row.getString(VALUE + i), row.optString(DATATYPE + i), kgId, 0);
        originalFieldValues[field.index] = field.getMinimised(this);
      }
    }
  }

  /**
   * Composes a query to retrieve the values for all scalar fields of this instance.
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
        if (graph == null || !Objects.equals(graph.getURI(), buildGraphIri(key.graphName))) {
          graph = NodeFactory.createURI(buildGraphIri(key.graphName));
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
   * Populates the named vector fields with values from the database. Usually less performant than <code>pullAll</code>
   * due to the additional complexity of the query required to achieve specificity. It should only be used for objects
   * with a very large number of triples not in the requested vectors.
   * @param fieldNames the list of vector field names to be populated.
   * @param kgId  the resource ID of the target knowledge graph to query.
   */
  public void pullVector(List<String> fieldNames, String kgId) {
    // Populate vectors
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
      if (fieldNames.contains(entry.getValue().field.getName())) {
        FieldInterface field = entry.getValue();
        String responseString = AccessAgentCaller.query(kgId, buildVectorQuery(entry.getKey()).buildString());
        JSONArray response = SPARQLUtils.unpackQueryResponse(responseString);
        for (int i = 0; i < response.length(); i++) {
          JSONObject row = response.getJSONObject(i);
          field.put(this, row.getString(VALUE), row.optString(DATATYPE), kgId, 0);
        }
        originalFieldValues[field.index] = field.getMinimised(this);
      }
    }
  }

  /**
   * Composes a query for all matches of a property described by a {@link FieldKey}, for this instance.
   * @param key the {@link FieldKey} describing the property to be queried.
   * @return the composed query.
   */
  public SelectBuilder buildVectorQuery(FieldKey key) {
    Node predicate = NodeFactory.createURI(key.predicate);
    Node graph = NodeFactory.createURI(buildGraphIri(key.graphName));
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
   * Equality is defined as all {@link Model}-managed fields matching. <code>originalFieldValues</code> and fields
   * without {@link FieldAnnotation} annotations do not have to match.
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
   * Returns the namespace an IRI is in, including a trailing slash.
   * @param iri object IRI.
   * @return namespace as string.
   */
  public static String getNamespace(String iri) {
    // Note that a trailing slash is automatically ignored, i.e. .../abc/ will split with a last element "abc".
    String[] splitUri = iri.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2)) + "/";
  }

  /**
   * Returns the namespace of the {@link Model}'s IRI, including a trailing slash.
   * @return namespace as string.
   */
  public String getNamespace() {
    return getNamespace(getIri().toString());
  }

  /**
   * Builds a graph IRI, including a trailing slash, from a given namespace and graph name.
   * Equivalent to namespace + graphName + "/".
   * @param namespace the namespace IRI.
   * @param graphName the short name of the graph, e.g. "surfacegeometry".
   * @return graph IRI as string
   */
  public static String buildGraphIri(String namespace, String graphName) {
    return namespace + graphName + "/";
  }

  /**
   * Builds a graph IRI, including trailing slash, from a graph name, using the {@link Model}'s IRI's namespace.
   * @param graphName the short name of the graph, e.g. "surfacegeometry".
   * @return graph IRI as string
   */
  public String buildGraphIri(String graphName) {
    return buildGraphIri(getNamespace(), graphName);
  }

}
