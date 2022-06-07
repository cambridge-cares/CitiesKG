package org.citydb.database.adapter.blazegraph.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.citydb.database.adapter.blazegraph.BlazegraphConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class BlazegraphConfigTest {

  @Test
  public void testNewBlazegraphConfig() {
    Set<String> geoDataTypes;
    BlazegraphConfig cfg = null;
    try {
      geoDataTypes = new HashSet<>();
      cfg = new BlazegraphConfig(geoDataTypes);
      assertNotNull(cfg);
    } catch (Exception e){
      fail();
    }
  }

  @Test
  public void testNewBlazegraphConfigFields() {
    BlazegraphConfig cfg = new BlazegraphConfig(new HashSet<>());
    assertEquals(1, cfg.getClass().getDeclaredFields().length);
    try {
      assertNotNull(cfg.getClass().getDeclaredField("config"));
      Field config = cfg.getClass().getDeclaredField("config");
      config.setAccessible(true);
      assertEquals(Properties.class, config.get(cfg).getClass());
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  @Test
  public void testNewBlazegraphConfigMethods() {
    BlazegraphConfig cfg = new BlazegraphConfig(new HashSet<>());
    assertEquals(1, cfg.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testGetConfig() {
    BlazegraphConfig cfg = new BlazegraphConfig(new HashSet<>());
    try {
      assertNotNull(cfg.getClass().getDeclaredMethod("getConfig"));
      Method getConfig = cfg.getClass().getDeclaredMethod("getConfig");
      assertEquals(((Properties) getConfig.invoke(cfg))
                      .getProperty("com.bigdata.journal.AbstractJournal.file")
              , "citiesKG.jnl");
      assertEquals(((Properties) getConfig.invoke(cfg))
                      .getProperty("com.bigdata.journal.AbstractJournal.file"),
              "citiesKG.jnl");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.journal.AbstractJournal.bufferMode"), "DiskRW");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.service.AbstractTransactionService.minReleaseAge"), "1");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.store.AbstractTripleStore.quads"), "true");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.store.AbstractTripleStore.statementIdentifiers"), "false");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.sail.truthMaintenance"), "false");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.store.AbstractTripleStore.textIndex"), "false");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
                      "com.bigdata.rdf.store.AbstractTripleStore.axiomsClass"),
              "com.bigdata.rdf.axioms.NoAxioms");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
                      "com.bigdata.rdf.store.AbstractTripleStore.vocabularyClass"),
              "uk.ac.cam.cares.jps.cities.db.CitiesKGVocabulary");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.btree.writeRetentionQueue.capacity"), "4000");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.btree.BTree.branchingFactor"), "128");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.journal.AbstractJournal.initialExtent"), "209715200");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.journal.AbstractJournal.maximumExtent"), "2097152000");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.namespace.wdq.lex.com.bigdata.btree.BTree.branchingFactor"), "800");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.namespace.wdq.lex.ID2TERM.com.bigdata.btree.BTree.branchingFactor"), "1600");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.namespace.wdq.lex.TERM2ID.com.bigdata.btree.BTree.branchingFactor"), "256");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.namespace.wdq.spo.com.bigdata.btree.BTree.branchingFactor"), "2048");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.namespace.wdq.spo.OSP.com.bigdata.btree.BTree.branchingFactor"), "128");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.namespace.wdq.spo.SPO.com.bigdata.btree.BTree.branchingFactor"), "1200");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.sail.bufferCapacity"), "10000000");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.journal.AbstractJournal.writeCacheBufferCount"), "3000");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rwstore.RWStore.smallSlotType"), "1024");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.journal.AbstractJournal.historicalIndexCacheCapacity"), "2000");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.journal.AbstractJournal.historicalIndexCacheTimeout"), "5000");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.store.AbstractTripleStore.geoSpatial"), "true");
      assertEquals(((Properties) getConfig.invoke(cfg)).getProperty(
              "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialIncludeBuiltinDatatypes"), "true");
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    }
  }

}
