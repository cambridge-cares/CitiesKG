package uk.ac.cam.cares.jps.cities.db.test;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.cities.db.CitiesKGVocabulary;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CitiesKGVocabularyTest extends TestCase {

    public void testNewCitiesKGVocabulary() {
        CitiesKGVocabulary vocab = null;
        CitiesKGVocabulary vocabNamed = null;

        try {
            vocab = new CitiesKGVocabulary();
            vocabNamed = new CitiesKGVocabulary("test");
        } finally {
            assertNotNull(vocab);
            assertNotNull(vocabNamed);
        }

    }

    public void testNewCitiesKGVocabularyFields() {
        CitiesKGVocabulary vocab = new CitiesKGVocabulary();
        assertEquals(4, vocab.getClass().getDeclaredFields().length);
        try {
            assertNotNull(vocab.getClass().getDeclaredField("log"));
            assertNotNull(vocab.getClass().getDeclaredField("CFG_ERR"));
            Field log = vocab.getClass().getDeclaredField("log");
            Field err = vocab.getClass().getDeclaredField("CFG_ERR");
            log.setAccessible(true);
            err.setAccessible(true);
            assertEquals("uk.ac.cam.cares.jps.cities.db.CitiesKGVocabulary", ((Logger) log.get(vocab)).getName());
            assertEquals("config.properties", vocab.getClass().getDeclaredField("CFG_PATH").get(vocab));
            assertEquals("db.uris", vocab.getClass().getDeclaredField("CFG_KEY_URIS").get(vocab));
            assertEquals("Could not load ", err.get(vocab));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    public void testNewCitiesKGVocabularyMethods() {
        CitiesKGVocabulary vocab = new CitiesKGVocabulary();
        assertEquals(2, vocab.getClass().getDeclaredMethods().length);
    }

    public void testGetURIs() {
        CitiesKGVocabulary vocab = new CitiesKGVocabulary();

        try {
            assertNotNull(vocab.getClass().getDeclaredMethod("getURIs", String.class, String.class));
            Method getURIs = vocab.getClass().getDeclaredMethod("getURIs", String.class, String.class);
            getURIs.setAccessible(true);
            assertEquals(JSONArray.class, getURIs.invoke(vocab, "config.properties", "db.uris").getClass());

            try {
                getURIs.invoke(vocab, "non-existent", "db.uris");
            } catch (InvocationTargetException e) {
                assertEquals(e.getTargetException().getStackTrace()[2].getClassName(), "java.util.Properties");
            }

            try {
                getURIs.invoke(vocab, "config.properties", "non-existent");
            } catch (InvocationTargetException e) {
                assertEquals(e.getTargetException().getStackTrace()[2].getClassName(), "org.json.JSONArray");
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    public void testAddValues() {
        CitiesKGVocabulary vocab = new CitiesKGVocabulary();
        try {
            assertNotNull(vocab.getClass().getDeclaredMethod("addValues"));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

}
