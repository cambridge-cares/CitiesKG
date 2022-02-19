The Model framework is a lightweight framework for the concise creation of Java classes to interact with structured data
in a knowledge graph. This document outlines how to use it, how it works, and how to extend its functionality. 

# Model Framework User Guide

## Demo

There is a short, heavily annotated file `ModelDemo.java` in this folder which showcases the key features of the 
framework and walks through some of the typical design decisions and usage patterns that one may encounter. It and the 
[Using the Model framework](#Using the Model framework) section of this README may be read in any order. Also see 
the diagrammatic overview of the classes and methods on offer in `diagram.xml`.

## Defining a Model

The `Model` class is the main class used in the framework to interact with structured data. In general,

- each `Model` subclass (henceforth "a model") corresponds to a class in the knowledge graph ontology, or a part of such
  an ontological class;
- each *field* of a model corresponds to a type of quad involving the node (a role in its ontology); and
- each *instance* of a model corresponds to a node (IRI) of that class in the knowledge graph, and its field values
  correspond to the counterparties to its class-declared roles.

An example model implementation is shown below:

```Java
@ModelAnnotation(defaultGraphName = "employees")
class Employee extends Model {

  @Getter @Setter @FieldAnnotation("http://mycompany.org/ontology#hasName")
  protected String name;

  @Getter @Setter @FieldAnnotation("http://mycompany.org/ontology#hasAge")
  protected Integer age;

  @Getter @Setter @FieldAnnotation("http://mycompany.org/ontology#hasDepartment")
  protected URI department;

  @Getter @Setter
  @FieldAnnotation(
      value = "http://mycompany.org/ontology#manages",
      graphName = "companyhierarchy",
      innerType = Employee.class)
  protected ArrayList<Employee> subordinates;

  @Getter @Setter
  @FieldAnnotation(
      value = "http://mycompany.org/ontology#manages",
      graphName = "companyhierarchy",
      backward = true)
  protected Employee manager;

}
```

The `Employee` model contains three fields, `name`, `manager`, and `subordinates`, the last of which is an array. It
describes an Employee ontology where each Employee has

- exactly one `ex:hasName` datatype property of `xsd:string` type in the `employees` graph;
- exactly one `ex:hasAge` datatype property of `xsd:integer` type in the `employees` graph;
- exactly one `ex:hasDepartment` object property in the `employees` graph; and
- any number of `ex:manages` object properties in the `companyhierarchy` graph; also,
- for each Employee there exists exactly one other Employee with object property `ex:manages` with the first Employee as
  the object, in the `companyhierarchy` graph.

These details are specified through each field's `FieldAnnotation`, which characterises the role (quad) the field
corresponds to in its arguments:

- `value`: the IRI of the predicate of the quad described. Qualified names will be looked up in the JPSBaseLib
  PrefixToUrlMap and also custom specifications in the config file. No default value.
- `graphName`: the short name of the graph of the quad, which is appended to the application's target namespace to
  obtain the actual graph IRI used. Defaults to the `nativeGraphName` of the declaring class' `ModelAnnotation`.
- `backward`: whether the model instance is the subject or the object of the quad. Default: `false`.
- `innerType`: the class of the elements of the `ArrayList`, if the field is an `ArrayList`. This exists because Java's
  runtime type erasure means this information is not available at runtime even with reflection. Can be left unspecified
  for non-list fields.

The `ModelAnnotation` just provides the `defaultGraphName` as a fallback for fields with unspecified `graphName`. The
Lombok `Getter` and `Setter` are mandatory.

An example of compliant data for an Employee "John Smith", formatted in TriG:

```
@prefix    : <http://mycompany.org/>
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix ont: <http://mycompany.org/ontology#> .
@prefix ppl: <http://mycompany.org/people/> .
@prefix grp: <http://mycompany.org/groups/> .

:employees { ppl:john ont:hasName    "John Smith"^^xsd:string .
             ppl:john ont:hasAge     "26"^^xsd:integer        .
             ppl:john ont:department grp:accounting           . }

:companyhierarchy { ppl:john  ont:manages ppl:sarah .
                    ppl:john  ont:manages ppl:bill  .
                    ppl:edith ont:manages ppl:john  . }
```

Currently supported types for `FieldAnnotation`-annotated fields are:

- `String`
- `Integer`
- `Double`
- `java.net.URI`
- Any subclass of `Model`
- Any subclass of `DatatypeModel` (see "Creating a DatatypeModel")

Note that non-list fields must always match exactly one value. **If there are multiple or zero matches, behaviour is
undefined.**

For an example of a `Model` built for a triple store (without named graphs), see `ModelDemo.java`.

## Working with Models

### Creating a ModelContext

Each `Model` must exist in a `ModelContext`. A `ModelContext` stores the access information of the graph database, the
base namespace for graph IRIs (if applicable), and keeps track of instantiated `Model`s to crosslink them when loading
from the database. A `ModelContext` may be a triple context or a quad context, the latter supporting named graphs.

- `ModelContext(String targetResourceId[, int initialCapacity])`

  Creates a triple context with optional specification of the starting capacity of the context, which is used to
  initialise the internal `HashMap` of members. The `targetResourceId` is the ID provided to AccessAgentCaller for 
  SPARQL queries and updates.

- `ModelContext(String targetResourceId, String graphNamespace[, int initialCapacity])`

  Creates a quad context with optional specification of initial capacity. The `graphNamespace` is prepended to the
  graph name definitions in `Model` definitions to creatae graph IRIs. This enables the same `Model` to be used for 
  different namespaces with their own named graphs.

### Instantiating a Model

Once a `ModelContext` is created, `Model`s should be created through factory functions of the `ModelContext`. These are:

- `createNewModel(Class<T> ofClass, String iri)`

  Constructor for `Model`s which do not exist in the database. The first time changes are pushed, all fields of the 
  `Model` will be written; unassigned fields will write their default values, typically `null`, translating to blank 
  nodes in the database. Also, deletions will not be executed on first push. See [Pushing changes](#Pushing changes).

- `createHollowModel(Class<T> ofClass, String iri)` 

  Constructor for `Model`s which exist in the database. Creates a "hollow" model, which has its IRI assigned, but 
  all of its fields at default values and disabled: they will never be written, even if changed by the user. A field 
  becomes enabled (resumes normal change tracking behaviour) once it has been populated by a pull method. See 
  [Pushing changes](#Pushing changes).

- `loadAll(Class<T> ofClass, String iri)`

  `loadPartialModel(Class<T> ofClass, String iri, String... fieldNames)`

  `recursiveLoadAll(Class<T> ofClass, String iri, int recursionRadius)`

  `recursiveLoadPartial(Class<T> ofClass, String iri, int recursionRadius, String... fieldNames)`

  Creates a hollow model with `createHollowModel` and invokes `pullAll`, `pullPartial`, `recursivePullAll` or 
  `recursivePullPartial` on it, populating `Model` fields (recursively) with the respective pull algorithms. See 
  [Pulling data from the knowledge graph](#Pulling data from the knowledge graph) for detail on behaviour.

- `loadAllWhere(Class<T> ofClass, WhereBuilder condition)`

  `loadPartialWhere(Class<T> ofClass, WhereBuilder condition, String... fieldNames)`

  Loads or pulls all models which match the given search condition, using the `pullAll` and `pullPartial` query 
  patterns modified with the condition. The condition should use the `ModelContext.getModelVar()` node to represent the 
  search target. Also [Pulling data from the knowledge graph](#Pulling data from the knowledge graph).

### Pulling data from the knowledge graph

`Model`s may be populated with data from the database using one of the four pull methods, the last two of which are 
recursive wrappers for the first two.

- `pullAll(Model model)`

  Queries all triples/quads linked to the IRI, scans the results on the Java side for matches with the fields 
  defined in the `Model`, and populates the fields in the `Model`. The queries look like:

  ```
  SELECT ?value ?predicate ?datatype ?isblank
  WHERE {
    GRAPH ?graph { <model_iri> ?predicate ?value }
    BIND(DATATYPE(?value) AS ?datatype)
    BIND(ISBLANK(?value) AS ?isblank)
  }
  ```

  ```
  SELECT ?value ?predicate ?datatype ?isblank
  WHERE {
    GRAPH ?graph { ?value ?predicate <model_iri> }
    BIND(DATATYPE(?value) AS ?datatype)
    BIND(ISBLANK(?value) AS ?isblank)
  }
  ```
  
- `pullPartial(Model model, String... fieldNames)`

  Constructs specific queries for the named fields, executes it, and populates fields in the `Model`. Scalar (non-list) 
  fields are collected in one query, then each vector (list) field in a separate query. 

  ```
  SELECT ?value1 ?datatype1 ?isblank1 ?value3 ?datatype3 ?isblank3 ?value6 ?datatype6 ?isblank6
  WHERE {
    GRAPH <graph_1> { <model_iri> <scalar_role_1> ?value1 }
    BIND(DATATYPE(?value1) AS ?datatype1)
    BIND(ISBLANK(?value1) AS ?isblank1)
    GRAPH <graph_3> { <model_iri> <scalar_role_3> ?value3 }
    BIND(DATATYPE(?value3) AS ?datatype3)
    BIND(ISBLANK(?value3) AS ?isblank3)
    GRAPH <graph_6> { ?value6 <scalar_role_6> <model_iri> }
    BIND(DATATYPE(?value6) AS ?datatype6)
    BIND(ISBLANK(?value6) AS ?isblank6)
  }
  ```
  ```
  SELECT ?value ?datatype ?isblank
  WHERE {
    GRAPH <graph_2> { ?value <vector_role_2> <model_iri> }
    BIND(DATATYPE(?value) AS ?datatype)
    BIND(ISBLANK(?value) AS ?isblank)
  }
  ```
  ```
  SELECT ?value ?datatype ?isblank
  WHERE {
    GRAPH <graph_3> { ?value <vector_role_3> <model_iri> }
    BIND(DATATYPE(?value) AS ?datatype)
    BIND(ISBLANK(?value) AS ?isblank)
  }
  ```

- `recursivePullAll(Model model, int recursionRadius)`

  `recursivePullPartial(Model model, int recursionRadius, String... fieldNames)`

  TL;DR: Invokes either `pullAll` or `pullPartial` on the target `Model`, and then also on any `Model`-type fields' 
  values retrieved in the invocation, and so on recursively to an extent of `recursionRadius` degrees of separation 
  from the original target.

  To elaborate: when a field of `Model` type is pulled, the IRI is looked up in the `ModelContext`. If there is a 
  match, the existing `Model` is returned; else, `ModelContext` constructs a hollow `Model` for the IRI and returns 
  that. For a non-recursive pull, the hollow `Model` can then be itself pulled to populate its fields.

  The recursive versions listen to the lookup step above (specifically, `ModelContext.getModel`) and queue the 
  requested `Model`s to also be pulled later (repeating until exhaustion of `recursionRadius`). What is important to 
  note from this is that even a `Model` pre-existing in the context will be pulled (updated) if it falls within the 
  radius of a recursive pull, since the trigger is the lookup, not the creation. *Therefore, a recursive pull on a 
  `Model` may cause changes in neighbouring `Model`s you are working on to be overwritten.* 

  `Model`s one step past the recursion radius will be hollow, as secondary `Model`s would be for a non-recursive pull.

From the example queries above, it is seen that `pullAll` and `pullPartial` are significantly different in terms of 
their performance profile:

|               | number of queries           | amount of data returned by query              |
|---------------|-----------------------------|-----------------------------------------------|
| `pullAll`     | 2                           | all triples in DB with IRI as subject/object. |
| `pullPartial` | 1 + number of vector fields | only the values actually desired.             |

`pullAll` is usually more efficient in terms of number of queries (constant vs. scaling with vector field count), but 
`pullPartial` decouples performance from the presence of extraneous target-associated triples. Therefore,

- `pullAll` should be preferred when *query latency* is a performance bottleneck and there is more than one vector 
  field, 
  so long as the number of target-associated triples in the database is not prohibitively larger than the number we are
  interested in.
- `pullPartial` should be preferred when the former would return too many triples, so long as we are not querying a 
  prohibitively large number of vector fields; an empty list of field names can be provided to instruct to pull all 
  fields in the model.
- `recursivePullPartial` may also be of particular interest over its `all` counterpart as a shortcut for directional 
  discovery, e.g. selecting to only recursively pull a "father" property in a "Person" class to investigate 
  patrilineal lineage without loading an exponential number of relatives.

For example, querying DBpedia with `pullAll` will generally cause you to hit a response row limit.

The names `pullAll` and `pullPartial` and their method signatures superficially seem to refer to pulling all or some 
of a `Model`'s properties, but in practice can be equivalently—and more descriptively—interpreted as querying all or 
the specific relevant set of an IRI's connected triples in the graph.

### Pushing changes

Pushing changes to the database is easy. Data values may be modified directly in the models, and then simply call 
`ModelContext.pushChanges(Model model)` to write the modifications to the database. Alternatively, `ModelContext.
pushAllChanges()` does this for all `Model`s in the context.

When publishing changes, the framework has a system for determining what updates to write. Each `Model` keeps a 
cache of its field values on last synchronisation with the database (pull or push), and only if the current value of a 
field is different from the cached "clean" value will an update for it be created. The update (collected between 
fields) will take the form of:

```
DELETE WHERE { GRAPH <graph_1> { <model_iri> <scalar_role_5> ?value . } } ;
DELETE WHERE { GRAPH <graph_2> { ?value <scalar_role_7> <model_iri> . } } ;
DELETE WHERE { GRAPH <graph_2> { <model_iri> <vector_role_3> ?value . } } ;
INSERT DATA {
  GRAPH <graph_1> { <model_iri> <scalar_role_5> <scalar_5_value> }
  GRAPH <graph_2> { 
    <scalar_7_value> <scalar_role_7> <model_iri>   .
    <model_iri> <vector_role_3> <vector_3_value_1> .
    <model_iri> <vector_role_3> <vector_3_value_2> .
  }
};
```

A field which has not been pulled before is assigned a special state depending on its origin, which grants special 
treatment during push.

- A `Model` created by `createNewModel` has its fields marked `NEW`, which indicates that a field (a) should be 
  pushed regardless of the current value, and (b) does not need a deletion update to clear the previous value in the 
  database.

- A `Model` created by `createHollowModel` has its fields marked `UNPULLED` which indicates that a field should not be 
  pushed regardless of the current value.

When a field is pushed or pulled—synchronised with the database—its `cleanValues` entry is set to the current value 
(the implementation is actually slightly more complicated than this). If it was previously `NEW` or `UNPULLED`, it thus 
loses that special state and adopts ordinary comparison-based change tracking for push behaviour. Any `Model`s 
returned by `load` methods will therefore have relevant fields (all, or those named to be pulled) active and 
change-tracking already.

A `Model` may be manually "cleaned"—its `cleanValues` set to match its current values—by calling `setClean()` on it, 
with optional specification of field names to clean. Similarly, it may also be forcefully "dirtied" with `setDirty()`, 
which sets the chosen `cleanValues` to yet another special state, `FORCE_PUSH`, which causes fields to always be 
written regardless of current value (*with* deletion, unlike the `NEW` state).

Note that `Model` fields are treated as their IRIs for change-tracking purposes; changes within a referenced `Model` 
will not trigger a push of the referencing object property, as this would not actually result in any modification to 
the database. Nor is there any recursive or propagative behaviour to `pushChanges(Model model)`; to push across multiple 
objects, call them individually or use the context-wide `pushChanges()`.

To delete an object, use `ModelContext.delete(Model model, boolean zealous)` to **flag** an `Model` for deletion. The 
deletion will only be actually executed on the next `ModelContext.pushAllChanges()`. The zealous argument 
determines the deletion mode; if true, this will delete all triples/quads linked to the object in the database; 
otherwise, only fields described by the `Model` will be deleted. The non-zealous update is identical to the 
deletion part of `pushChanges`, and the zealous update looks like:

```
DELETE WHERE { <model_iri> ?predicate ?value      } ;
DELETE WHERE { ?value      ?predicate <model_iri> } ;
```

### Model wrappers for ModelContext methods

Many `ModelContext` methods have wrapping methods in `Model`, such as `model.pushChanges()` being equivalent to
`model.getContext().pushChanges(model)`. The difference is entirely cosmetic.

## Defining a DatatypeModel

`DatatypeModel` is an interface for classes representing, decoding, and encoding custom RDF literals, with support for
polymorphic RDF datatype IRIs within the same `DatatypeModel`. Subclasses of `DatatypeModel` may be used for fields of a 
`Model`. A `DatatypeModel` need not strictly utilise RDF datatypes, however, and it is valid to implement a 
`DatatypeModel` for e.g. custom manipulation for special strings.

A `DatatypeModel` must implement the following:

- `constructor with arguments (String value, String datatype)`

  This is not explicitly described in the interface definition due to language limitations, but it is retrieved by
  reflection at runtime and used by the framework. The `value` and `datatype` provided in the invocation are 
  respectively the `?value` and `DATATYPE(?value)` strings returned by a query to the database.

- `Node getNode()`

  Returns a Jena `Node` object encoding this object. This should be reversible with the constructor in the sense that if

    ```java
    Node node = obj1.getNode();
    MyDatatypeModel obj2 = new MyDatatypeModel(
        node.getLiteralLexicalForm(),
        node.getLiteralDatatypeUri()
    );
    ```

  then `obj1.equals(obj2)` should be `true`.

For an example, see `uk.ac.cam.cares.twa.cities.models.geo.GeometryType`.

# Model Framework Developer Guide

## How it works

### FieldKey

A `FieldKey` is a hashable, comparable object encoding the quad characterisation information in a `FieldAnnotation`. It
has fields:

- `predicate`: the full IRI of the predicate, copied or expanded from `FieldAnnotation.value`.
- `graphName`: short name of the graph, from `FieldAnnotation.value` if specified, else
  `ModelAnnotation.defaultGraphName`.
- `backward`: the same as `FieldAnnotation.backward`.

It serves as (a) a lookup key and (b) a sorting key for fields, the latter of which facilitates graph-based grouping 
in queries.

### FieldInterface

A `FieldInterface` is a class, not an interface in the Java language sense. One is created for each field with a
`FieldAnnotation`. During construction, it builds and stores a collection of functions to interact with its target
field based on the field type and annotation information. These are:

- Builtin methods fetched by reflection:
    - `getter`: the Lombok getter of the field.
    - `setter`: the Lombok setter of the field.
- Custom "outer-dependent" functions:
    - `listConstructor`: the constructor for an empty list, only assigned if the field is a list.
    - `putter`: the action for consuming an input value; for a list, this appends, otherwise, it sets (overwrites).
- Custom "inner-dependent" functions:
    - `parser`: converts string input (e.g. from a query) into the field's type.
    - `nodeGetter`: converts an object of the field's type into a Jena `Node`.
    - `minimiser`: converts an object of the field's type into a minimal representation for which
      if `nodeGetter(a).equals(nodeGetter(b))`, then `minimiser(a).equals(minimiser(b))`.

The methods exposed by `FieldInterface` wrap these functions for streamlined use by `Model`. They main ones are:

- `put`: wrapper for a composition of `putter`⋅`parser`.
- `clear`: sets the field to its default value, which is the output of `listConstructor` for a list, otherwise `null`.
- `getMinimised`: returns the output of `minimiser` on the field value, unless it is a list, in which case returns a
  list of the outputs of `minimiser` on each element.
- `getNodes`: returns the outputs of `nodeGetter` on elements of the field value; for non-lists, this has length 1.

### MetaModel

A `MetaModel` is created for each `Model` subclass the first time an instance thereof is created; all future instances
will then link back to the same `MetaModel`. Conceptually, it may be thought of as the collected output of 
reflection-based runtime annotation processing, which is stored for use across all instances.

The core element of each `MetaModel` is the `FieldKey`-indexed collection of `FieldInterface`s for its target class,
`TreeMap<FieldKey, FieldInterface> fieldMap`. This serves as the engine through which the `Model` base methods interact 
with the annotated fields declared by subclasses.

The other fields in `MetaModel` are `scalarFields` and `vectorFields`, which are simply the scalar (non-list)
and vector (list) entries in `fieldMap` extracted for convenience.

The use of `TreeMap` is deliberate so the entries are sorted by key.

### Tying it all together

`MetaModel`, `FieldKey` and `FieldInterface` are leveraged together in the main methods provided by the `Model` base
class.

- On `pullAll`, the graph database is queried for all quads containing the model instance's IRI as the subject or
  object. Each row of the response is processed as such:
  - The predicate, graph and direction of the quad are compiled into a `FieldKey`.
  - The `FieldKey` is looked up in `metaModel.fieldMap` to retrieve the corresponding `FieldInterface`.
  - The value and datatype of the counterparty in the quad is injected into the instance by `fieldInterface.put`. The
    conversion to the field's Java type and handling of lists is all compartmentalised inside `FieldInterface`.
  - A minimised copy of the new value is retrieved by `fieldInterface.getMinimised` and saved in `cleanValues`.
- On `pullPartial`, we:
  - Iterate through `metaModel.scalarFields` to build a combined query from the `FieldKey`s of requested fields.
  - Iterate through `metaModel.vectorFields` to build a separate query for each vector `FieldKey` requested.
  - Inject the response values into the `Model` through the respective `FieldInterface`s (and save minimised copies to 
    `cleanValues`)
- On `pushChanges`, we iterate through `metaModel.fieldMap`, and for each entry:
  - The current value (minimised), retrieved via the `FieldInterface`, is compared to the counterpart in `cleanValues`
    to determine if the field is dirty.
  - If so, updates to delete of the existing quad(s) and insert of the new quad(s) are built with
   `fieldInterface.getNode(s)`.
  - Minimised values are written to `cleanValues`.

The `loadAllWhere` and `loadPartial` methods use the same query builders as `pullAll` and `pullPartial`, but replace 
the `Model` IRI with a `?model` variable Node, and then append (a) search condition to describe `?model`, and (b) `ORDER 
BY ?model`. The response is decomposed into segments corresponding to different individuals, and each segment read 
in by the same response processors as `pullAll` and `pullPartial`. 

##Notes for future development

###How to add new types

####Method 1: Direct addition to FieldInterface

Support for different types is implemented in the constructor of `FieldInterface`. Simply add an additional 
condition to the `innerType` interrogation section, capturing the type to be added and setting their `parser`, 
`nodeGetter` and `minimiser` functions.

The new type should also be added to the tests for the package. Add a field of the new type to `TestModel`, and add 
a new test to `FieldInterfaceTests` following the pattern of other types. `FieldInterfaceTests` is already nicely 
abstracted to make this quick and easy. The field counts in MetaModelTest must also be incremented to reflect the 
increased size of TestModel.

This method is appropriate for relatively common types such as date and time types, numeric types, etc.

####Method 2: Creation of a DatatypeModel

See [Defining a DatatypeModel](#Defining a DatatypeModel). This is more appropriate if you need behaviour for a 
particular use case.

###Why does deletion work like that?

`DELETE WHERE` is used in place of `DELETE DATA` in the deletion parts of the change-pushing updates. This is, I 
think, justified by there being a change-tracking system: properties which have not modified will not be republished 
and overwrite changes by other agents or uses; modified properties will overwrite other changes; this is standard 
behaviour for many systems. Some alternative behaviours are:

- Properties which have been changed are not overwritten, and instead silently duplicated, keeping both the old and 
  new triple. This is undesirable as it will silently create violations of ontologies and necessitate clean-up agents or 
  routines. An advantage is that it will likely perform better than the current deletion algorithm, but the benefits 
  are slim.

- Trying to write to properties which have been changed in the database since last pull throws an exception. This 
  theoretically enables an almost superset of current behaviour, since the end-user developer may catch that 
  exception and in response do an override or pull and re-push. However, it is clunky and introduces a lot of new 
  degrees of complexity into both the framework and the user experience; the one must not only interact with current 
  values and a simple clean/dirty state, but also micromanage congruence between the clean reference and the 
  database. Finally, querying every time we push is likely to be expensive, so an option will have to be included to 
  skip this step. All in all, such a solution would introduce significant complexity to enable a feature which few 
  would use due to the performance impact and limited use case.

It is possible there is an elegant solution I have not thought of. If so, feel free to implement it.