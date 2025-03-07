package org.citydb.citygml.importer.controller.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.citydb.citygml.importer.controller.Importer;
import org.citydb.citygml.importer.database.content.DBObjectTestHelper;
import org.citydb.database.adapter.blazegraph.BlazegraphConfigBuilder;
import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.junit.jupiter.api.Test;

//@Note: Testing only additions for Blazegraph
public class ImporterTest {

  @Test
  public void testNewImporter() {
    Importer importer;

    try {
      importer = new Importer(DBObjectTestHelper.getCityGMLBuilder(),
          DBObjectTestHelper.getSchemapping(), DBObjectTestHelper.getConfig(),
          DBObjectTestHelper.getObjectRegistry().getEventDispatcher());
      assertNotNull(importer);
    } catch (CityGMLBuilderException e) {
      fail();
    }
  }

  @Test
  public void testNewImporterFields() {
    //@Note: Testing only additions for Blazegraph
    try {
      Importer importer = new Importer(DBObjectTestHelper.getCityGMLBuilder(),
          DBObjectTestHelper.getSchemapping(), DBObjectTestHelper.getConfig(),
          DBObjectTestHelper.getObjectRegistry().getEventDispatcher());

      Field BLAZE_CFG_MSG = importer.getClass().getDeclaredField("BLAZE_CFG_MSG");
      Field CFG_MSG_SUCCESS = importer.getClass().getDeclaredField("CFG_MSG_SUCCESS");
      Field CFG_MSG_FAIL = importer.getClass().getDeclaredField("CFG_MSG_FAIL");
      Field blazegraphConfigBuilder = importer.getClass()
          .getDeclaredField("blazegraphConfigBuilder");
      BLAZE_CFG_MSG.setAccessible(true);
      CFG_MSG_SUCCESS.setAccessible(true);
      CFG_MSG_FAIL.setAccessible(true);
      blazegraphConfigBuilder.setAccessible(true);

      assertEquals("Writing Blazegraph configuration to ", BLAZE_CFG_MSG.get(importer));
      assertEquals(" successful.", CFG_MSG_SUCCESS.get(importer));
      assertEquals(" failed. ", CFG_MSG_FAIL.get(importer));
      assertNotNull(blazegraphConfigBuilder.get(importer));
      assertEquals(BlazegraphConfigBuilder.class, blazegraphConfigBuilder.get(importer).getClass());

    } catch (CityGMLBuilderException | NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  @Test
  public void testNewImporterMethods() {
    //@Note: Testing only additions for Blazegraph
    try {
      Importer importer = new Importer(DBObjectTestHelper.getCityGMLBuilder(),
          DBObjectTestHelper.getSchemapping(), DBObjectTestHelper.getConfig(),
          DBObjectTestHelper.getObjectRegistry().getEventDispatcher());
      assertNotNull(importer.getClass().getDeclaredMethod("writeBlazegraphConfig", String.class));
    } catch (CityGMLBuilderException | NoSuchMethodException e) {
      fail();
    }
  }

  @Test
  public void testWriteBlazegraphConfig() {
    File dir = new File("db");
    File tmp = new File(dir, "RWStore.properties");
    try {
      Importer importer = new Importer(DBObjectTestHelper.getCityGMLBuilder(),
          DBObjectTestHelper.getSchemapping(), DBObjectTestHelper.getConfig(),
          DBObjectTestHelper.getObjectRegistry().getEventDispatcher());
      Method writeBlazegraphConfig = importer.getClass().getDeclaredMethod(
          "writeBlazegraphConfig", String.class);
      writeBlazegraphConfig.setAccessible(true);
      String path = "db/RWStore.properties";
      assertEquals("Writing Blazegraph configuration to " + path + " failed. ",
          writeBlazegraphConfig.invoke(importer,path));
      dir.mkdirs();
      tmp.createNewFile();
      assertEquals("Writing Blazegraph configuration to " + path + " successful.",
          writeBlazegraphConfig.invoke(importer,path));
    } catch (CityGMLBuilderException | NoSuchMethodException | IllegalAccessException |
        InvocationTargetException | IOException e) {
      fail();
    } finally {
      if (tmp.exists()) {
        tmp.delete();
      }
      if (dir.exists()) {
        dir.delete();
      }
    }
  }

}
