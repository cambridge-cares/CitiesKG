package uk.ac.cam.cares.twa.cities.utils.owlconverter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import sun.net.www.protocol.http.HttpURLConnection;
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
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = "a";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = "c";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = null;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = null;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = null;
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = "c";
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = null;
      input[2] = null;
      input[0] = testfile;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = "b";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = "c";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = testOntoIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = "a";
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[2] = testSparqlIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[1] = "b";
      input[2] = testSparqlIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
    } catch (IllegalAccessException e) {
      fail();
    }

    try {
      input[0] = testfile;
      input[1] = "b";
      input[2] = testSparqlIri;
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI>");
          
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

    if (ontFile.exists()) {
      assertTrue(ontFile.delete());
    }

  }

  @Test
  public void testNewOwlConverterProcessOntologyToNquadsMethod() {
    OwlConverter oc = new OwlConverter();
    Method processOntologyToNquads = null;
    String testOntoIri = "http://www.test.com/ontoTest.owl";
    String testSparqlIri = "http://www.test.com/sparql/";
    String nTriples = "_:blank-node1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> _:blank-node2 .\n"
        + "_:blank-node3 <http://www.lesfleursdunormal.fr/static/_downloads/owlready_ontology.owl#class_property_type> \"only\" .\n";
    String nQuads = "<http://www.test.com/ontoTest.owl#blank-node1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://www.test.com/ontoTest.owl#blank-node2> <http://www.test.com/sparql/ontoTest/> .\n"
        + "<http://www.test.com/ontoTest.owl#blank-node3> <http://www.lesfleursdunormal.fr/static/_downloads/owlready_ontology.owl#class_property_type> "
        + "\"only\" <http://www.test.com/sparql/ontoTest/> .\n";

    try {
      processOntologyToNquads = oc.getClass().getDeclaredMethod("processOntologyToNquads",
          String.class, String.class, String.class);
      processOntologyToNquads.setAccessible(true);
    } catch (NoSuchMethodException e) {
      fail();
    }

    try {
      assertEquals(processOntologyToNquads.invoke(oc, nTriples, testOntoIri, testSparqlIri), nQuads);
    } catch (IllegalAccessException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewOwlConverterUploadToEndpointMethod() {
    OwlConverter oc = new OwlConverter();
    Method uploadToEndpoint = null;
    String nQuads = "<http://www.test.com/ontoTest.owl#blank-node1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
        + "<http://www.test.com/ontoTest.owl#blank-node2> <http://www.test.com/sparql/ontoTest/> .\n"
        + "<http://www.test.com/ontoTest.owl#blank-node3> <http://www.lesfleursdunormal.fr/static/_downloads/owlready_ontology.owl#class_property_type> "
        + "\"only\" <http://www.test.com/sparql/ontoTest/> .\n";
    String testSparqlIri = "/sparql/endpoint/";

    try {
      uploadToEndpoint = oc.getClass().getDeclaredMethod("uploadToEndpoint", String.class, String.class);
      uploadToEndpoint.setAccessible(true);
    } catch (NoSuchMethodException e) {
      fail();
    }

    HttpServer httpServer = null;
    try {
      // Mock SPARQL endpoint
      httpServer = HttpServer.create(new InetSocketAddress(8000),
          0);
      httpServer.createContext(testSparqlIri, new HttpHandler() {
        public void handle(HttpExchange exchange) throws IOException {
          boolean fail = false;
          //Check if nQuads file contents is received by the mocked SPARQL endpoint
          if (exchange.getRequestHeaders().get("Content-type").get(0).equals("text/x-nquads")) {
            if (IOUtils.toString(exchange.getRequestBody(), "utf-8").equals(nQuads)) {
              byte[] response = "{\"success\": true}".getBytes();
              exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
              exchange.getResponseBody().write(response);
              exchange.close();
            } else {
              fail = true;
            }
          } else {
            fail = true;
          }
          if (fail) {
            byte[] response = "".getBytes();
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
          }
        }
      });
      httpServer.start();

      try {
        uploadToEndpoint.invoke(oc, "invalid string", "http://localhost:8000" + testSparqlIri);
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getMessage(), "http://localhost:8000/sparql/endpoint/ responded with code: 400");
      }

      uploadToEndpoint.invoke(oc, nQuads, "http://localhost:8000" + testSparqlIri);

    } catch (Exception e) {
      fail();
    }finally {
      httpServer.stop(0);
    }

  }


}
