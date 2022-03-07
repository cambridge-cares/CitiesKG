package uk.ac.cam.cares.twa.cities.models.test;

import junit.framework.TestCase;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * To run ModelContextTest, you need:
 * - A Blazegraph namespace at http://localhost:9999/blazegraph/namespace/test/sparql
 */
public class ModelContextTest extends TestCase {

  private static final String testResourceId = "HARDCODE:http://localhost:9999/blazegraph/namespace/test/sparql";
  private static final String testNamespace = "http://localhost:9999/blazegraph/namespace/test/sparql/";

  private static final MetaModel metaModel = MetaModel.get(TestModel.class);

  private int countTriples(ModelContext context) {
    JSONArray response = context.query("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }");
    return response.getJSONObject(0).getInt("count");
  }

  public void testPushNewObject() {
    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    context.update("CLEAR ALL");
    TestModel.createRandom(context, 12345, 3, 3).pushChanges();
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));
  }

  public void testPushChanges() {
    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    context.update("CLEAR ALL");
    TestModel model = TestModel.createRandom(context, 12345, 3, 3);
    model.pushChanges();
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    model.setDoubleProp(7.77);
    model.pushChanges();
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("DELETE"));
    Mockito.verify(context, Mockito.times(2)).update(Mockito.contains("doubleprop"));
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("stringprop"));
    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));
  }

  public void testPushPartialChanges() {
    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
    pullModel.pull("doubleProp", "uriProp");
    pullModel.setDoubleProp(7.77);
    pullModel.setStringProp("modified");
    pullModel.pushChanges();
    // Only fields which were pulled should ever be pushed, even if unpulled fields have been changed.
    Mockito.verify(pullContext).update(Mockito.contains("doubleprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
    // If we pull the string field, we should afterwards be able to change and push it.
    pullModel.pull("stringProp");
    pullModel.pushChanges();
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringProp"));
    pullModel.setStringProp("modified2");
    pullModel.pushChanges();
    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("stringprop"));
    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("doubleprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
  }

  public void testDelete() {

    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.update("CLEAR ALL");

    TestModel model1 = TestModel.createRandom(context, 12345, 3, 0);

    context.update("INSERT DATA { <" + model1.getIri() + "> <test> <data> }");
    int baseCount = countTriples(context);

    context.pushAllChanges();
    int firstModelCount = countTriples(context);

    TestModel model2 = TestModel.createRandom(context, 3152, 8, 0);
    context.pushAllChanges();
    int secondModelCount = countTriples(context);

    model1.delete(false);
    context.pushAllChanges();
    assertEquals(baseCount + secondModelCount - firstModelCount, countTriples(context));

    model2.delete(false);
    context.pushAllChanges();
    assertEquals(baseCount, countTriples(context));

    model1 = TestModel.createRandom(context, 12345, 3, 0);
    model1.pushChanges();
    assertEquals(firstModelCount, countTriples(context));

  }

  public void testDeleteZealous() {

    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.update("CLEAR ALL");

    TestModel model1 = TestModel.createRandom(context, 12345, 3, 0);
    context.pushAllChanges();
    context.update("INSERT DATA { <" + model1.getIri() + "> <test> <data> }");
    int firstModelCount = countTriples(context);

    TestModel model2 = TestModel.createRandom(context, 3152, 8, 0);
    context.pushAllChanges();
    int secondModelCount = countTriples(context);

    model1.delete(true);
    context.pushAllChanges();
    assertEquals(secondModelCount - firstModelCount, countTriples(context));

  }

  public void testPullAll() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.loadAll(TestModel.class, pushModel.getIri());
    assertEquals(pushModel, pullModel);
  }

  public void testPullRecursive() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.recursiveLoadAll(TestModel.class, pushModel.getIri(), 2);
    assertEquals(pushModel, pullModel);
    assertEquals(pushModel.getModelProp(), pullModel.getModelProp());
    assertEquals(pushModel.getModelProp().getModelProp(), pullModel.getModelProp().getModelProp());
    assertNotEquals(pushModel.getModelProp().getModelProp().getModelProp(), pullModel.getModelProp().getModelProp().getModelProp());

    ModelContext comparisonContext = new ModelContext(testResourceId, testNamespace);
    TestModel comparisonModel = comparisonContext.createHollowModel(TestModel.class, pushModel.getModelProp().getModelProp().getModelProp().getIri());
    assertEquals(comparisonModel, pullModel.getModelProp().getModelProp().getModelProp());
  }

  public void testPullScalars() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
    pullModel.pull("doubleProp", "uriProp");
    assertEquals(pushModel.getDoubleProp(), pullModel.getDoubleProp());
    assertEquals(pushModel.getUriProp(), pullModel.getUriProp());
    assertNotEquals(pushModel.getStringProp(), pullModel.getStringProp());
    pullModel.pull();
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.scalarFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  public void testPullVector() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
    pullModel.pull("backwardVector", "emptyForwardVector");
    assertEquals(new HashSet<>(pushModel.getBackwardVector()), new HashSet<>(pullModel.getBackwardVector()));
    assertEquals(new HashSet<>(pushModel.getEmptyForwardVector()), new HashSet<>(pullModel.getEmptyForwardVector()));
    assertNotEquals(new HashSet<>(pushModel.getForwardVector()), new HashSet<>(pullModel.getForwardVector()));
    pullModel.pull();
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  public void testLoadAllWhere() throws ParseException {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel.createRandom(pushContext, 12345, 0, 20);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    List<TestModel> pullModels = pullContext.pullAllWhere(TestModel.class,
        new WhereBuilder().addWhere(
            ModelContext.getModelVar(),
            NodeFactory.createURI(SPARQLUtils.expandQualifiedName("dbpediao:intprop")),
            "?intprop"
        ).addFilter("?intprop > 0")
    );
    for(Model pm: pushContext.members.values()) {
      TestModel pushModel = (TestModel) pm;
      TestModel pullModelMatch = null;
      for(TestModel pullModel: pullModels) {
        if(pullModel.getIri().equals(pushModel.getIri())) {
          if(pullModelMatch != null) fail();
          pullModelMatch = pullModel;
        }
      }
      if(pushModel.getIntProp() > 0) {
        assertNotNull(pullModelMatch);
        assertEquals(pullModelMatch, pushModel);
      } else {
        assertNull(pullModelMatch);
      }
    }
  }

  public void testRecursiveLoadAllWhere() throws ParseException {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel.createRandom(pushContext, 12345, 0, 20);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    List<TestModel> pullModels = pullContext.recursivePullAllWhere(TestModel.class,
        new WhereBuilder().addWhere(
            ModelContext.getModelVar(),
            NodeFactory.createURI(SPARQLUtils.expandQualifiedName("dbpediao:intprop")),
            "?intprop"
        ).addFilter("?intprop > 0"),
        1
    );
    for(Model pm: pushContext.members.values()) {
      TestModel pushModel = (TestModel) pm;
      TestModel pullModelMatch = null;
      for(TestModel pullModel: pullModels) {
        if(pullModel.getIri().equals(pushModel.getIri())) {
          if(pullModelMatch != null) fail();
          pullModelMatch = pullModel;
        }
      }
      if(pushModel.getIntProp() > 0) {
        assertNotNull(pullModelMatch);
        assertEquals(pullModelMatch, pushModel);
        assertFalse(pushModel.isHollow("modelProp"));
        if(pushModel.getModelProp() != null) {
          if (pushModel.getIntProp() <= 0 && pushModel.getModelProp().getIntProp() <= 0) {
            assertTrue(pushModel.getModelProp().isHollow("modelProp"));
          } else {
            assertFalse(pushModel.getModelProp().isHollow("modelProp"));
          }
        }
      } else {
        assertNull(pullModelMatch);
      }
    }
  }

  public void testLoadPartialWhere() throws ParseException {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel.createRandom(pushContext, 12345, 0, 20);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    List<TestModel> pullModels = pullContext.pullPartialWhere(TestModel.class,
        new WhereBuilder().addWhere(
            ModelContext.getModelVar(),
            NodeFactory.createURI(SPARQLUtils.expandQualifiedName("dbpediao:intprop")),
            "?intprop"
        ).addFilter("?intprop > 0"),
        "intProp",
        "backwardVector"
    );
    for(Model pm: pushContext.members.values()) {
      TestModel pushModel = (TestModel) pm;
      TestModel pullModelMatch = null;
      for(TestModel pullModel: pullModels) {
        if(pullModel.getIri().equals(pushModel.getIri())) {
          if(pullModelMatch != null) fail();
          pullModelMatch = pullModel;
        }
      }
      if(pushModel.getIntProp() > 0) {
        assertNotNull(pullModelMatch);
        assertEquals(pullModelMatch.getBackwardVector(), pushModel.getBackwardVector());
        assertEquals(pullModelMatch.getIntProp(), pushModel.getIntProp());
        assertNull(pullModelMatch.getStringProp());
        assertEquals(new ArrayList<Double>(), pullModelMatch.getForwardVector());
      } else {
        assertNull(pullModelMatch);
      }
    }
  }

  public void testRecursiveLoadPartialWhere() throws ParseException {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel.createRandom(pushContext, 12345, 0, 20);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    List<TestModel> pullModels = pullContext.pullPartialWhere(TestModel.class,
        new WhereBuilder().addWhere(
            ModelContext.getModelVar(),
            NodeFactory.createURI(SPARQLUtils.expandQualifiedName("dbpediao:intprop")),
            "?intprop"
        ).addFilter("?intprop > 0"),
        "intProp",
        "modelProp",
        "backwardVector"
    );
    for(Model pm: pushContext.members.values()) {
      TestModel pushModel = (TestModel) pm;
      TestModel pullModelMatch = null;
      for(TestModel pullModel: pullModels) {
        if(pullModel.getIri().equals(pushModel.getIri())) {
          if(pullModelMatch != null) fail();
          pullModelMatch = pullModel;
        }
      }
      if(pushModel.getIntProp() > 0) {
        assertNotNull(pullModelMatch);
        assertEquals(pullModelMatch.getBackwardVector(), pushModel.getBackwardVector());
        assertEquals(pullModelMatch.getIntProp(), pushModel.getIntProp());
        assertNull(pullModelMatch.getStringProp());
        assertEquals(new ArrayList<Double>(), pullModelMatch.getForwardVector());
        assertFalse(pushModel.isHollow("modelProp"));
        if(pushModel.getModelProp() != null) {
          if (pushModel.getIntProp() <= 0 && pushModel.getModelProp().getIntProp() <= 0) {
            assertTrue(pushModel.getModelProp().isHollow("modelProp"));
          } else {
            assertFalse(pushModel.getModelProp().isHollow("modelProp"));
          }
        }
      } else {
        assertNull(pullModelMatch);
      }
    }
  }

  public void testRetire() {
    ModelContext context = new ModelContext(testResourceId, testNamespace);
    TestModel model1 = TestModel.createRandom(context, 1234, 7, 0);
    try {
      TestModel.createRandom(context, 1234, 7, 0);
      fail();
    } catch (JPSRuntimeException ignored) {
    }
    model1.retire();
    TestModel model2 = TestModel.createRandom(context, 1234, 7, 0);
    assertNotSame(model1, model2);
  }

  public void testEquals() {
    ModelContext context1 = new ModelContext(testResourceId, testNamespace);
    ModelContext context2 = new ModelContext(testResourceId, testNamespace);
    TestModel model1 = TestModel.createRandom(context1, 1234, 7, 2);
    TestModel model2 = TestModel.createRandom(context2, 1234, 7, 2);
    assertEquals(model1, model2);
    model2.getModelProp().setStringProp("modified string 1");
    assertEquals(model1, model2);
    model2.setModelProp(TestModel.createRandom(context2, 12, 0, 1));
    assertNotEquals(model1, model2);
    model1.setModelProp(TestModel.createRandom(context1, 12, 0, 1));
    assertEquals(model1, model2);
    model2.setStringProp("modified string 2");
    assertNotEquals(model1, model2);
  }

  public void testIRICollision() {
    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.createNewModel(TestModel.class, "test");
    try {
      context.createNewModel(TestModel.class, "test");
      fail();
    } catch (JPSRuntimeException ignored) {
    }
  }

  public void testQuadModelInTripleContext() {
    ModelContext context = new ModelContext(testResourceId);
    try {
      context.createNewModel(TestModel.class, "test");
      fail();
    } catch (JPSRuntimeException ignored) {
    }
  }

  public static class TripleTestModel extends Model {
    @Getter @Setter @FieldAnnotation("JPSLAND:stringprop") private String stringProp;
    @Getter @Setter @FieldAnnotation("dbpediao:intprop") private Integer intProp;
    @Getter @Setter @FieldAnnotation("dbpediao:doubleprop") private Double doubleProp;
    @Getter @Setter @FieldAnnotation(value = "dbpediao:forwardvector", innerType = Double.class) private ArrayList<Double> forwardVector;
    @Getter @Setter @FieldAnnotation(value = "dbpediao:emptyforwardvector", innerType = Double.class) private ArrayList<Double> emptyForwardVector;
    @Getter @Setter @FieldAnnotation(value = "dbpediao:backwardVector", backward = true, innerType = URI.class) private ArrayList<URI> backwardVector;
  }

  public void testTripleModelPushPullAll() {
    ModelContext pushContext = new ModelContext(testResourceId);
    pushContext.update("CLEAR ALL");
    TripleTestModel pushModel = pushContext.createNewModel(TripleTestModel.class, "http://testiri.com/test");
    pushModel.setStringProp("teststring");
    pushModel.getForwardVector().add(2.77);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId);
    TripleTestModel pullModel = pullContext.loadAll(TripleTestModel.class, pushModel.getIri());
    assertEquals(pushModel, pullModel);
  }

  public void testTripleModelPullScalarsVectors() {
    ModelContext pushContext = new ModelContext(testResourceId);
    pushContext.update("CLEAR ALL");
    TripleTestModel pushModel = pushContext.createNewModel(TripleTestModel.class, "http://testiri.com/test");
    pushModel.setStringProp("teststring");
    pushModel.setDoubleProp(77.11);
    pushModel.getForwardVector().add(2.77);
    pushModel.getBackwardVector().add(URI.create("http://testiri.com/testuri"));
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId);
    TripleTestModel pullModel = pullContext.loadPartial(TripleTestModel.class, pushModel.getIri(), "stringProp", "forwardVector");
    assertNotEquals(pushModel, pullModel);
    assertEquals(pushModel.getStringProp(), pullModel.getStringProp());
    assertNotEquals(pushModel.getDoubleProp(), pullModel.getDoubleProp());
    assertEquals(pushModel.getForwardVector(), pullModel.getForwardVector());
    assertNotEquals(pushModel.getBackwardVector(), pullModel.getBackwardVector());
  }

}
