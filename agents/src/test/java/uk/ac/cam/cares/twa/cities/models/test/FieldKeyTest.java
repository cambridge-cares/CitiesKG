package uk.ac.cam.cares.twa.cities.models.test;

import junit.framework.TestCase;
import org.apache.commons.lang.ArrayUtils;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.FieldKey;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class FieldKeyTest extends TestCase {

  public void testAnnotationConstructor() throws NoSuchFieldException {
    // Test no graph, forward
    FieldKey key = new FieldKey(
        TestModel.class.getDeclaredField("modelProp").getAnnotation(FieldAnnotation.class),
        TestModel.class.getAnnotation(ModelAnnotation.class));
    assertEquals("testmodels", key.graphName);
    assertEquals("http://dbpedia.org/ontology/modelprop", key.predicate);
    assertFalse(key.backward);
    // Test no graph, backward
    key = new FieldKey(
        TestModel.class.getDeclaredField("backUriProp").getAnnotation(FieldAnnotation.class),
        TestModel.class.getAnnotation(ModelAnnotation.class));
    assertEquals("testmodels", key.graphName);
    assertEquals("http://www.theworldavatar.com/ontology/ontoland/OntoLand.owl#backuriprop", key.predicate);
    assertTrue(key.backward);
    // Test specified graph
    key = new FieldKey(
        TestModel.class.getDeclaredField("graphTest1a").getAnnotation(FieldAnnotation.class),
        TestModel.class.getAnnotation(ModelAnnotation.class));
    assertEquals("graph1", key.graphName);
    assertEquals("http://dbpedia.org/ontology/graphtest1a", key.predicate);
    assertFalse(key.backward);
  }

  public void testEquals() {
    assertEquals(new FieldKey("g", "p", true), new FieldKey("g", "p", true));
    assertNotEquals(new FieldKey("g", "p", true), new FieldKey("h", "p", true));
    assertNotEquals(new FieldKey("g", "p", true), new FieldKey("g", "v", true));
    assertNotEquals(new FieldKey("g", "p", true), new FieldKey("g", "p", false));
  }

  public void testHashCode() {
    assertEquals(new FieldKey("g", "p", true).hashCode(),
        new FieldKey("g", "p", true).hashCode());
    assertNotEquals(new FieldKey("g", "p", true).hashCode(),
        new FieldKey("p", "g", true).hashCode());
    assertNotEquals(new FieldKey("g", "p", true).hashCode(),
        new FieldKey("g", "p", false).hashCode());
  }

  public void testCompareTo() {
    // Sort by graph, then by backward, then by predicate.
    FieldKey[] keys = {
        new FieldKey("a", "r", false),
        new FieldKey("a", "s", false),
        new FieldKey("a", "p", true),
        new FieldKey("a", "q", true),
        new FieldKey("b", "r", false),
        new FieldKey("b", "s", false),
        new FieldKey("b", "p", true),
        new FieldKey("b", "q", true),
    };
    FieldKey[] reversed = keys.clone();
    ArrayUtils.reverse(reversed);
    Object[] sorted = Arrays.stream(reversed).sorted().toArray();
    assertArrayEquals(keys, sorted);
  }

}
