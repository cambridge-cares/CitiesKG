package org.citydb.database.adapter.blazegraph.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.citydb.database.adapter.blazegraph.BlazegraphAdapter;
import org.citydb.database.adapter.blazegraph.BlazegraphConfigBuilder;
import org.citydb.database.adapter.blazegraph.BlazegraphGeoDatatype;
import org.junit.jupiter.api.Test;

public class BlazegraphConfigBuilderTest extends TestCase {

  private final String TEST_URI = "http://localhost/blazegraph/literals/POLYGON-1-3";
  private final String TEST_GEODATATYPE = "{\"config\":{\"fields\":["
      + "{\"serviceMapping\":\"X0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"},"
      + "{\"serviceMapping\":\"Y0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"},"
      + "{\"serviceMapping\":\"Z0\",\"multiplier\":\"100000\",\"valueType\":\"DOUBLE\"}],"
      + "\"uri\":" + TEST_URI + "}}";

  @Test
  public void testNewBlazegraphConfigBuilder() {

    BlazegraphConfigBuilder cfgb = null;
    try {
      cfgb = BlazegraphConfigBuilder.getInstance();
    } finally {
      assertNotNull(cfgb);
    }
  }

  @Test
  public void testNewBlazegraphConfigBuilderFields() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();
    assertEquals(5, cfgb.getClass().getDeclaredFields().length);

    try {
      assertNotNull(cfgb.getClass().getDeclaredField("log"));
      assertNotNull(cfgb.getClass().getDeclaredField("instance"));
      assertNotNull(cfgb.getClass().getDeclaredField("CFG_ERR"));
      assertNotNull(cfgb.getClass().getDeclaredField("geoDataTypes"));
      assertNotNull(cfgb.getClass().getDeclaredField("uriStrings"));

      Field log = cfgb.getClass().getDeclaredField("log");
      Field instance = cfgb.getClass().getDeclaredField("instance");
      Field CFG_ERR = cfgb.getClass().getDeclaredField("CFG_ERR");
      Field geoDataTypes = cfgb.getClass().getDeclaredField("geoDataTypes");
      Field uriStrings = cfgb.getClass().getDeclaredField("uriStrings");

      log.setAccessible(true);
      instance.setAccessible(true);
      CFG_ERR.setAccessible(true);
      geoDataTypes.setAccessible(true);
      uriStrings.setAccessible(true);

      assertEquals("org.citydb.database.adapter.blazegraph.BlazegraphConfigBuilder",
          ((Logger) log.get(cfgb)).getName());
      assertEquals(BlazegraphConfigBuilder.class, instance.get(cfgb).getClass());
      assertEquals("Could not load ", CFG_ERR.get(cfgb));
      assertEquals(HashSet.class, geoDataTypes.get(cfgb).getClass());
      assertEquals(HashSet.class, uriStrings.get(cfgb).getClass());

    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewBlazegraphConfigBuilderMethods() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();
    assertEquals(8, cfgb.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testAddGeoDataType() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method addGeoDataType = cfgb.getClass().getDeclaredMethod("addGeoDataType", String.class);
      Field geoDataTypes = cfgb.getClass().getDeclaredField("geoDataTypes");
      geoDataTypes.setAccessible(true);
      assertEquals(0, ((Set<String>) geoDataTypes.get(cfgb)).size());
      addGeoDataType.invoke(cfgb, TEST_GEODATATYPE);
      assertEquals(1, ((Set<String>) geoDataTypes.get(cfgb)).size());
      assertEquals(TEST_GEODATATYPE, ((Set<String>) geoDataTypes.get(cfgb)).iterator().next());
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testAddURIString() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method addURIString = cfgb.getClass().getDeclaredMethod("addURIString", String.class);
      Field uriStrings = cfgb.getClass().getDeclaredField("uriStrings");
      uriStrings.setAccessible(true);
      assertEquals(0, ((Set<String>) uriStrings.get(cfgb)).size());
      addURIString.invoke(cfgb, TEST_URI);
      assertEquals(1, ((Set<String>) uriStrings.get(cfgb)).size());
      assertEquals(TEST_URI, ((Set<String>) uriStrings.get(cfgb)).iterator().next());
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testBuildVocabulariesConfig() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method buildVocabulariesConfig = cfgb.getClass().getDeclaredMethod("buildVocabulariesConfig");
      Method addURIString = cfgb.getClass().getDeclaredMethod("addURIString", String.class);
      addURIString.invoke(cfgb, TEST_URI);
      Properties cfg = (Properties) buildVocabulariesConfig.invoke(cfgb);
      assertTrue(cfg.containsKey(BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_KEY_URIS));
      assertEquals(cfg.getProperty(BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_KEY_URIS),
          "[\"" + TEST_URI + "\"]");
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testBuild() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method build = cfgb.getClass().getDeclaredMethod("build");
      Method addGeoDataType = cfgb.getClass().getDeclaredMethod("addGeoDataType", String.class);
      addGeoDataType.invoke(cfgb, TEST_GEODATATYPE);
      Properties cfg = (Properties) build.invoke(cfgb);
      assertTrue(cfg.containsKey(BlazegraphGeoDatatype.KEY_MAIN + 0));
      assertEquals(cfg.getProperty(BlazegraphGeoDatatype.KEY_MAIN + 0), TEST_GEODATATYPE);
      assertEquals(cfg.getProperty("com.bigdata.journal.AbstractJournal.file")
          , "citiesKG.jnl");
      assertEquals(cfg.getProperty("com.bigdata.journal.AbstractJournal.file"),
          "citiesKG.jnl");
      assertEquals(cfg.getProperty("com.bigdata.journal.AbstractJournal.bufferMode"),
          "DiskRW");
      assertEquals(cfg.getProperty("com.bigdata.service.AbstractTransactionService.minReleaseAge"),
          "1");
      assertEquals(cfg.getProperty("com.bigdata.rdf.store.AbstractTripleStore.quads"),
          "true");
      assertEquals(
          cfg.getProperty("com.bigdata.rdf.store.AbstractTripleStore.statementIdentifiers"),
          "false");
      assertEquals(cfg.getProperty("com.bigdata.rdf.sail.truthMaintenance"), "false");
      assertEquals(cfg.getProperty("com.bigdata.rdf.store.AbstractTripleStore.textIndex"),
          "false");
      assertEquals(cfg.getProperty("com.bigdata.rdf.store.AbstractTripleStore.axiomsClass"),
          "com.bigdata.rdf.axioms.NoAxioms");
      assertEquals(cfg.getProperty("com.bigdata.rdf.store.AbstractTripleStore.vocabularyClass"),
          "uk.ac.cam.cares.jps.cities.db.CitiesKGVocabulary");
      assertEquals(cfg.getProperty("com.bigdata.btree.writeRetentionQueue.capacity"), "4000");
      assertEquals(cfg.getProperty("com.bigdata.btree.BTree.branchingFactor"), "128");
      assertEquals(cfg.getProperty("com.bigdata.journal.AbstractJournal.initialExtent"),
          "209715200");
      assertEquals(cfg.getProperty("com.bigdata.journal.AbstractJournal.maximumExtent"),
          "2097152000");
      assertEquals(
          cfg.getProperty("com.bigdata.namespace.wdq.lex.com.bigdata.btree.BTree.branchingFactor"),
          "800");
      assertEquals(cfg.getProperty(
          "com.bigdata.namespace.wdq.lex.ID2TERM.com.bigdata.btree.BTree.branchingFactor"), "1600");
      assertEquals(cfg.getProperty(
          "com.bigdata.namespace.wdq.lex.TERM2ID.com.bigdata.btree.BTree.branchingFactor"), "256");
      assertEquals(
          cfg.getProperty("com.bigdata.namespace.wdq.spo.com.bigdata.btree.BTree.branchingFactor"),
          "2048");
      assertEquals(cfg.getProperty(
          "com.bigdata.namespace.wdq.spo.OSP.com.bigdata.btree.BTree.branchingFactor"), "128");
      assertEquals(cfg.getProperty(
          "com.bigdata.namespace.wdq.spo.SPO.com.bigdata.btree.BTree.branchingFactor"), "1200");
      assertEquals(cfg.getProperty("com.bigdata.rdf.sail.bufferCapacity"), "10000000");
      assertEquals(cfg.getProperty("com.bigdata.journal.AbstractJournal.writeCacheBufferCount"),
          "3000");
      assertEquals(cfg.getProperty("com.bigdata.rwstore.RWStore.smallSlotType"), "1024");
      assertEquals(
          cfg.getProperty("com.bigdata.journal.AbstractJournal.historicalIndexCacheCapacity"),
          "2000");
      assertEquals(
          cfg.getProperty("com.bigdata.journal.AbstractJournal.historicalIndexCacheTimeout"),
          "5000");
      assertEquals(cfg.getProperty("com.bigdata.rdf.store.AbstractTripleStore.geoSpatial"), "true");
      assertEquals(cfg.getProperty(
          "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialIncludeBuiltinDatatypes"), "true");
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testLoadURIs() throws NoSuchFieldException, IllegalAccessException {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method loadURIs = cfgb.getClass().getDeclaredMethod("loadURIs", String.class);
      loadURIs.setAccessible(true);
      Field uriStrings = cfgb.getClass().getDeclaredField("uriStrings");
      uriStrings.setAccessible(true);

      assertEquals(0, ((Set<String>) uriStrings.get(cfgb)).size());
      loadURIs.invoke(cfgb, BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_PATH);
      assertTrue(((Set<String>) uriStrings.get(cfgb)).size() > 0);
    } catch (NoSuchMethodException | InvocationTargetException | NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
    //@// TODO: 25/01/2021 still failing - make it work
  }

  @Test
  public void testLoadDatatypes() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method loadURIs = cfgb.getClass().getDeclaredMethod("loadDatatypes", String.class);
      loadURIs.setAccessible(true);
      Field geoDataTypes = cfgb.getClass().getDeclaredField("geoDataTypes");
      geoDataTypes.setAccessible(true);

      assertEquals(0, ((Set<String>) geoDataTypes.get(cfgb)).size());
      loadURIs.invoke(cfgb, BlazegraphAdapter.BLAZEGRAPH_CFG_PATH);
      assertTrue(((Set<String>) geoDataTypes.get(cfgb)).size() > 0);
    } catch (NoSuchMethodException | InvocationTargetException | NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
    //@// TODO: 25/01/2021 still failing - make it work
  }

  @Test
  public void testLoadProperties() {
    BlazegraphConfigBuilder cfgb = BlazegraphConfigBuilder.getInstance();

    try {
      Method loadProperties = cfgb.getClass().getDeclaredMethod("loadProperties", String.class);
      loadProperties.setAccessible(true);
      Properties prop = (Properties) loadProperties.invoke(BlazegraphAdapter.BLAZEGRAPH_CFG_PATH);
      assertNotNull(prop);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      fail();
    }
    //@// TODO: 25/01/2021 still failing - make it work
  }
  
}
