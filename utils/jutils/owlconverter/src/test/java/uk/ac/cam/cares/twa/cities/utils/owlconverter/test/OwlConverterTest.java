package uk.ac.cam.cares.twa.cities.utils.owlconverter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.twa.cities.utils.owlconverter.OwlConverter;

public class OwlConverterTest {

  @Test
  public void testNewOwlConverter() {
    OwlConverter oc;

    try {
      oc = new OwlConverter();
      assertNotNull(oc);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewOwlConverterFields() {
    OwlConverter oc = new OwlConverter();
    assertEquals(3, oc.getClass().getDeclaredFields().length);

    Field CTYPE_NQ;
    Field ERR_INVALID_INPUT;
    Field ERR_ONT_IO;

    try {
      CTYPE_NQ = oc.getClass().getDeclaredField("CTYPE_NQ");
      CTYPE_NQ.setAccessible(true);
      assertEquals(CTYPE_NQ.get(oc), "text/x-nquads");
      ERR_INVALID_INPUT = oc.getClass().getDeclaredField("ERR_INVALID_INPUT");
      ERR_INVALID_INPUT.setAccessible(true);
      assertEquals(ERR_INVALID_INPUT.get(oc), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
      ERR_ONT_IO = oc.getClass().getDeclaredField("ERR_ONT_IO");
      ERR_ONT_IO.setAccessible(true);
      assertEquals(ERR_ONT_IO.get(oc), "Loading ontology failed.");
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewOwlConverterMethods() {
    OwlConverter oc = new OwlConverter();
    assertEquals(5, oc.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewOwlConverterValidateInputMethod() {
    OwlConverter oc = new OwlConverter();
    Method validateInput = null;
    try {
      validateInput = oc.getClass().getDeclaredMethod("validateInput", String[].class);
      validateInput.setAccessible(true);
    } catch (NoSuchMethodException e) {
      fail();
    }
    String[] input = new String[3];
    String testfile = System.getProperty("java.io.tmpdir") + "test.owl";
    String testOntoIri = "http://www.test.com/test.owl";
    String testSparqlIri = "http://www.test.com/sparql/";

    try {
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = "a";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = "c";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = null;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = null;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = null;
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = "c";
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = null;
      input[2] = null;
      input[0] = testfile;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = "c";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = testOntoIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = "a";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = testSparqlIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = "b";
      input[2] = testSparqlIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = testfile;
      input[1] = "b";
      input[2] = testSparqlIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = testfile;
      input[1] = testOntoIri;
      input[2] = testSparqlIri;
      assertTrue((Boolean) validateInput.invoke(oc, new Object[] {input}));
    } catch (InvocationTargetException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewOwlConverterLoadOntologyToStringMethod() {
    OwlConverter oc = new OwlConverter();
    Method loadOntologyToString = null;
    String testfile = System.getProperty("java.io.tmpdir") + "test.owl";
    try {
      loadOntologyToString = oc.getClass().getDeclaredMethod("loadOntologyToString", String.class);
      loadOntologyToString.setAccessible(true);
    } catch (NoSuchMethodException e) {
      fail();
    }

    try {
      loadOntologyToString.invoke(oc, testfile);
    } catch (IllegalAccessException e) {
      fail();
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Loading ontology failed.");
    }

    File ontFile = new File(testfile);
    try {
      FileUtils.writeStringToFile(ontFile, "<?xml version=\"1.0\"?>\n"
          + "    <owl:Ontology rdf:about=\"htt/broken.owl\">", Charset.defaultCharset());
    } catch (IOException e) {
      fail();
    }

    try {
      loadOntologyToString.invoke(oc, testfile);
    } catch (IllegalAccessException e) {
      fail();
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Loading ontology failed.");
    }

    try {
      FileUtils.writeStringToFile(ontFile, "test", Charset.defaultCharset());
    } catch (IOException e) {
      fail();
    }

    try {
      assertTrue(((String) loadOntologyToString.invoke(oc, testfile)).contains(
          "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Ontology> ."));
    } catch (IllegalAccessException | InvocationTargetException e) {
      fail();
    }


  }


}
