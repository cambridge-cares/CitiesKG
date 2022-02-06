# Model Framework

The Model framework is a lightweight framework for the concise creation of Java classes to interact with structured data
in a knowledge graph.

## Using the Model framework

### Creating a Model

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

  @Getter @Setter @FieldAnnotation("http://example.org/ontology#hasName")
  protected String name;

  @Getter @Setter @FieldAnnotation("http://example.org/ontology#hasAge")
  protected Integer age;

  @Getter @Setter @FieldAnnotation("http://example.org/ontology#hasDepartment")
  protected URI department;

  @Getter @Setter
  @FieldAnnotation(
      value = "http://example.org/ontology#manages",
      graphName = "companyhierarchy",
      innerType = Employee.class)
  protected ArrayList<Employee> subordinates;

  @Getter @Setter
  @FieldAnnotation(
      value = "http://example.org/ontology#manages",
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

```xml
@prefix :
<http://example.org/> .
        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
        @prefix ont: <http://example.org/ontology#> .

        :employees { :john ont:hasName    "John Smith"^^xsd:string .
        :john ont:hasAge     "26"^^xsd:integer        .
        :john ont:department :accounting              . }

        :companyhierarchy { :john  ont:manages :sarah .
        :john  ont:manages :bill  .
        :edith ont:manages :john  . }
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

### Using a Model

The base `Model` class provides the following main methods:

- `constructor with no arguments`

  Initialises a model with default values for all fields. Marks all fields as dirty.

- `setIri(URI iri)`

  Sets the IRI of the model instance.

- `pullAll(String kgId, int recursiveInstantiationDepth)`

  Queries the resource identified by `kgId` via AccessAgent and OntoKGRouter for the quads of the currently set model
  IRI and populates the fields of the model instance. `recursiveInstantiationDepth` is how many nested models deep
  should be instantiated: if >0, then for each field of `Model` type, a new model instance is created, the corresponding
  IRI returned by the query is assigned to it, and `pullAll` is called on it with `recursiveInstantiationDepth - 1`; if
  0, `Model` fields will be instantiated and their IRIs assigned, but `pullAll` will not be called.

  Marks all fields as clean.

- `queuePushUpdate(boolean pushForward, boolean pushBackward)`

  Queues a SPARQL update to overwrite relevant quads in the database with the current values of all dirty fields. The
  arguments allow independent toggling of whether to do this for forward and backward fields (see "Creating a Model" for
  what this means).

  Marks all fields as clean.

- `executeUpdates(String kgId, boolean force)`

  Static; executes all queued updates at the resource identified by `kgId`. If `!force`, only executes if the current
  queue exceeds 250,000 characters. **Note that each thread has its own update queue if the application is
  multithreaded.**

A field is dirty if it differs from its value from when it was last synchronised with the database (marked clean), or if
it was more recently manually marked dirty than marked clean. Note that if a field is a `Model`, only the URI is
compared in this assessment, and only the object property quad itself is pushed; any changes within the
referenced `Model` must be independently pushed.

A typical workflow looks like:

```java
public static void main(String[]args){

    // Pull John's data from the database.
    Employee john=new Employee();
    john.setIri(URI.create("http://example.org/john"));
    john.pullAll("mycompany",0);

    // David is our new manager hire.
    Employee david=new Employee();
    david.setIri(URI.create("http://example.org/david"));
    david.setName("David Anderson");
    david.setAge(21);

    // David has been assigned under John's manager, Sarah.
    david.setManager(john.getManager());

    // John is transferring one of his subordinates, Bill, to David.
    Employee bill=john.getSubordinates().pop();
    david.getSubordinates().add(bill);

    // Push updates
    john.queuePushUpdate(true,true);
    david.queuePushUpdate(true,true);
    Model.executeUpdates("mycompany",true);

    }
```

The update executed will look like:

[TODO]

`Model` also provides other methods for state manipulation and more specific methods of pulling data for performance
optimisation; see the class file documentation for more details.

Note that null values in scalar (non-list) fields are legal and will be written as a blank node; the quad will not be
omitted. Similarly, blank nodes are read into the Model as null values. To support optional properties, use an
ArrayList. Once again, **pulling from an IRI which does not satisfy the scalar requirements results in undefined
behaviour.** At the time of writing, I believe that the behaviour is that the first push will duplicate quads due to a
delete failure, but a second push will successfully clear duplicates, so long as the offending quads are in scope of the
update. There is no guarantee that this is still true at the time of reading.

### Creating a DatatypeModel

`DatatypeModel` is an interface for classes representing, decoding, and encoding custom RDF literals, with support for
polymorphic RDF datatype IRIs within the same `DatatypeModel`. Subclasses may be used for `Model` fields.

A `DatatypeModel` must implement the following:

- `constructor with arguments (String value, String datatype)`

  This is not explicitly described in the interface definition due to language limitations, but it is retrieved by
  reflection at runtime and used by the framework. The `value` and `datatype` provided in invocation are respectively
  the `?value` and `(DATATYPE(?value) AS ?datatype)` returned by a query to the database.

- `org.apache.jena.graph.Node getNode()`

  Returns a Jena `Node` object encoding this object. This should be reversible with the constructor in the sense that if

    ```java
    Node node = obj1.getNode();
    MyDatatypeModel obj2 = new MyDatatypeModel(
        node.getLiteralLexicalForm(),
        node.getLiteralDatatypeUri()
    );
    ```

  then `obj1` and `obj2` should be exactly value-equivalent.

For an example, see `uk.ac.cam.cares.twa.cities.models.geo.GeometryType`.

## How the framework works: for maintenance and extension

### FieldKey

A `FieldKey` is a hashable, comparable object encoding the quad characterisation information in a `FieldAnnotation`. It
has fields:

- `predicate`: the full IRI of the predicate, copied or expanded from `FieldAnnotation.value`.
- `graphName`: short name of the graph, from `FieldAnnotation.value` if specified,
  else `ModelAnnotation.nativeGraphName`.
- `backward`: the same as `FieldAnnotation.backward`.

It serves as (a) a lookup key and (b) a sorting key for fields.

### FieldInterface

A `FieldInterface` is a class, not an interface in the syntactic sense. One is created for each field with
a `FieldAnnotation`. During construction, it builds and stores a collection of functions to interact with its target
field based on the field type and annotation information. These are:

- Builtin methods fetched by reflection:
    - `getter`: the Lombok getter of the field.
    - `setter`: the Lombok setter of the field.
- Custom "outer-dependent" functions:
    - `listConstructor`: the constructor for an empty list, only assigned if the field is a list.
    - `putter`: the action for consuming an input value; for a list, this appends, otherwise, it sets (overwrites).
- Custom "inner-dependent" functions:
    - `parser`: converts string input (e.g. from a query) into the field's type.
    - `nodeGetter`: converts an object of the field's type inot a Jena `Node`.
    - `minimiser`: converts an object of the field's type into a minimal representation for which
      if `nodeGetter(a).equals(nodeGetter(b))`, then `minimiser(a).equals(minimiser(b))`.

The methods exposed by `FieldInterface` wrap these functions for streamlined use by `Model`. They main ones are:

- `put`: wrapper for a composition of `putter`⋅`parser`.
- `clear`: sets the field to its default value, which is the output of `listConstructor` for a list, otherwise `null`.
- `getMinimised`: returns the output of `minimiser` on the field value, unless it is a list, in which case returns a
  list of the outputs of `minimiser` on each element.
- `getNode(s)`: returns the output of `nodeGetter` on the field value (for a list; returns an array of the outputs
  of `nodeGetter` on each element).

### MetaModel

A `MetaModel` is created for each `Model` subclass the first time an instance thereof is created; all future instances
will then link back to the same `MetaModel`. It may be thought of as the collected output of reflection-based runtime
annotation processing, which is not to be repeated for each model instance for performance reasons.

***Fundamentally, each `MetaModel` is the `FieldKey`-indexed collection of `FieldInterface`s for its target class, and
serves as the engine through which the `Model` base methods interact with the annotated fields declared by
subclasses.***

The main member is `fieldMap`, a `TreeMap<FieldKey, FieldInterface>`. This is the bread and butter of the Model
framework. The other fields in `MetaModel` are `scalarFields` and `vectorFields`, which are simply the scalar (non-list)
and vector (list) entries in `fieldMap` extracted for convenience.

The use of `TreeMap` is deliberate, since having `FieldKey` sorted by graph makes updating more compact.

### Bringing it all together

`MetaModel`, `FieldKey` and `FieldInterface` are leveraged together in the main methods provided by the `Model` base
class.

- On `pullAll`, the graph database is queried for all quads containing the model instance's IRI as the subject or
  object. Each row of the response is processed as such:
    - The predicate, graph and direction of the quad are compiled into a `FieldKey`.
    - The `FieldKey` is looked up in `metaModel.fieldMap` to retrieve the corresponding `FieldInterface`.
    - The value and datatype of the counterparty in the quad is injected into the instance by `fieldInterface.put`. The
      conversion to the field's Java type and handling of lists vs. non-lists is all black-boxed inside `FieldInterface`
      .
    - A minimised copy of the new value is retrieved by `fieldInterface.getMinimised` and saved in `cleanValues`.
- On `queuePushUpdate`, we iterate through `metaModel.fieldMap`, and for each entry,
    - The current value (minimised), retrieved via the `FieldInterface`, is compared to the counterpart in `cleanValues`
      to determine if the field is dirty.
    - If so, deletion of the existing quad(s) and insertion of the new quad(s) are queued
      using `fieldInterface.getNode(s)`.
    - There is actually some complexity in ordering the updates to obtain the desired behaviour in edge cases; see code
      comments for more detail.
    - The new values are written to `cleanValues`.

The other methods work similarly, with `metaModel.scalarFields` and `metaModel.vectorFields` used in
specific `pullScalars` and `pullVector` methods.

One thing to note is the `dirtyAll` method, which forces all fields to be considered dirty; this is called in the
constructor so ex nihilo model instances will always write everything on push. This is implemented by setting every
entry in `cleanValues` to an object which no `FieldInterface.getMinimised` will produce; at this time, `Object.class` is
used.