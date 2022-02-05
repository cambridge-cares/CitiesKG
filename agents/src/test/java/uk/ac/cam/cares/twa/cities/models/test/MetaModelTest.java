package uk.ac.cam.cares.twa.cities.models.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.FieldKey;
import uk.ac.cam.cares.twa.cities.models.MetaModel;

import java.io.InvalidClassException;

public class MetaModelTest extends TestCase {

  public void testConstructor() throws InvalidClassException, NoSuchMethodException {
    MetaModel metaModel = MetaModel.get(TestModel.class);
    // test default graph, forward
    assertTrue(metaModel.fieldMap.containsKey(
        new FieldKey("testmodels", SPARQLUtils.expandQualifiedName("JPSLAND:stringpropnull"), false)));
    // test default graph, backward
    assertTrue(metaModel.fieldMap.containsKey(
        new FieldKey("testmodels", SPARQLUtils.expandQualifiedName("JPSLAND:backuriprop"), true)));
    // test specified graph
    assertTrue(metaModel.fieldMap.containsKey(
        new FieldKey("graph3", SPARQLUtils.expandQualifiedName("dbpediao:graphtest3a"), false)));
    // test number of fields loaded is correct
    assertEquals(20, metaModel.scalarFieldList.size());
    assertEquals(3, metaModel.vectorFieldList.size());
    assertEquals(23, metaModel.fieldMap.size());
  }

}
