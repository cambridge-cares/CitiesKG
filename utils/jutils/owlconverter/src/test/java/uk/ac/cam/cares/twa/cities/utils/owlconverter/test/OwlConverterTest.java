package uk.ac.cam.cares.twa.cities.utils.owlconverter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    try {
      Method validateInput = oc.getClass().getDeclaredMethod("validateInput", String[].class);
      validateInput.setAccessible(true);
      String[] input = new String[3];
      validateInput.invoke(oc, new Object[] {input});
    } catch (InvocationTargetException e) {
      assertEquals(e.getTargetException().getMessage(), "Invalid arguments supplied: "
          + "{Mandatory CLI arguments: <OWL file path> <ontology IRI> <SPARQL endpoint IRI> }");
    } catch (NoSuchMethodException | IllegalAccessException e) {
      fail();
    }
  }


}
