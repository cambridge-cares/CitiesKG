package uk.ac.cam.cares.jps.cities.db.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.jps.cities.db.CitiesKGVocabulary;

import java.lang.reflect.Field;

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
        assertEquals(3, vocab.getClass().getDeclaredFields().length);
        try {
            assertTrue(vocab.getClass().getDeclaredField("CFG_PATH").get(vocab).equals("config.properties"));
            assertTrue(vocab.getClass().getDeclaredField("CFG_KEY_URIS").get(vocab).equals("db.uris"));
            assertNotNull(vocab.getClass().getDeclaredField("CFG_ERR"));
            Field err = vocab.getClass().getDeclaredField("CFG_ERR");
            err.setAccessible(true);
            assertTrue(err.get(vocab).equals("Could not load "));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

}
