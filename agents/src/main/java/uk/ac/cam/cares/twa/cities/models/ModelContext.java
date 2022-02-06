package uk.ac.cam.cares.twa.cities.models;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ModelContext {

  private static class MemberKey {
    public final Class<?> ofClass;
    public final String iri;

    public MemberKey(Class<?> ofClass, String iri) {
      this.ofClass = ofClass;
      this.iri = iri;
    }

    @Override
    public int hashCode() {
      return Objects.hash(ofClass, iri);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MemberKey && ((MemberKey) obj).ofClass.equals(ofClass) && ((MemberKey) obj).iri.equals(iri);
    }
  }

  /**
   * {@link RecursivePullSession} operates in a loop where it calls {@link ModelContext#pullAll(Model)} on all items in
   * its working queue, causing invocations of {@link ModelContext#getModel(Class, String)} by {@link FieldInterface}s,
   * which are reported to {@link RecursivePullSession#queue(Model)}, where they are added to the pending queue. The
   * pending queue is then moved to the working queue and the process repeats.
   * <p>
   * Each round of pulling retrieves nodes one step away from the previous round's nodes, i.e. an additional degree of
   * separation from the origin provided in the session construction. Repeat visits are prevented by tracking the set of
   * traversed nodes. The number of rounds of pulling thus defines the maximum separation radius from the origin to
   * retrieve, which is our interpretation of {@code recursionRadius} for a non-acyclic graph. The breadth-first
   * strategy is necessary to prevent paths from cutting each other off.
   * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
   * @version $Id$
   */
  private class RecursivePullSession {

    private Queue<Model> pendingPullQueue = new LinkedList<>();
    private Queue<Model> workingPullQueue = new LinkedList<>();
    private final Set<String> traversedIris = new HashSet<>();
    private int remainingDegreesOfSeparation;

    private final boolean partial;
    private final String[] fieldNames;

    RecursivePullSession(Model origin, int recursionRadius) {
      pendingPullQueue.add(origin);
      this.remainingDegreesOfSeparation = recursionRadius;
      this.partial = false;
      this.fieldNames = null;
    }

    RecursivePullSession(Model origin, int recursionRadius, String... fieldNames) {
      queue(origin);
      this.remainingDegreesOfSeparation = recursionRadius;
      this.partial = true;
      this.fieldNames = fieldNames;
    }

    void execute() {
      // each loop, we load models at +1 increasing radius (degrees of separation) from before.
      while (remainingDegreesOfSeparation >= 0) {
        remainingDegreesOfSeparation--;
        // switch queues
        Queue<Model> temp = workingPullQueue;
        workingPullQueue = pendingPullQueue;
        pendingPullQueue = temp;
        // go through working queue; each pull may add to the pending queue
        while (!workingPullQueue.isEmpty()) {
          if (partial) pullPartial(workingPullQueue.poll(), fieldNames);
          else pullAll(workingPullQueue.poll());
        }
      }
    }

    void queue(Model model) {
      if (traversedIris.add(model.iri))
        pendingPullQueue.add(model);
    }

  }

  private RecursivePullSession currentPullSession;

  // SPARQL variable names
  private static final String GRAPH = "graph";
  private static final String PREDICATE = "predicate";
  private static final String VALUE = "value";
  private static final String DATATYPE = "datatype";
  private static final String ISBLANK = "isblank";
  // Helper constants for constructing SPARQL queries
  private static final String NOT = "NOT";
  private static final String ISBLANK_FUN = "ISBLANK";
  private static final String DATATYPE_FUN = "DATATYPE";
  private static final String QM = "?";
  private static final String OP = "(";
  private static final String CP = ")";
  // Error text
  private static final String QUAD_MODEL_IN_TRIPLE_CONTEXT_ERROR_TEXT = "Quad Model cannot be initialised in triple context.";
  private static final String OBJECT_NOT_FOUND_EXCEPTION_TEXT = "Object not found in database.";
  private static final String MODEL_ALREADY_REGISTERED_EXCEPTION_TEXT = "Model already registered for IRI.";
  // Threshold for executeUpdates to execute if "force" is not specified
  private static final int EXECUTION_CHARACTER_THRESHOLD = 250000;

  public final String targetResourceId;
  public final String graphNamespace;
  public final Map<MemberKey, Model> members;

  public boolean isQuads() {
    return graphNamespace != null;
  }

  /**
   * Creates a triple store context.
   * @param targetResourceId the target resource ID passed to AccessAgentCaller for database access.
   */
  public ModelContext(String targetResourceId) {
    this.targetResourceId = targetResourceId;
    this.graphNamespace = null;
    this.members = new HashMap<>();
  }

  /**
   * Creates a triple store context with the specified initial member capacity.
   * @param targetResourceId the target resource ID passed to AccessAgentCaller for database access.
   * @param initialCapacity  the capacity to initialise the context members hashmap with.
   */
  public ModelContext(String targetResourceId, int initialCapacity) {
    this.targetResourceId = targetResourceId;
    this.graphNamespace = null;
    this.members = new HashMap<>(initialCapacity);
  }

  /**
   * Creates a quad store context.
   * @param targetResourceId the target resource ID passed to AccessAgentCaller for database access.
   * @param graphNamespace   the graph namespace to which graph names provided in {@link Model} definitions are appended
   *                         to obtain the actual graph IRIs.
   */
  public ModelContext(String targetResourceId, String graphNamespace) {
    this.targetResourceId = targetResourceId;
    this.graphNamespace = graphNamespace;
    this.members = new HashMap<>();
  }

  /**
   * Creates a quad store context with the specified initial member capacity.
   * @param targetResourceId the target resource ID passed to AccessAgentCaller for database access.
   * @param graphNamespace   the graph namespace to which graph names provided in {@link Model} definitions are appended
   *                         to obtain the actual graph IRIs.
   * @param initialCapacity  the capacity to initialise the context members hashmap with.
   */
  public ModelContext(String targetResourceId, String graphNamespace, int initialCapacity) {
    this.targetResourceId = targetResourceId;
    this.graphNamespace = graphNamespace;
    this.members = new HashMap<>(initialCapacity);
  }

  /**
   * Creates a model with uninitialised {@code cleanValues}. Only for internal use.
   */
  private <T extends Model> T createPrototypeModel(Class<T> ofClass, String iri) {
    if (MetaModel.get(ofClass).isQuads && !isQuads()) {
      throw new JPSRuntimeException(QUAD_MODEL_IN_TRIPLE_CONTEXT_ERROR_TEXT);
    }
    MemberKey key = new MemberKey(ofClass, iri);
    if (members.containsKey(key)) {
      throw new JPSRuntimeException(MODEL_ALREADY_REGISTERED_EXCEPTION_TEXT);
    }
    try {
      Constructor<T> constructor = ofClass.getDeclaredConstructor();
      T instance = constructor.newInstance();
      instance.iri = iri;
      instance.context = this;
      members.put(key, instance);
      return instance;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Creates a model whose fields are all at default values and marked dirty.
   */
  public <T extends Model> T createNewModel(Class<T> ofClass, String iri) {
    T instance = createPrototypeModel(ofClass, iri);
    Arrays.fill(instance.cleanValues, Model.SpecialFieldInstruction.NEW);
    return instance;
  }

  /**
   * Creates a model whose fields are all at default values and disabled (fields never push regardless of value). Any
   * pull actions will cause the pulled fields to become enabled and resume normal clean/dirty push behaviour.
   * Equivalent to
   */
  public <T extends Model> T createHollowModel(Class<T> ofClass, String iri) {
    T model = createPrototypeModel(ofClass, iri);
    Arrays.fill(model.cleanValues, Model.SpecialFieldInstruction.UNPULLED);
    return model;
  }

  /**
   * Convenience wrapper for {@link #createHollowModel(Class, String)} followed by {@link #pullPartial(Model, String...)}.
   * @return the partially loaded model.
   */
  public <T extends Model> T loadPartialModel(Class<T> ofClass, String iri, String... fieldNames) {
    T model = createHollowModel(ofClass, iri);
    pullPartial(model, fieldNames);
    return model;
  }

  /**
   * Convenience wrapper for {@link #createHollowModel(Class, String)} followed by {@link #recursivePullPartial(Model, int, String...)}.
   * @return the partially loaded model.
   */
  public <T extends Model> T recursiveLoadPartialModel(Class<T> ofClass, String iri, int recursionRadius, String... fieldNames) {
    T model = createHollowModel(ofClass, iri);
    recursivePullPartial(model, recursionRadius, fieldNames);
    return model;
  }

  /**
   * Convenience wrapper for {@link #createHollowModel(Class, String)} followed by {@link #pullAll(Model)}.
   * @return the loaded model.
   */
  public <T extends Model> T loadModel(Class<T> ofClass, String iri) {
    T model = createHollowModel(ofClass, iri);
    pullAll(model);
    return model;
  }

  /**
   * Convenience wrapper for {@link #createHollowModel(Class, String)} followed by {@link #recursivePullAll(Model, int)}.
   * @return the loaded model.
   */
  public <T extends Model> T recursiveLoadModel(Class<T> ofClass, String iri, int recursionRadius) {
    T model = createHollowModel(ofClass, iri);
    recursivePullAll(model, recursionRadius);
    return model;
  }

  /**
   * If this context already has registered a {@link Model} of the given IRI, return that. Otherwise, create a hollow
   * model for it. If this context has a {@link RecursivePullSession} active, i.e. this is a downstream request of a
   * {@link #recursivePullAll(Model, int)} or {@link #recursivePullPartial(Model, int, String...)}  call), this fetch
   * request is reported to the pull session.
   * @return the requested {@link Model}.
   */
  public <T extends Model> T getModel(Class<T> ofClass, String iri) {
    Model model = members.get(new MemberKey(ofClass, iri));
    if (model == null) model = createHollowModel(ofClass, iri);
    //
    if (currentPullSession != null) currentPullSession.queue(model);
    return ofClass.cast(model);
  }

  /**
   * Flags the specified model to be (a) destroyed in the target resource, including all quads described or not
   * described in the Java class definition, and (b) removed from the context's registry, the next time a context-wide
   * push is requested. Note that it is an error to delete a model and create a new one of the same IRI before first
   * pushing the change; the model is still registered to the context between this and the {@link #pushChanges(Model)}.
   */
  public void delete(Model model) {
    model.deleted = true;
  }

  /**
   * Pushes all changes in the context. Equivalent to {@link #pushChanges(Model)}} on every member of the context, but more
   * optimised.
   */
  public void pushAllChanges() {
    UpdateRequest deletions = new UpdateRequest();
    UpdateBuilder insertions = new UpdateBuilder();
    Stack<MemberKey> toBeRemoved = new Stack<>();
    boolean anyInserts = false;
    for (Map.Entry<MemberKey, Model> entry : members.entrySet()) {
      if (entry.getValue().deleted) {
        makeDeletionDeltas(entry.getValue(), deletions);
        toBeRemoved.add(entry.getKey());
      } else {
        // makeChangeDeltas returns whether any insertions were added
        anyInserts = makeChangeDeltas(entry.getValue(), deletions, insertions) || anyInserts;
      }
      if (deletions.toString().length() > EXECUTION_CHARACTER_THRESHOLD) {
        update(deletions.add(insertions.build()).toString());
        deletions = new UpdateRequest();
        insertions = new UpdateBuilder();
      }
      entry.getValue().setAllClean();
    }
    for (MemberKey key : toBeRemoved) members.remove(key);
    // anyInserts tracking is needed since trying to build an empty UpdateBuilder causes an error, and I cannot find
    // a way to probe whether an UpdateBuilder contains any operations.
    if (anyInserts) deletions.add(insertions.build());
    if (deletions.getOperations().size() > 0)
      update(deletions.toString());
  }

  /**
   * Pushes all dirty field values to the database for a model.
   */
  public void pushChanges(Model model) {
    UpdateRequest deletions = new UpdateRequest();
    UpdateBuilder insertions = new UpdateBuilder();
    if (makeChangeDeltas(model, deletions, insertions)) // return value is whether insertions were added
      deletions.add(insertions.build());
    model.setAllClean();
    if (deletions.getOperations().size() > 0)
      update(deletions.toString());
  }

  /**
   * Determines the changes in a model which should be pushed, and add necessary deletion updates and insertion quads
   * to the given output update builders.
   * @param model         the model to make deltas for.
   * @param deletionsOut  the output destination for deletion updates.
   * @param insertionsOut the output destination for insertion quads.
   * @return whether any insertions were performed.
   */
  private boolean makeChangeDeltas(Model model, UpdateRequest deletionsOut, UpdateBuilder insertionsOut) {
    boolean anyInserts = false;
    for (Map.Entry<FieldKey, FieldInterface> entry : model.metaModel.fieldMap.entrySet()) {
      FieldInterface fieldInterface = entry.getValue();
      FieldKey key = entry.getKey();
      // Check for special instructions
      Object cleanValue = model.cleanValues[fieldInterface.index];
      if (cleanValue == Model.SpecialFieldInstruction.UNPULLED) {
        continue;
      } else if (cleanValue != Model.SpecialFieldInstruction.NEW) {
        // Check if dirty
        if (Objects.equals(fieldInterface.getMinimised(model), cleanValue))
          continue;
      }
      Node self = NodeFactory.createURI(model.iri);
      Node predicate = NodeFactory.createURI(key.predicate);
      Node graph = isQuads() ? NodeFactory.createURI(graphNamespace + key.graphName) : null;
      // Add deletion
      if (cleanValue != Model.SpecialFieldInstruction.NEW) {
        WhereBuilder where = new WhereBuilder().addWhere(key.backward ? (QM + VALUE) : self, predicate, key.backward ? self : (QM + VALUE));
        if (isQuads()) {
          deletionsOut.add(new UpdateBuilder().addGraph(graph, where).buildDeleteWhere());
        } else {
          deletionsOut.add(new UpdateBuilder().addWhere(where).buildDeleteWhere());
        }
      }
      // Add insertion; technically object properties are duplicate-inserted from both ends, but I don't think it's
      // actually going to be distinguishably more performant if we do a check for this and do it once each instead.
      for (Node valueValue : fieldInterface.getNodes(model)) {
        Triple triple = new Triple(key.backward ? valueValue : self, predicate, key.backward ? self : valueValue);
        if (isQuads()) {
          insertionsOut.addInsert(new Quad(graph, triple));
        } else {
          insertionsOut.addInsert(triple);
        }
        anyInserts = true;
      }
    }
    return anyInserts;
  }

  /**
   * Adds the updates for the deletion of a model to an output update request.
   */
  private void makeDeletionDeltas(Model model, UpdateRequest deletionsOut) {
    Node self = NodeFactory.createURI(model.iri);
    deletionsOut.add(new UpdateBuilder().addWhere(QM + VALUE, QM + PREDICATE, self).buildDeleteWhere());
    deletionsOut.add(new UpdateBuilder().addWhere(self, QM + PREDICATE, QM + VALUE).buildDeleteWhere());
  }

  /**
   * Executes {@link #pullAll(Model)} on the model instance, loading its data from the database, and also does this
   * recursively for all its model references to a depth of {@code recursionRadius}. If the referenced
   * models already exist in this context, they are updated with new values; else, they are created and pulled.
   * <p>
   * Note that this uses a general query for all quads linked to the IRI and filters afterwards, which may be less
   * performant when the {@link Model} only declares a small subset of them; for alternative behaviour, see
   * {@link #pullPartial(Model, String...)}.
   * @param recursionRadius degrees of separation from the original target model through which referenced
   *                        models should be pulled. Models just one degree beyond will be instantiated as
   *                        a hollow model if not already registered in the context, but not pulled.
   */
  public void recursivePullAll(Model model, int recursionRadius) {
    currentPullSession = new RecursivePullSession(model, recursionRadius);
    currentPullSession.execute();
    currentPullSession = null;
  }

  /**
   * Populates all fields of the model instance with values from the database and sets all fields clean. {@link Model}
   * references are instantiated as hollow models if not already present in the context; to recursively
   * instantiate/update, see {@link #recursivePullAll(Model, int)}.
   * <p>
   * Note that this uses a general query for all quads linked to the IRI and filters afterwards, which may be less
   * performant when the {@link Model} only declares a small subset of them; for alternative behaviour, see
   * {@link #pullPartial(Model, String...)}.
   */
  public void pullAll(Model model) {
    model.clearAll();
    pullAllInDirection(model, true);
    pullAllInDirection(model, false);
    model.setAllClean();
  }

  /**
   * Populates all fields in one direction with values from the database.
   * @param model    the model instance to populate.
   * @param backward whether iriName is the object or subject in the rows retrieved.
   */
  private void pullAllInDirection(Model model, boolean backward) {
    Query queryString = buildPullAllInDirectionQuery(model.iri, backward);
    JSONArray queryResult = query(queryString.toString());
    for (int index = 0; index < queryResult.length(); index++) {
      JSONObject row = queryResult.getJSONObject(index);
      // If using named graphs (quads), decompose the retrieved graph IRI into a graph name
      String graph = row.optString(GRAPH, "");
      if (graphNamespace != null && graph.startsWith(graphNamespace))
        graph = graph.substring(graphNamespace.length());
      // Do the field lookup and put
      FieldKey fieldKey = new FieldKey(graph, row.getString(PREDICATE), backward);
      FieldInterface fieldInterface = model.metaModel.fieldMap.get(fieldKey);
      if (fieldInterface == null) continue;
      if (isTruthy(row.getString(ISBLANK))) {
        fieldInterface.clear(model);
      } else {
        fieldInterface.put(model, row.getString(VALUE), row.optString(DATATYPE));
      }
    }
  }

  /**
   * Composes a query to pull all triples relating to the target IRI.
   * @param iri      the IRI from which to pull data.
   * @param backward whether to query quads with <code>iriName</code> as the object (true) or subject (false).
   * @return the composed query.
   */
  private Query buildPullAllInDirectionQuery(String iri, boolean backward) {
    try {
      // SELECT ?value ?predicate ?datatype ?isblank
      SelectBuilder select = new SelectBuilder()
          .addVar(QM + VALUE).addVar(QM + PREDICATE).addVar(QM + DATATYPE).addVar(QM + ISBLANK);
      // WHERE { <self> ?predicate ?value }   or   WHERE { ?value ?predicate <self> }
      WhereBuilder where = new WhereBuilder()
          .addWhere(backward ? (QM + VALUE) : NodeFactory.createURI(iri),
              QM + PREDICATE, backward ? NodeFactory.createURI(iri) : (QM + VALUE));
      if (isQuads()) {
        select.addVar(QM + GRAPH).addGraph(QM + GRAPH, where);
      } else {
        select.addWhere(where);
      }
      // BIND(datatype(?value) AS ?datatype) BIND(isBlank(?value) AS ?isblank)
      select.addBind(DATATYPE_FUN + OP + QM + VALUE + CP, QM + DATATYPE)
          .addBind(ISBLANK_FUN + OP + QM + VALUE + CP, QM + ISBLANK);
      return select.build();
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Executes {@link #pullPartial(Model, String...)} on the model instance, loading its data from the database, and also
   * does this recursively for all its model references to a depth of {@code recursionRadius}. If the
   * referenced models already exist in this context, they are updated with new values; else, they are created and
   * pulled.
   * <p>
   * If {@code fieldNames} is empty, all fields are pulled; this is different behaviour from {@link #pullAll(Model)}
   * as this uses a specific query for desired values, not a general query for all quads linked to the IRI, and may be
   * more performant when only a small subset is defined in the {@link Model}.
   * @param recursionRadius degrees of separation from the original target model through which referenced
   *                        models should be pulled. Models just one degree beyond will be instantiated as
   *                        a hollow model if not already registered in the context, but not pulled.
   * @param fieldNames      the names of vector and scalar fields to be populated. The same
   *                        {@code fieldNames} is provided to all partial pulls in the recursion; if a
   *                        model does not have a named field, that name is ignored.
   */
  public void recursivePullPartial(Model model, int recursionRadius, String... fieldNames) {
    currentPullSession = new RecursivePullSession(model, recursionRadius, fieldNames);
    currentPullSession.execute();
    currentPullSession = null;
  }

  /**
   * Populates the named fields from the target resource. {@link Model} references are instantiated as hollow models if
   * not already present in the context; to recursively instantiate/update, see {@link #recursivePullPartial(Model, int, String...)}.
   * <p>
   * If {@code fieldNames} is empty, all fields are pulled; this is different behaviour from {@link #pullAll(Model)}
   * as this uses a specific query for desired values, not a general query for all quads linked to the IRI, and may be
   * more performant when only a small subset is defined in the {@link Model}.
   * @param model      the {@link Model} to populate.
   * @param fieldNames the names of vector and scalar fields to be populated.
   */
  public void pullPartial(Model model, String... fieldNames) {
    pullScalars(model, fieldNames);
    pullVectors(model, fieldNames);
  }

  /**
   * Populates the named scalar fields from the target resource. {@link Model} references are instantiated as
   * hollow models if not already present in the context; to recursively instantiate/update, see
   * {@link #recursivePullPartial(Model, int, String...)}.
   * <p>
   * If {@code fieldNames} is empty, all scalar fields are pulled; this is different behaviour from
   * {@link #pullAll(Model)} as this uses a specific query for desired values, not a general query for all quads linked
   * to the IRI, and may be more performant when only a small subset is defined in the {@link Model}.
   * @param model      the {@link Model} to populate.
   * @param fieldNames the names of scalar fields to be populated.
   */
  private void pullScalars(Model model, String... fieldNames) {
    // Build and execute the query for scalar values
    SelectBuilder query = buildScalarsQuery(model, fieldNames);
    if (query.getVars().size() == 0) return;
    JSONArray scalarResponse = query(query.buildString());
    if (scalarResponse.length() == 0) throw new JPSRuntimeException(OBJECT_NOT_FOUND_EXCEPTION_TEXT);
    // Put the query results into the model
    JSONObject row = scalarResponse.getJSONObject(0);
    for (Map.Entry<FieldKey, FieldInterface> entry : model.metaModel.scalarFieldList) {
      FieldInterface field = entry.getValue();
      if (!row.has(VALUE + field.index)) continue;
      if (isTruthy(row.getString(ISBLANK + field.index))) {
        field.clear(model);
      } else {
        field.put(model, row.getString(VALUE + field.index), row.optString(DATATYPE + field.index));
      }
      model.cleanValues[field.index] = field.getMinimised(model);
    }
  }

  /**
   * Composes a query to retrieve the values for named scalar fields of this instance.
   */
  private SelectBuilder buildScalarsQuery(Model model, String... fieldNames) {
    try {
      SelectBuilder select = new SelectBuilder();
      for (Map.Entry<FieldKey, FieldInterface> entry : model.metaModel.scalarFieldList) {
        // Filter for only requested fields
        FieldInterface field = entry.getValue();
        FieldKey key = entry.getKey();
        if (fieldNames.length > 0 && !ArrayUtils.contains(fieldNames, field.field.getName())) continue;
        // Create nodes to use
        Node self = NodeFactory.createURI(model.iri);
        Node predicate = NodeFactory.createURI(key.predicate);
        String valueN = QM + VALUE + field.index;
        String datatypeN = QM + DATATYPE + field.index;
        String isBlankN = QM + ISBLANK + field.index;
        // SELECT ?value ?predicate ?datatype ?isblank
        select.addVar(valueN).addVar(datatypeN).addVar(isBlankN);
        // WHERE { <self> <predicate> ?value }   or   WHERE { ?value <predicate> <self> }
        WhereBuilder where = new WhereBuilder().addWhere(key.backward ? valueN : self, predicate, key.backward ? self : valueN);
        if (isQuads()) {
          select.addGraph(NodeFactory.createURI(graphNamespace + key.graphName), where);
        } else {
          select.addWhere(where);
        }
        // BIND(datatype(?value) AS ?datatype) BIND(isBlank(?value) AS ?isblank)
        select.addBind(DATATYPE_FUN + OP + valueN + CP, datatypeN)
            .addBind(ISBLANK_FUN + OP + valueN + CP, isBlankN);
      }
      return select;
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Populates the named vector fields from the target resource. {@link Model} references are instantiated as
   * hollow models if not already present in the context; to recursively instantiate/update, see
   * {@link #recursivePullPartial(Model, int, String...)}.
   * <p>
   * If {@code fieldNames} is empty, all scalar vector are pulled; this is different behaviour from
   * {@link #pullAll(Model)} as this uses a specific query for desired values, not a general query for all quads linked
   * to the IRI, and may be more performant when only a small subset is defined in the {@link Model}.
   * @param model      the {@link Model} to populate.
   * @param fieldNames the names of vector fields to be populated.
   */
  private void pullVectors(Model model, String... fieldNames) {
    for (Map.Entry<FieldKey, FieldInterface> entry : model.metaModel.vectorFieldList) {
      // Filter for only requested fields
      FieldInterface field = entry.getValue();
      if (fieldNames.length > 0 && !ArrayUtils.contains(fieldNames, field.field.getName())) continue;
      // Query: the vector query guaranteed to be non-empty, unlike in pullScalars, so no need to perform check
      JSONArray response = query(buildVectorQuery(model, entry.getKey()).buildString());
      // Put response values into model
      for (int i = 0; i < response.length(); i++) {
        JSONObject row = response.getJSONObject(i);
        if (isTruthy(row.getString(ISBLANK))) {
          field.clear(model);
        } else {
          field.put(model, row.getString(VALUE), row.optString(DATATYPE));
        }
      }
      model.cleanValues[field.index] = field.getMinimised(model);
    }
  }

  /**
   * Composes a query for all matches of a property described by a {@link FieldKey}, for this instance.
   */
  private SelectBuilder buildVectorQuery(Model model, FieldKey key) {
    Node self = NodeFactory.createURI(model.iri);
    Node predicate = NodeFactory.createURI(key.predicate);
    try {
      // SELECT ?value ?predicate ?datatype ?isblank
      SelectBuilder select = new SelectBuilder().addVar(QM + VALUE).addVar(QM + DATATYPE).addVar(QM + ISBLANK);
      // WHERE { <self> <predicate> ?value }   or   WHERE { ?value <predicate> <self> }
      WhereBuilder where = new WhereBuilder()
          .addWhere(key.backward ? (QM + VALUE) : self, predicate, key.backward ? self : (QM + VALUE));
      if (isQuads()) {
        Node graph = NodeFactory.createURI(graphNamespace + key.graphName);
        select.addGraph(graph, where);
      } else {
        select.addWhere(where);
      }
      // BIND(datatype(?value) AS ?datatype) BIND(isBlank(?value) AS ?isblank)
      select.addBind(DATATYPE_FUN + OP + QM + VALUE + CP, QM + DATATYPE)
          .addBind(ISBLANK_FUN + OP + QM + VALUE + CP, QM + ISBLANK);
      return select;
    } catch (ParseException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Executes a SPARQL query at the target resource of this context and returns the response.
   * @param query the query string.
   * @return the deserialised {@link JSONArray} of rows in the response.
   */
  public JSONArray query(String query) {
    if (targetResourceId.startsWith("HARDCODE:")) {
      String endpoint = targetResourceId.substring(9);
      String responseString = new RemoteStoreClient(endpoint).execute(query);
      return new JSONArray(responseString);
    } else {
      String responseString = AccessAgentCaller.query(targetResourceId, query);
      JSONObject response = new JSONObject(responseString);
      return new JSONArray(response.getString("result"));
    }
  }

  /**
   * Executes a  SPARQL update at the target resource of this context.
   * @param update the update string.
   */
  public void update(String update) {
    if (targetResourceId.startsWith("HARDCODE:")) {
      String endpoint = targetResourceId.substring(9);
      new RemoteStoreClient(endpoint, endpoint).executeUpdate(update);
    } else {
      AccessAgentCaller.update(targetResourceId, update);
    }
  }

  public boolean isTruthy(String str) {
    return str.equals("true") || str.equals("1");
  }

}
