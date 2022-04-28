package uk.ac.cam.cares.twa.cities.models.test;

import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.twa.cities.models.DatatypeModel;

import static org.junit.jupiter.api.Assertions.*;

public class DatatypeModelTest {

    @Test
    public void testNewDatatypeModelMethod() throws NoSuchMethodException {

        Class datatypeModel = DatatypeModel.class;
        assertEquals(1, datatypeModel.getDeclaredMethods().length);
        assertNotNull(datatypeModel.getDeclaredMethod("getNode"));
    }
}
