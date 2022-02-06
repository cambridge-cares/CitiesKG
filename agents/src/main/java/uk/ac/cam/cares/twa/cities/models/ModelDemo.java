package uk.ac.cam.cares.twa.cities.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Random;

public class ModelDemo {

  public static class Employee extends Model {

    @Getter @Setter @FieldAnnotation("http://example.org/ontology#hasName")
    protected String name;

    @Getter @Setter @FieldAnnotation("http://example.org/ontology#hasAge")
    protected Integer age;

    @Getter @Setter @FieldAnnotation(value = "http://example.org/ontology#manages", backward = true)
    protected Employee manager;

    @Getter @Setter @FieldAnnotation(value = "http://example.org/ontology#manages", innerType = Employee.class)
    protected ArrayList<Employee> subordinates;

  }

  public static void main(String[] args) {
    createOrgChart();
    fireAllExecutives();
  }

  private static final String[] givenNames = {"Stanley", "Anthony", "George", "Alexander", "Alice", "Elizabeth", "Jacqueline", "Ellen"};
  private static final String[] surnames = {"Bragg", "Fresnel", "Feynman", "Kepler", "Goodall", "Franklin", "Curie", "Turing"};

  private static String randomName(Random random) {
    return givenNames[random.nextInt(8)] + " " + surnames[random.nextInt(8)];
  }

  public static void createOrgChart() {

    // Create context to work in, and also clear any old existing data
    ModelContext context = new ModelContext("HARDCODE:http://localhost:9999/blazegraph/namespace/test/sparql");
    context.update("CLEAR ALL");

    // Random data source
    Random random = new Random(32);

    // MyCompany has a young 27-year-old CEO, Albert Einstein. He is the only person who is his own boss.
    Employee ceo = context.createNewModel(Employee.class, "http://mycompany.org/people/0");
    ceo.name = "Albert Einstein";
    ceo.age = 27;
    ceo.manager = ceo;
    ceo.subordinates.add(ceo);
    // There are 8 directors working under Albert Einstein.
    int employeeId = 1;
    for (int i = 0; i < 8; i++) {
      Employee director = context.createNewModel(Employee.class, "http://mycompany.org/people/" + (employeeId++));
      director.name = randomName(random);
      director.age = 25 + random.nextInt(20);
      director.manager = ceo;
      ceo.subordinates.add(director);

      // Each director has 5 analysts working under them.
      for (int j = 0; j < 5; j++) {
        Employee analyst = context.createNewModel(Employee.class, "http://mycompany.org/people/" + (employeeId++));
        analyst.name = randomName(random);
        analyst.age = 20 + random.nextInt(20);
        analyst.manager = director;
        director.subordinates.add(analyst);
      }
    }

    // Notice how we created our employees through context.createNewModel --- they are registered to our context, so
    // we only need a single line to push the entirety of the created org chart.
    context.pushAllChanges();

  }

  private static void fireAllExecutives() {

    // Start completely fresh with a new context, to simulate operating on a previously existing knowledge graph.
    ModelContext context = new ModelContext("HARDCODE:http://localhost:9999/blazegraph/namespace/test/sparql");

    // Load data from the database
    Employee ceo = context.recursiveLoadPartialModel(
        Employee.class,
        "http://mycompany.org/people/0", // assume that we know a priori that this is the CEO
        99,                    // our tree is only 3 layers deep, so any number here >= 3 is equivalent.
        "manager", "subordinates", "age"); // specify to only load hierarchy and age data for now

    /* Since our knowledge graph is pretty sparse, we could also use recursiveLoadModel to load all properties at not
     * significantly greater cost, but in a real company database, there would be a lot of data coming along we don't
     * really need. Non-partial pulls and loads (they have the same underlying back end) are expensive proportional to
     * the number of triples/quads linked to the IRI(s) being pulled/loaded, regardless of whether they are defined in
     * the model. Partial pulls and loads, as used here, are by default slower, but scale with the number of fields
     * requested, and to a much lesser extent the number of fields in the Model. Partial pull with all Model fields
     * requested (no fieldNames requested will do this) has a valid use case if the latter time complexity is needed. */

    // Let's say that the company is restructuring. Albert Einstein has fired all the directors under him, and wants
    // to promote the analyst under each deposed director to replace them.

    for (Employee director : new ArrayList<>(ceo.subordinates)) {
      if(director == ceo) continue;
      // Identify oldest analyst under director
      Employee oldestAnalyst = director.subordinates.get(0);
      for (Employee analyst : director.subordinates)
        if (analyst.age > oldestAnalyst.age)
          oldestAnalyst = analyst;
      // Erase director
      ceo.subordinates.remove(director);   // *----------------------- can be omitted: redundant with director.delete()
      director.delete();
      // Reassign promoted analyst to work directly under CEO.
      ceo.subordinates.add(oldestAnalyst);  // *---------------------- can be omitted: redundant with oldestAnalyst.manager = ceo
      oldestAnalyst.manager = ceo;
      // Reassign other analyst to be managed by promoted analyst.
      assert oldestAnalyst.subordinates.size() == 0;
      for (Employee analyst : director.subordinates) {
        if (analyst != oldestAnalyst) {
          oldestAnalyst.subordinates.add(analyst);  // *-------------- can be omitted: redundant with analyst.manager = oldestAnalyst
          analyst.manager = oldestAnalyst;
        }
      }
    }

    /* When we push, the context will identify only fields which have changed and write those updates.
     * Note that fields which have never been pulled will never be written, even if they have been modified --- they are
     * disabled. The exception is if the model was created by context.createNewModel, in which case all fields will be
     * written even if they are not changed (default values are null if not specified, which become blank nodes).
     *
     * This is why the three lines highlighted above may be omitted: only one end of a triple which is doubly described
     * in the system needs to be changed; the outdated end will not create a conflicting update because if left alone it
     * will not be observed as having been changed. However, if both were changed but not concordantly, then a conflict
     * *will* arise, resulting in undefined behaviour, so one must be careful, especially in more complex manipulations.
     *
     * In an actual application, consider defining a helper function in employee e.g.
     *
     * public void reparent(Employee newParent) {
     *   if(this.manager != null) this.manager.subordinates.remove(this);
     *   this.manager = newParent;
     *   if(newParent != null) newParent.subordinates.add(this);
     * }                                                                                                              */

    context.pushAllChanges();

  }

}
