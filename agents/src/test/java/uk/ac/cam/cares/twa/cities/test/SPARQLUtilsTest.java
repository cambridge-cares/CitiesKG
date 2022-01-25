package uk.ac.cam.cares.twa.cities.test;

import junit.framework.TestCase;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;

public class SPARQLUtilsTest extends TestCase {

  public void testAddPrefix() {
    SelectBuilder builder = new SelectBuilder();
    SPARQLUtils.addPrefix("ocgml:test", builder);
    SPARQLUtils.addPrefix("xsd", builder);
    String builtString = builder.buildString();
    System.out.println(builtString);
    assertTrue(builtString.contains("PREFIX  xsd"));
    assertTrue(builtString.contains("PREFIX  ocgml"));
  }

  public void testExpandQualifiedName() {
    assertEquals("http://dbpedia.org/ontology/testname", SPARQLUtils.expandQualifiedName("dbpediao:testname"));
    assertEquals("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#id", SPARQLUtils.expandQualifiedName("ocgml:id"));
    assertEquals("nonexistentprefix:testname", SPARQLUtils.expandQualifiedName("nonexistentprefix:testname"));
    assertEquals("noprefixinexpression", SPARQLUtils.expandQualifiedName("noprefixinexpression"));
    assertEquals("http://dbpedia.org/ontology/testname", SPARQLUtils.expandQualifiedName("http://dbpedia.org/ontology/testname"));
  }

  public void testGetPrefixUrl() {
    assertEquals("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#", SPARQLUtils.getPrefixUrl("ocgml"));
    assertEquals("http://dbpedia.org/ontology/", SPARQLUtils.getPrefixUrl("dbpediao"));
  }

  public void testUnpackQueryResponse () {
    JSONArray unpacked = new JSONArray();
    JSONObject row = new JSONObject();
    row.put("column1", "value1");
    unpacked.put(row);
    assertEquals(unpacked.toString(), SPARQLUtils.unpackQueryResponse("{ \"result\": \"[{'column1': 'value1'}]\" }").toString());
  }

}
