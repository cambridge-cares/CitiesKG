package uk.ac.cam.cares.twa.cities.models.test;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * To run ModelContextTest, you need:
 * - A Blazegraph namespace at http://localhost:9999/blazegraph/namespace/test/sparql
 */
public class ModelContextTest {

  private static final String testResourceId = "http://localhost:48080/test";
  private static final String testNamespace = "http://localhost:9999/blazegraph/namespace/test/sparql/";

  private static final MetaModel metaModel = MetaModel.get(TestModel.class);

  private int countTriples(ModelContext context) {
    JSONArray response = context.query("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }");
    return response.getJSONObject(0).getInt("count");
  }

  @Test
  public void testPushNewObject() {
//    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
//    context.update("CLEAR ALL");
//    TestModel.createRandom(context, 12345, 3, 3).pushChanges();
//    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
//    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));

    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(context).update(Mockito.contains("CLEAR ALL"));
    Mockito.doNothing().when(context).update(Mockito.contains("INSERT DATA"));

    context.update("CLEAR ALL");
    TestModel testModel = Mockito.spy(TestModel.createRandom(context, 12345, 3, 3));
    testModel.pushChanges();

    Mockito.verify(context, Mockito.times(1)).update("CLEAR ALL");
    Mockito.verify(testModel, Mockito.times(1)).pushChanges();
    Mockito.verify(context, Mockito.times(1)).pushChanges(testModel);
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));

    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "26"))).when(context).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));

  }

  @Test
  public void testPushChanges() {
//    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
//    context.update("CLEAR ALL");
//    TestModel model = TestModel.createRandom(context, 12345, 3, 3);
//    model.pushChanges();
//    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
//    model.setDoubleProp(7.77);
//    model.pushChanges();
//    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("DELETE"));
//    Mockito.verify(context, Mockito.times(2)).update(Mockito.contains("doubleprop"));
//    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("stringprop"));
//    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));

    ModelContext context = Mockito.spy((new ModelContext(testResourceId, testNamespace)));

    Mockito.doNothing().when(context).update(Mockito.contains("CLEAR ALL"));
    Mockito.doNothing().when(context).update(Mockito.contains("INSERT DATA"));

    context.update("CLEAR ALL");

    TestModel testModel = Mockito.spy(TestModel.createRandom(context, 12345, 3, 3));
    testModel.pushChanges();
    testModel.setDoubleProp(7.77);
    testModel.pushChanges();

    Mockito.verify(context, Mockito.times(1)).update("CLEAR ALL");
    Mockito.verify(context, Mockito.times(2)).update(Mockito.contains("INSERT DATA"));
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("DELETE"));
    Mockito.verify(context, Mockito.times(2)).update(Mockito.contains("doubleprop"));
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("stringprop"));

    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "26"))).when(context).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));
  }

  @Test
  public void testPushPartialChanges() {
//    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
//    pushContext.update("CLEAR ALL");
//    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
//    pushContext.pushAllChanges();
//
//    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
//    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
//    pullModel.pull("doubleProp", "uriProp");
//    pullModel.setDoubleProp(7.77);
//    pullModel.setStringProp("modified");
//    pullModel.pushChanges();
////     Only fields which were pulled should ever be pushed, even if unpulled fields have been changed.
//    Mockito.verify(pullContext).update(Mockito.contains("doubleprop"));
//    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringprop"));
//    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
////     If we pull the string field, we should afterwards be able to change and push it.
//    pullModel.pull("stringProp");
//    pullModel.pushChanges();
//    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringProp"));
//    pullModel.setStringProp("modified2");
//    pullModel.pushChanges();
//    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("stringprop"));
//    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("doubleprop"));
//    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
//

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TestModel pushModel = Mockito.spy(TestModel.createRandom(pushContext, 12345, 3, 3));
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    TestModel pullModel = Mockito.spy(pullContext.createHollowModel(TestModel.class, pushModel.getIri()));
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("value4", "https://eg/examplenamespace/randomuris/1402202751");
    jsonObject.put("value3", "0.8330913489710237");
    jsonObject.put("isblank4", "false");
    jsonObject.put("isblank3", "false");
    jsonObject.put("datatype3", "http://www.w3.org/2001/XMLSchema#double");

    Method buildScalarsQuery = null;
    SelectBuilder scalarsQuery = null;
    try {
      buildScalarsQuery = pullContext.getClass().getDeclaredMethod("buildScalarsQuery", Node.class, MetaModel.class, String[].class);
      buildScalarsQuery.setAccessible(true);
      scalarsQuery = (SelectBuilder) buildScalarsQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), metaModel, new String[]{"doubleProp", "uriProp"});
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(new JSONArray().put(jsonObject)).when(pullContext).query(Mockito.contains(scalarsQuery.buildString()));
    pullModel.pull("doubleProp", "uriProp");
    Mockito.verify(pullContext, Mockito.times(1)).query(scalarsQuery.buildString());

    pullModel.setDoubleProp(7.77);
    pullModel.setStringProp("modified");
    Mockito.doNothing().when(pullContext).update(Mockito.contains("INSERT DATA"));
    Mockito.doNothing().when(pullContext).update(Mockito.contains("DELETE"));
    pullModel.pushChanges();
    // Only fields which were pulled should ever be pushed, even if unpulled fields have been changed.

    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringProp"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriProp"));

    JSONObject jsonObject1 = new JSONObject();
    jsonObject1.put("isblank0", "false");
    jsonObject1.put("value0", "randomString-287790814");
    jsonObject1.put("datatype0", "\"http://www.w3.org/2001/XMLSchema#string\"");
    try {
      scalarsQuery = (SelectBuilder) buildScalarsQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), metaModel, new String[]{"stringProp"});
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(new JSONArray().put(jsonObject1)).when(pullContext).query(Mockito.contains(scalarsQuery.buildString()));
    pullModel.pull("stringProp");
    Mockito.verify(pullContext, Mockito.times(2)).query(Mockito.contains("SELECT"));

    // If we pull the string field, we should afterwards be able to change and push it.
    pullModel.pushChanges();
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringProp"));

    pullModel.setStringProp("modified2");
    pullModel.pushChanges();
    Mockito.verify(pullContext, Mockito.times(2)).update(Mockito.contains("INSERT DATA"));
    Mockito.verify(pullContext, Mockito.times(2)).update(Mockito.contains("DELETE"));
    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("stringprop"));
    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("doubleprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
  }

  @Test
  public void testDelete() {

    ModelContext context1 = Mockito.spy((new ModelContext(testResourceId, testNamespace)));
    TestModel testModel1 = TestModel.createRandom(context1, 12345, 3, 0);
    String query = "INSERT DATA { <" + testModel1.getIri() + "> <test> <data> }";

    Mockito.doNothing().when(context1).update(Mockito.contains("CLEAR ALL"));
    Mockito.doNothing().when(context1).update(Mockito.contains("INSERT DATA"));
    Mockito.doNothing().when(context1).update(Mockito.contains("DELETE WHERE"));
    Mockito.doNothing().when(context1).update(query);

    context1.update("CLEAR ALL");
    Mockito.verify(context1, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    context1.update(query);
    Mockito.verify(context1, Mockito.times(1)).update(query);

    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "1"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    int baseCount = countTriples(context1);

    context1.pushAllChanges();
    Mockito.verify(context1, Mockito.times(2)).update(Mockito.contains("INSERT DATA"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "27"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    int firstModelCount = countTriples(context1);

    TestModel testModel2 = TestModel.createRandom(context1, 3152, 8, 0);
    context1.pushAllChanges();

    Mockito.verify(context1, Mockito.times(3)).update(Mockito.contains("INSERT DATA"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "63"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    int secondModelCount = countTriples(context1);

    testModel1.delete(false);
    context1.pushAllChanges();

    Mockito.verify(context1, Mockito.times(1)).update(Mockito.contains("DELETE WHERE"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "37"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    assertEquals(baseCount + secondModelCount - firstModelCount, countTriples(context1));

    testModel2.delete(false);
    context1.pushAllChanges();

    Mockito.verify(context1, Mockito.times(2)).update(Mockito.contains("DELETE WHERE"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "1"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    assertEquals(baseCount, countTriples(context1));

    testModel1 = Mockito.spy(TestModel.createRandom(context1, 12345, 3, 0));
    testModel1.pushChanges();

    Mockito.verify(context1, Mockito.times(4)).update(Mockito.contains("INSERT DATA"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "27"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    assertEquals(firstModelCount, countTriples(context1));
  }

  @Test
  public void testDeleteZealous() {

    ModelContext context1 = Mockito.spy((new ModelContext(testResourceId, testNamespace)));
    TestModel testModel1 = TestModel.createRandom(context1, 12345, 3, 0);
    String query = "INSERT DATA { <" + testModel1.getIri() + "> <test> <data> }";

    Mockito.doNothing().when(context1).update(Mockito.contains("CLEAR ALL"));
    Mockito.doNothing().when(context1).update(Mockito.contains("INSERT DATA"));
    Mockito.doNothing().when(context1).update(Mockito.contains("DELETE WHERE"));
    Mockito.doNothing().when(context1).update(query);

    context1.update("CLEAR ALL");
    Mockito.verify(context1, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));
    context1.pushAllChanges();
    Mockito.verify(context1, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));
    context1.update(query);
    Mockito.verify(context1, Mockito.times(1)).update(query);

    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "27"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    int firstModelCount = countTriples(context1);

    TestModel testModel2 = TestModel.createRandom(context1, 3152, 8, 0);
    context1.pushAllChanges();

    Mockito.verify(context1, Mockito.times(3)).update(Mockito.contains("INSERT DATA"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "63"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    int secondModelCount = countTriples(context1);

    testModel1.delete(false);
    context1.pushAllChanges();

    Mockito.verify(context1, Mockito.times(1)).update(Mockito.contains("DELETE WHERE"));
    Mockito.doReturn(new JSONArray().put(new JSONObject().put("count", "36"))).when(context1).query(Mockito.contains("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    assertEquals(secondModelCount - firstModelCount, countTriples(context1));
  }

  public JSONArray creatResponse1_0(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string")
            .put("isblank","false").put("value", "randomString-287790814").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop")
                    .put("isblank","true").put("value", "1813e294f9ac91753c408b278af3ebf2").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.8330913489710237").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull")
                    .put("isblank","true").put("value", "2e99c7c53fa664fd2da413c947485199").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.34911535662488336").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.9138466810904882").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector")
                    .put("isblank","true").put("value", "a37e6b8477e475c9b7ed4e3497a6d451").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a")
                    .put("isblank","true").put("value", "a6abda03daafd007a4bbdf58ded07a0f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a")
                    .put("isblank","true").put("value", "f6e65041ac5f56aa66d28fe69ea31c3d").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b")
                    .put("isblank","true").put("value", "d57dfd33a11e22b9c20d4469d0a20100").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a")
                    .put("isblank","true").put("value", "a3fb68e7cf404de42e7d3962cc94ac97").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b")
                    .put("isblank","true").put("value", "d05a9ab886eb7d5e612261809f3ded33").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c")
                    .put("isblank","true").put("value", "804d12f31819430074ade016221c0475").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int")
                    .put("isblank","false").put("value", "-355989640").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull")
                    .put("isblank","true").put("value", "16343243de676dbe359a8f321ac87733").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop")
                    .put("isblank","false").put("value", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull")
                    .put("isblank","true").put("value", "9697279a4bf1792f251430e0eb92f034").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop")
                    .put("isblank","false").put("value", "https://eg/examplenamespace/randomuris/1402202751").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull")
                    .put("isblank","true").put("value", "f43b45e47362f4e6e44bf62478b597b6").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop")
                    .put("isblank","true").put("value", "43b6bcab4c10dcdc83428db841d08f72").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull")
                    .put("isblank","true").put("value", "e478f5f6d6f8fcea89cd1db9e16e1c25").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  public JSONArray createResponse2_0(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/randomuris/151766778").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/randomuris/1924478780").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "4560b9a575b30e0ee06a61fb884974ba").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "5a9dfea5945f93c5d5df9fc2148042cb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank","true")
                    .put("value", "ceb0f5eeab1e153aca56a0e3a0058f9a").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  public JSONArray createResponse1_1(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string")
                    .put("isblank","false").put("value", "randomString1525380402").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop")
                    .put("isblank","true").put("value", "41a067e8afcec196bdbddee743dacd40").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.3475288646103122").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull")
                    .put("isblank","true").put("value", "bf19f5854c3112433370def2ad7b5c0e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.13561788626175464").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.517410013055376").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector")
                    .put("isblank","true").put("value", "cbf1df26d860cd93cc3daecc5684c2f8").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a")
                    .put("isblank","true").put("value", "b6aec88ed2a222ab764da6094f23d244").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a")
                    .put("isblank","true").put("value", "7636e2c8e1ad4ddabd70874a219ed3e7").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b")
                    .put("isblank","true").put("value", "ca04894217d214407bd8a4703120339e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a")
                    .put("isblank","true").put("value", "7b6f60c56e66475ea7572aa53da9b026").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b")
                    .put("isblank","true").put("value", "fc92a3d244ee0d7989b507ebdd0af4c6").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c")
                    .put("isblank","true").put("value", "0168100e4034105d94579cacc4335de3").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int")
                    .put("isblank","false").put("value", "1094996859").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull")
                    .put("isblank","true").put("value", "7106ec445b1caab74d823381bec5d5fd").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop")
                    .put("isblank","false").put("value", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull")
                    .put("isblank","true").put("value", "6ebe8a29db927b016e96f56e80d2a748").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop")
                    .put("isblank","false").put("value", "https://eg/examplenamespace/randomuris/1905807410").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull")
                    .put("isblank","true").put("value", "44c7dd444cd430d7e07cfee72d8a7fd1").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop")
                    .put("isblank","true").put("value", "94cd358a2d2f0e6aae8b6fb60033b893").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull")
                    .put("isblank","true").put("value", "6153ef31a84619132f8191795213f661").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  public JSONArray createResponse2_1(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/4bb4eeac-5793-3eee-b6d5-d453b9f759bd").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/randomuris/1355381216").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/randomuris/1657489626").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "3924e67c277f76a9c892abff45095f96").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "4560b9a575b30e0ee06a61fb884974ba").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "44ac5a1593f786f20a8412a42e26832f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank","true")
                    .put("value", "30e7ef8d9d2abe73add84117331dca61").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  public JSONArray createResponse1_2(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string")
                    .put("isblank","false").put("value", "randomString-2050421886").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop")
                    .put("isblank","true").put("value", "41a067e8afcec196bdbddee743dacd40").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.666633888087799").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull")
                    .put("isblank","true").put("value", "bf19f5854c3112433370def2ad7b5c0e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.023474617533402298").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype","http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank","false").put("value", "0.034685238715615796").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector")
                    .put("isblank","true").put("value", "cbf1df26d860cd93cc3daecc5684c2f8").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a")
                    .put("isblank","true").put("value", "b6aec88ed2a222ab764da6094f23d244").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a")
                    .put("isblank","true").put("value", "7636e2c8e1ad4ddabd70874a219ed3e7").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b")
                    .put("isblank","true").put("value", "ca04894217d214407bd8a4703120339e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a")
                    .put("isblank","true").put("value", "7b6f60c56e66475ea7572aa53da9b026").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b")
                    .put("isblank","true").put("value", "fc92a3d244ee0d7989b507ebdd0af4c6").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c")
                    .put("isblank","true").put("value", "0168100e4034105d94579cacc4335de3").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int")
                    .put("isblank","false").put("value", "486786104").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull")
                    .put("isblank","true").put("value", "7106ec445b1caab74d823381bec5d5fd").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop")
                    .put("isblank","false").put("value", "https://eg/examplenamespace/1d5fd7f8-5cb6-3dcf-88d8-edadc309dc81").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull")
                    .put("isblank","true").put("value", "6ebe8a29db927b016e96f56e80d2a748").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop")
                    .put("isblank","false").put("value", "https://eg/examplenamespace/randomuris/-727373186").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull")
                    .put("isblank","true").put("value", "44c7dd444cd430d7e07cfee72d8a7fd1").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop")
                    .put("isblank","true").put("value", "94cd358a2d2f0e6aae8b6fb60033b893").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull")
                    .put("isblank","true").put("value", "6153ef31a84619132f8191795213f661").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  public JSONArray createResponse2_2(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/randomuris/-745292122").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank","false")
                    .put("value", "https://eg/examplenamespace/randomuris/874244884").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "3924e67c277f76a9c892abff45095f96").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "4560b9a575b30e0ee06a61fb884974ba").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/backwardVector").put("isblank","true")
                    .put("value", "44ac5a1593f786f20a8412a42e26832f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank","true")
                    .put("value", "30e7ef8d9d2abe73add84117331dca61").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  @Test
  public void testPullAll() {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    JSONArray jsonArray1 = creatResponse1_0();
    JSONArray jsonArray2 = createResponse2_0();


    Method buildQuery;
    SelectBuilder query1 = null;
    SelectBuilder query2 = null;
    try {
      buildQuery = pullContext.getClass().getDeclaredMethod("buildPullAllInDirectionQuery", Node.class, boolean.class);
      buildQuery.setAccessible(true);
      query1 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), false);
      query2 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), true);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(query1.buildString()));
    Mockito.doReturn(jsonArray2).when(pullContext).query(Mockito.contains(query2.buildString()));

    TestModel pullModel = pullContext.loadAll(TestModel.class, pushModel.getIri());
    Mockito.verify(pullContext, Mockito.times(1)).query(query1.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query2.buildString());
    assertEquals(pushModel, pullModel);

  }

  @Test
  public void testPullRecursive() {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    JSONArray jsonArray1_0 = creatResponse1_0();
    JSONArray jsonArray2_0 = createResponse2_0();
    JSONArray jsonArray1_1 = createResponse1_1();
    JSONArray jsonArray2_1 = createResponse2_1();
    JSONArray jsonArray1_2 = createResponse1_2();
    JSONArray jsonArray2_2 = createResponse2_2();

    Method buildQuery;
    SelectBuilder query1_0 = null;
    SelectBuilder query2_0 = null;
    SelectBuilder query1_1 = null;
    SelectBuilder query2_1 = null;
    SelectBuilder query1_2 = null;
    SelectBuilder query2_2 = null;

    try {
      buildQuery = pullContext.getClass().getDeclaredMethod("buildPullAllInDirectionQuery", Node.class, boolean.class);
      buildQuery.setAccessible(true);
      query1_0 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), false);
      query2_0 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), true);
      query1_1 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getModelProp().getIri()), false);
      query2_1 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getModelProp().getIri()), true);
      query1_2 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getModelProp().getModelProp().getIri()), false);
      query2_2 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getModelProp().getModelProp().getIri()), true);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(jsonArray1_0).when(pullContext).query(Mockito.contains(query1_0.buildString()));
    Mockito.doReturn(jsonArray2_0).when(pullContext).query(Mockito.contains(query2_0.buildString()));
    Mockito.doReturn(jsonArray1_1).when(pullContext).query(Mockito.contains(query1_1.buildString()));
    Mockito.doReturn(jsonArray2_1).when(pullContext).query(Mockito.contains(query2_1.buildString()));
    Mockito.doReturn(jsonArray1_2).when(pullContext).query(Mockito.contains(query1_2.buildString()));
    Mockito.doReturn(jsonArray2_2).when(pullContext).query(Mockito.contains(query2_2.buildString()));


    TestModel pullModel = pullContext.recursiveLoadAll(TestModel.class, pushModel.getIri(), 2);

    Mockito.verify(pullContext, Mockito.times(1)).query(query1_0.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query2_0.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query1_1.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query2_1.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query1_2.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query2_2.buildString());


    assertEquals(pushModel, pullModel);
    assertEquals(pushModel.getModelProp(), pullModel.getModelProp());
    assertEquals(pushModel.getModelProp().getModelProp(), pullModel.getModelProp().getModelProp());
    assertNotEquals(pushModel.getModelProp().getModelProp().getModelProp(), pullModel.getModelProp().getModelProp().getModelProp());

    ModelContext comparisonContext = new ModelContext(testResourceId, testNamespace);
    TestModel comparisonModel = comparisonContext.createHollowModel(TestModel.class, pushModel.getModelProp().getModelProp().getModelProp().getIri());
    assertEquals(comparisonModel, pullModel.getModelProp().getModelProp().getModelProp());
  }

  public JSONArray createResponseForTestPullScalars_1(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("value4", "https://eg/examplenamespace/randomuris/1402202751").put("value3","0.8330913489710237")
                    .put("isblank4", "false").put("isblank3", "false").put("datatype3", "http://www.w3.org/2001/XMLSchema#double"));
    return jsonArray;
  }

  public JSONArray createResponseForTestPullScalars_2(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("value6", "8054a516eb0379af364ca07a3760e132").put("value5","https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d")
                    .put("value8", "4bfab0f0f8e58a85686560be3882a8be").put("value7", "fa00cebb2454bfc15e0c920d8e0e374c").put("value2", "7c86355ade39e976269d9c2ac529a6d3")
                    .put("value1", "-355989640").put("value4", "https://eg/examplenamespace/randomuris/1402202751").put("value3", "0.8330913489710237")
                    .put("datatype1", "http://www.w3.org/2001/XMLSchema#int").put("value9", "7c6244ed15fce769ae0377b20eba0849").put("datatype0", "http://www.w3.org/2001/XMLSchema#string")
                    .put("datatype3", "http://www.w3.org/2001/XMLSchema#double").put("value19", "5c1833d952d595b71461c4e2742f52bc").put("value18", "634e25607bb6e671ca0b39c3b13a60f7")
                    .put("value15", "87542be53d21b81e41a899eb6e741c3b").put("value14", "9e3915fadc00646ba1566ed23f8e2a2b").put("value17", "12f21ce1ddf2f64db529f9c0a83ae843")
                    .put("value16", "3f323345081eaf2cc1e37bbc854658db").put("value11", "d09395becdab11b4f2652ebd7e9613ec").put("value10", "fbfe8ac83e80eb5db7ecdf7664214938")
                    .put("value13", "ac9355a9a7ae457b7c89f8bd9a1424e7").put("value12", "https://eg/examplenamespace/randomuris/151766778").put("isblank18", "true")
                    .put("isblank17", "true").put("isblank19", "true").put("isblank0", "false").put("isblank1", "false").put("isblank4", "false").put("isblank5", "false")
                    .put("isblank2", "true").put("isblank3", "false").put("isblank8", "true").put("isblank9", "true").put("isblank6", "true").put("isblank7", "true")
                    .put("isblank10", "true").put("value0", "randomString-287790814").put("isblank12", "false").put("isblank11", "true").put("isblank14", "true").put("isblank13", "true")
                    .put("isblank16", "true").put("isblank15", "true"));
    return jsonArray;
  }

  public JSONArray forwardvectorResponse(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("datatype", "http://www.w3.org/2001/XMLSchema#double").put("isblank","false").put("value", "0.34911535662488336"))
            .put(new JSONObject().put("datatype", "http://www.w3.org/2001/XMLSchema#double").put("isblank", "false").put("value", "0.9138466810904882"))
            .put(new JSONObject().put("isblank", "true").put("value", "af0f3876990654840d89538a90bbc54c"));

    return jsonArray;
  }

  public JSONArray backwardvectorResponse(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("isblank","false").put("value", "https://eg/examplenamespace/randomuris/1924478780"))
            .put(new JSONObject().put("isblank","true").put("value", "e636a2847c83a665ba5a8b1049c7527f"))
            .put(new JSONObject().put("isblank","true").put("value", "e56cc5e09e37ce073bd25f5085e2b76c"));

    return jsonArray;
  }

  @Test
  public void testPullScalars() {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TestModel pushModel = Mockito.spy(TestModel.createRandom(pushContext, 12345, 3, 3));
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    TestModel pullModel = Mockito.spy(pullContext.createHollowModel(TestModel.class, pushModel.getIri()));

    JSONArray jsonArray = createResponseForTestPullScalars_1();
    JSONArray jsonArray1 = createResponseForTestPullScalars_2();
    JSONArray jsonArray2 = new JSONArray();
    JSONArray jsonArray3 = forwardvectorResponse();
    JSONArray jsonArray4 = backwardvectorResponse();

    Method buildScalarsQuery;
    SelectBuilder scalarsQuery = null;
    SelectBuilder query = null;
    Method buildVectorQuery;
    List<String> vectorQuery = new ArrayList<>();
    try {
      buildScalarsQuery = pullContext.getClass().getDeclaredMethod("buildScalarsQuery", Node.class, MetaModel.class, String[].class);
      buildScalarsQuery.setAccessible(true);
      buildVectorQuery = pullContext.getClass().getDeclaredMethod("buildVectorQuery", Node.class, FieldKey.class);
      buildVectorQuery.setAccessible(true);

      scalarsQuery = (SelectBuilder) buildScalarsQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), metaModel, new String[]{"doubleProp", "uriProp"});
      query = (SelectBuilder) buildScalarsQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), metaModel, new String[0]);

      for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
        if (new String[0].length > 0 && !ArrayUtils.contains(new String[0], entry.getValue().field.getName())) continue;
        vectorQuery.add(((SelectBuilder) buildVectorQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), entry.getKey())).buildString());
      }

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    Mockito.doReturn(jsonArray).when(pullContext).query(Mockito.contains(scalarsQuery.buildString()));
    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(query.buildString()));
    Mockito.doReturn(jsonArray2).when(pullContext).query(Mockito.contains(vectorQuery.get(0)));
    Mockito.doReturn(jsonArray3).when(pullContext).query(Mockito.contains(vectorQuery.get(1)));
    Mockito.doReturn(jsonArray4).when(pullContext).query(Mockito.contains(vectorQuery.get(2)));

    pullModel.pull("doubleProp", "uriProp");
    Mockito.verify(pullContext, Mockito.times(1)).query(scalarsQuery.buildString());

    assertEquals(pushModel.getDoubleProp(), pullModel.getDoubleProp());
    assertEquals(pushModel.getUriProp(), pullModel.getUriProp());
    assertNotEquals(pushModel.getStringProp(), pullModel.getStringProp());

    pullModel.pull();
    Mockito.verify(pullContext, Mockito.times(1)).query(query.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(0));
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(1));
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(2));

    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.scalarFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  @Test
  public void testPullVector() {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TestModel pushModel = Mockito.spy(TestModel.createRandom(pushContext, 12345, 3, 3));
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    TestModel pullModel = Mockito.spy(pullContext.createHollowModel(TestModel.class, pushModel.getIri()));

    JSONArray jsonArray1 = createResponseForTestPullScalars_2();
    JSONArray jsonArray2 = new JSONArray();
    JSONArray jsonArray3 = forwardvectorResponse();
    JSONArray jsonArray4 = backwardvectorResponse();

    Method buildScalarsQuery;
    SelectBuilder query = null;
    Method buildVectorQuery;
    List<String> vectorQuery = new ArrayList<>();
    try {
      buildScalarsQuery = pullContext.getClass().getDeclaredMethod("buildScalarsQuery", Node.class, MetaModel.class, String[].class);
      buildScalarsQuery.setAccessible(true);
      buildVectorQuery = pullContext.getClass().getDeclaredMethod("buildVectorQuery", Node.class, FieldKey.class);
      buildVectorQuery.setAccessible(true);

      query = (SelectBuilder) buildScalarsQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), metaModel, new String[0]);
      for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
        if (new String[0].length > 0 && !ArrayUtils.contains(new String[0], entry.getValue().field.getName())) continue;
        vectorQuery.add(((SelectBuilder) buildVectorQuery.invoke(pullContext, NodeFactory.createURI(pullModel.getIri()), entry.getKey())).buildString());
      }

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(query.buildString()));
    Mockito.doReturn(jsonArray2).when(pullContext).query(Mockito.contains(vectorQuery.get(0)));
    Mockito.doReturn(jsonArray3).when(pullContext).query(Mockito.contains(vectorQuery.get(1)));
    Mockito.doReturn(jsonArray4).when(pullContext).query(Mockito.contains(vectorQuery.get(2)));

    pullModel.pull("backwardVector", "emptyForwardVector");
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(0));
    Mockito.verify(pullContext, Mockito.never()).query(vectorQuery.get(1));
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(2));

    assertEquals(new HashSet<>(pushModel.getBackwardVector()), new HashSet<>(pullModel.getBackwardVector()));
    assertEquals(new HashSet<>(pushModel.getEmptyForwardVector()), new HashSet<>(pullModel.getEmptyForwardVector()));
    assertNotEquals(new HashSet<>(pushModel.getForwardVector()), new HashSet<>(pullModel.getForwardVector()));

    pullModel.pull();
    Mockito.verify(pullContext, Mockito.times(1)).query(query.buildString());
    Mockito.verify(pullContext, Mockito.times(2)).query(vectorQuery.get(0));
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(1));
    Mockito.verify(pullContext, Mockito.times(2)).query(vectorQuery.get(2));

    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  public JSONArray createResponsefortestLoadAllWhere_1(){
    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "a8d946b1eae26a772283da9c3c51c117").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "0.666633888087799").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "4bdf502ba0c332ab3bd4eb85babdf53a").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "932700a34b5697636535e1b43ba342d3").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "94a9593c1410d0007c88e8e1c04c5500").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "4ebafc8420489dbf3463d30d1eb55aa5").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "c954314c9c1753deeb73ab24916f7398").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "a51174640d158d6b9f15aa3323cfa1ab").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "49db66264174cf6021ed272dfd002b3a").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "486786104").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "6a9a2eaca4934b3fbe8f6dd7a6ca53b2").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "https://eg/examplenamespace/1d5fd7f8-5cb6-3dcf-88d8-edadc309dc81").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "93bb4473b77e3a4a6504cd103cf37b78").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "https://eg/examplenamespace/randomuris/-727373186").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "dd455da4f9fe3084fd9c977f11b49f9e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "89c2d0c84e62e53ffbb33a0bce8d4ecd").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "288736b0cd3e71ab313b46bb8db01b28").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "randomString-2050421886").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "1025dde2b8f71597a43f14d8edfb2bf3").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "0.9096600288931762").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "91fdbcabb9b7e6b3f3a1d39d25ef555e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "51f3c3337a5d13f7a3068d3e424ffce0").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "f838f680692397b3bde3b28b9bd967b7").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "40d1252c24c5cb1772fd63e5a4b75701").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "19ab66842a27dad68f5c317853f66e2f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "8824fede5f53230cfc6b89d4688ad22f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "65e582e1d56b2902d6341dcddcfff6c0").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "317989244").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "35fc3c33202695aa293482b297b09c8a").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "baa2e41978257e9e89a2632d78077785").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "https://eg/examplenamespace/randomuris/750723155").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "7c90fab79f30df42fa8d4eddc60ff60f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "c3eba5230307614e4db0095dbda24759").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "b3819113e4964e62c92ed85fc76adf56").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "randomString558130596").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "1025dde2b8f71597a43f14d8edfb2bf3").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "0.3475288646103122").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "91fdbcabb9b7e6b3f3a1d39d25ef555e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "51f3c3337a5d13f7a3068d3e424ffce0").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "f838f680692397b3bde3b28b9bd967b7").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "40d1252c24c5cb1772fd63e5a4b75701").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "19ab66842a27dad68f5c317853f66e2f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "8824fede5f53230cfc6b89d4688ad22f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "65e582e1d56b2902d6341dcddcfff6c0").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "1094996859").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "35fc3c33202695aa293482b297b09c8a").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "baa2e41978257e9e89a2632d78077785").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "https://eg/examplenamespace/randomuris/1905807410").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "7c90fab79f30df42fa8d4eddc60ff60f").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "c3eba5230307614e4db0095dbda24759").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "b3819113e4964e62c92ed85fc76adf56").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "randomString1525380402").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "5888c9d846489b335cac40ab1132f2fa").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "0.04699505989148167").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "71d095e54704609c0e8f5118eff199d2").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "027156f738071ed44cb379298d21489c").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "c1d174c93a75e7204a375c8f25d7bf39").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "8c2a9a3bfc1e8e02dbaf6630bf23e2e5").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "ab9f68e3468e4ee9d326c09adc6535dc").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "2a0e3a307f8bb4f4e94812e2d4ed2929").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "376d7d06f74642b0522828962c9f23ac").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "1311774767").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "c864935e37e03234d1cdad050b5f4a62").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "75e3f27904d1a1d5ab2aae2955a399d2").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "da0e7d1159050a7ffdf8f794b12aedfe").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "https://eg/examplenamespace/randomuris/-1794331249").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "ab3769efa9e87aed22461b0b74133d18").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "56ae3173539ac9a18c95c288100488a7").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "8f51f8313e98005b95123c1697782bd0").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "randomString-2069892745").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/bigintprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "16afff2244ed0484ee9a00381521764d").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("datatype","http://www.w3.org/2001/XMLSchema#double").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "0.641973939628295").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doublepropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "6bca02af4a3dfcb4ab4e84f6e19413e7").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest1a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "0bff0f1e7fd111b743c8f786b8cc5695").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph1"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "7aa344aad13e9c6e856312b896d68adb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest2b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "078220016fff464b95c465f14ffccb68").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph2"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3a").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "c8709dce33e9bda27ffe62dbce29a68b").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3b").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "d556e9c5f76a83fbfafee97ee1a90ef8").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/graphtest3c").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "38369a97ddaad1f91e7bcc719a90cfba").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/graph3"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("datatype","http://www.w3.org/2001/XMLSchema#int").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "1637003915").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "ccbee93cc2509f800f61bb261e3a8251").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "https://eg/examplenamespace/09489aa4-9574-3972-8eed-3919e4cb85ee").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "151b537d9b1e8ed8f89aee9c51709e90").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "https://eg/examplenamespace/randomuris/-169596869").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/uripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "dd9d63edf33708bfad584408eeba1d94").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#geometryprop").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "b7d102321c74a69f8e30d5a42db0993e").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringpropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "0d997bfc24dbbe0b07d8eeb3dec1c2f9").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype","http://www.w3.org/2001/XMLSchema#string").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "randomString478919291").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }

  public JSONArray createResponsefortestLoadAllWhere_2(){

    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "https://eg/examplenamespace/randomuris/874244884").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75").put("value", "063bcf549db644be09cd72cbff0c36cb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "https://eg/examplenamespace/0bbe0442-1589-3ab9-a026-19ddafc100fe").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "https://eg/examplenamespace/randomuris/-310488746").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("value", "063bcf549db644be09cd72cbff0c36cb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "https://eg/examplenamespace/4bb4eeac-5793-3eee-b6d5-d453b9f759bd").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "https://eg/examplenamespace/randomuris/1657489626").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d").put("value", "063bcf549db644be09cd72cbff0c36cb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "https://eg/examplenamespace/ca1036ea-b751-3ae9-b749-fc4f16897fc9").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "https://eg/examplenamespace/randomuris/1644384244").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1").put("value", "063bcf549db644be09cd72cbff0c36cb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/modelprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop").put("isblank", "false")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "https://eg/examplenamespace/randomuris/370504892").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuripropnull").put("isblank", "true")
                    .put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda").put("value", "063bcf549db644be09cd72cbff0c36cb").put("graph", "http://localhost:9999/blazegraph/namespace/test/sparql/testmodels"));

    return jsonArray;
  }
  @Test
  public void testLoadAllWhere() throws ParseException {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    Mockito.spy(TestModel.createRandom(pushContext, 12345, 0, 10));
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    JSONArray jsonArray = createResponsefortestLoadAllWhere_1();
    JSONArray jsonArray1 = createResponsefortestLoadAllWhere_2();

    Method buildQuery;
    SelectBuilder query1 = null;
    SelectBuilder query2 = null;
    WhereBuilder condition = new WhereBuilder().addWhere(ModelContext.getModelVar(),
            NodeFactory.createURI(SPARQLUtils.expandQualifiedName("dbpediao:intprop")),
            "?intprop"
        ).addFilter("?intprop > 0");

    try {
      buildQuery = pullContext.getClass().getDeclaredMethod("buildPullAllInDirectionQuery", Node.class, boolean.class);
      buildQuery.setAccessible(true);
      Field MODEL = pullContext.getClass().getDeclaredField("MODEL");
      MODEL.setAccessible(true);
      Node modelNode = NodeFactory.createVariable((String) MODEL.get(pullContext));
      query1 = ((SelectBuilder) buildQuery.invoke(pullContext, modelNode, false)).addWhere(condition).addVar(modelNode).addOrderBy(modelNode);
      query2 = ((SelectBuilder) buildQuery.invoke(pullContext, modelNode, true)).addWhere(condition).addVar(modelNode).addOrderBy(modelNode);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(jsonArray).when(pullContext).query(Mockito.contains(query1.buildString()));
    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(query2.buildString()));

    List<TestModel> pullModels = pullContext.pullAllWhere(TestModel.class, condition);

    Mockito.verify(pullContext, Mockito.times(1)).query(query1.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query2.buildString());

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

  @Test
  public void testRecursiveLoadAllWhere() throws ParseException {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel.createRandom(pushContext, 12345, 0, 10);
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

  public JSONArray createResponsefortestLoadPartialWhere(){

    JSONArray jsonArray = new JSONArray()
            .put(new JSONObject().put("value1", "486786104").put("isblank1", "false").put("model", "https://eg/examplenamespace/0ed64570-dc61-3703-9f4c-f8975b068b75")
                    .put("datatype1", "http://www.w3.org/2001/XMLSchema#int"))
            .put(new JSONObject().put("value1", "1094996859").put("isblank1", "false").put("model", "https://eg/examplenamespace/4771c262-0f35-32c8-8865-a04b1a6c2e5d")
                    .put("datatype1", "http://www.w3.org/2001/XMLSchema#int"))
            .put(new JSONObject().put("value1", "317989244").put("isblank1", "false").put("model", "https://eg/examplenamespace/1d680cfb-9097-3a7e-96d3-ffa7c6cf6aea")
                    .put("datatype1", "http://www.w3.org/2001/XMLSchema#int"))
            .put(new JSONObject().put("value1", "1311774767").put("isblank1", "false").put("model", "https://eg/examplenamespace/868aa231-a97d-36d8-990d-b6b1863345d1")
                    .put("datatype1", "http://www.w3.org/2001/XMLSchema#int"))
            .put(new JSONObject().put("value1", "1637003915").put("isblank1", "false").put("model", "https://eg/examplenamespace/8e92cd4c-6c42-37b4-ac42-393ef3c08cda")
                    .put("datatype1", "http://www.w3.org/2001/XMLSchema#int"));

    return jsonArray;
  }
  @Test
  public void testLoadPartialWhere() throws ParseException {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    Mockito.spy(TestModel.createRandom(pushContext, 12345, 0, 10));
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));

    JSONArray jsonArray = createResponsefortestLoadPartialWhere();
    JSONArray jsonArray1 = new JSONArray();

    Method buildScalarsQuery;
    SelectBuilder scalarsQuery = null;
    Method buildVectorQuery;
    List<String> vectorQuery = new ArrayList<>();
    WhereBuilder condition = new WhereBuilder().addWhere(
            ModelContext.getModelVar(),
            NodeFactory.createURI(SPARQLUtils.expandQualifiedName("dbpediao:intprop")),
            "?intprop"
    ).addFilter("?intprop > 0");
    String [] fieldNames = {"intProp", "backwardVector"};

    try {
      buildScalarsQuery = pullContext.getClass().getDeclaredMethod("buildScalarsQuery", Node.class, MetaModel.class, String[].class);
      buildScalarsQuery.setAccessible(true);
      buildVectorQuery = pullContext.getClass().getDeclaredMethod("buildVectorQuery", Node.class, FieldKey.class);
      buildVectorQuery.setAccessible(true);
      Field MODEL = pullContext.getClass().getDeclaredField("MODEL");
      MODEL.setAccessible(true);
      Node modelNode = NodeFactory.createVariable((String) MODEL.get(pullContext));

      scalarsQuery = ((SelectBuilder) buildScalarsQuery.invoke(pullContext, modelNode, metaModel, fieldNames)).addWhere(condition).addVar(modelNode);
      for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
        if (fieldNames.length > 0 && !ArrayUtils.contains(fieldNames, entry.getValue().field.getName())) continue;
        vectorQuery.add((((SelectBuilder) buildVectorQuery.invoke(pullContext, modelNode, entry.getKey())).addWhere(condition).addVar(modelNode).addOrderBy(modelNode)).buildString());
      }

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
      e.printStackTrace();
    }

    Mockito.doReturn(jsonArray).when(pullContext).query(Mockito.contains(scalarsQuery.buildString()));
    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(vectorQuery.get(0)));

    List<TestModel> pullModels = pullContext.pullPartialWhere(TestModel.class, condition, "intProp", "backwardVector");

    Mockito.verify(pullContext, Mockito.times(1)).query(scalarsQuery.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(0));

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

  @Test
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

  @Test
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

  @Test
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

  @Test
  public void testIRICollision() {
    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.createNewModel(TestModel.class, "test");
    try {
      context.createNewModel(TestModel.class, "test");
      fail();
    } catch (JPSRuntimeException ignored) {
    }
  }

  @Test
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

  @Test
  public void testTripleModelPushPullAll() {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TripleTestModel pushModel = pushContext.createNewModel(TripleTestModel.class, "http://testiri.com/test");
    pushModel.setStringProp("teststring");
    pushModel.getForwardVector().add(2.77);
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId));

    JSONArray jsonArray1 = new JSONArray()
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/doubleprop").put("isblank", "true")
                    .put("value", "194cb54a7c035392c7b0d6c17c932035"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/forwardvector").put("datatype", "http://www.w3.org/2001/XMLSchema#double")
                    .put("isblank", "false").put("value", "2.77"))
            .put(new JSONObject().put("predicate", "http://dbpedia.org/ontology/intprop").put("isblank", "true")
                    .put("value", "97c131c385044e482f30f7127d578dc0"))
            .put(new JSONObject().put("predicate", "http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#stringprop").put("datatype", "http://www.w3.org/2001/XMLSchema#string")
                    .put("isblank", "false").put("value", "teststring"));
    JSONArray jsonArray2 = new JSONArray();


    Method buildQuery;
    SelectBuilder query1 = null;
    SelectBuilder query2 = null;
    try {
      buildQuery = pullContext.getClass().getDeclaredMethod("buildPullAllInDirectionQuery", Node.class, boolean.class);
      buildQuery.setAccessible(true);
      query1 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), false);
      query2 = (SelectBuilder) buildQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), true);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(query1.buildString()));
    Mockito.doReturn(jsonArray2).when(pullContext).query(Mockito.contains(query2.buildString()));

    TripleTestModel pullModel = pullContext.loadAll(TripleTestModel.class, pushModel.getIri());
    Mockito.verify(pullContext, Mockito.times(1)).query(query1.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(query2.buildString());
    assertEquals(pushModel, pullModel);

  }

  @Test
  public void testTripleModelPullScalarsVectors() {

    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId));

    Mockito.doNothing().when(pushContext).update(Mockito.contains("CLEAR ALL"));
    pushContext.update("CLEAR ALL");
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("CLEAR ALL"));

    TripleTestModel pushModel = pushContext.createNewModel(TripleTestModel.class, "http://testiri.com/test");
    pushModel.setStringProp("teststring");
    pushModel.setDoubleProp(77.11);
    pushModel.getForwardVector().add(2.77);
    pushModel.getBackwardVector().add(URI.create("http://testiri.com/testuri"));
    Mockito.doNothing().when(pushContext).update(Mockito.contains("INSERT DATA"));
    pushContext.pushAllChanges();
    Mockito.verify(pushContext, Mockito.times(1)).update(Mockito.contains("INSERT DATA"));

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId));

    JSONArray jsonArray1 = new JSONArray().put(new JSONObject().put("isblank0", "false").put("value0", "teststring").put("datatype0", "http://www.w3.org/2001/XMLSchema#string"));
    JSONArray jsonArray2 = new JSONArray().put(new JSONObject().put("datatype", "http://www.w3.org/2001/XMLSchema#double").put("isblank", "false").put("value", "2.77"));

    Method buildScalarsQuery;
    SelectBuilder scalarsQuery = null;
    Method buildVectorQuery;
    List<String> vectorQuery = new ArrayList<>();
    String [] fieldNames = {"stringProp", "forwardVector"};
    try {
      buildScalarsQuery = pullContext.getClass().getDeclaredMethod("buildScalarsQuery", Node.class, MetaModel.class, String[].class);
      buildScalarsQuery.setAccessible(true);
      buildVectorQuery = pullContext.getClass().getDeclaredMethod("buildVectorQuery", Node.class, FieldKey.class);
      buildVectorQuery.setAccessible(true);

      scalarsQuery = (SelectBuilder) buildScalarsQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), metaModel, fieldNames);
      for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
        if (fieldNames.length > 0 && !ArrayUtils.contains(fieldNames, entry.getValue().field.getName())) continue;
        vectorQuery.add(((SelectBuilder) buildVectorQuery.invoke(pullContext, NodeFactory.createURI(pushModel.getIri()), entry.getKey())).buildString());
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Mockito.doReturn(jsonArray1).when(pullContext).query(Mockito.contains(scalarsQuery.buildString()));
    Mockito.doReturn(jsonArray2).when(pullContext).query(Mockito.contains(vectorQuery.get(0)));

    TripleTestModel pullModel = pullContext.loadPartial(TripleTestModel.class, pushModel.getIri(), fieldNames);

    Mockito.verify(pullContext, Mockito.times(1)).query(scalarsQuery.buildString());
    Mockito.verify(pullContext, Mockito.times(1)).query(vectorQuery.get(0));

    assertNotEquals(pushModel, pullModel);
    assertEquals(pushModel.getStringProp(), pullModel.getStringProp());
    assertNotEquals(pushModel.getDoubleProp(), pullModel.getDoubleProp());
    assertEquals(pushModel.getForwardVector(), pullModel.getForwardVector());
    assertNotEquals(pushModel.getBackwardVector(), pullModel.getBackwardVector());

  }

}
