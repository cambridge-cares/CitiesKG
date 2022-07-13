package uk.ac.cam.cares.twa.cities.model.geo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesTest {
    public String FS = System.getProperty("file.separator");
    public File testDir = new File(System.getProperty("java.io.tmpdir") + "utilitiestest");
    public File testFile = new File(this.testDir.getAbsolutePath() + this.FS + "testFile.csv");


    public void setUp() {
        // set up a directory and file for testGetInputFiles and testGetInputDir
        try {
            this.testDir.mkdirs();
            File testFileResource = new File(Objects.requireNonNull(this.getClass().getResource("/summary_1.csv")).getFile());
            Files.copy(testFileResource.toPath(), this.testFile.toPath());
        } catch (IOException e) {
            fail();
        }
    }

    @AfterEach
    public void tearDown() {
        // delete dir and files created in setUp + dir created in testCreateDir
        if (Objects.requireNonNull(this.testFile.exists())) {
            if (!this.testFile.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.testDir.exists())) {
            if (!this.testDir.delete()) {
                fail();
            }
        }
    }

    @Test
    public void testUtilities() {
        try {
            Utilities utilities = new Utilities("testpath");
            assertNotNull(utilities);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewUtilitiesFields() {
        // this test is deliberately left blank
        // fields tested in testGetInputFiles and testGetInputDir
    }

    @Test
    public void testNewUtilitiesMethods() {
        try {
            Utilities utilities = new Utilities("testpath");
            assertEquals(7, utilities.getClass().getDeclaredMethods().length);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetInputFiles() {
        setUp();

        // test case where input is a file
        String[] result1 = Utilities.getInputFiles(this.testFile.getAbsolutePath());
        assertEquals(1, result1.length);
        assertEquals(this.testFile.getAbsolutePath(), result1[0]);

        // test case where input is a directory
        String[] result2 = Utilities.getInputFiles(this.testDir.getAbsolutePath());
        assertEquals(1, result2.length);
        assertEquals(this.testFile.getAbsolutePath(), result2[0]);
    }

    @Test
    public void testGetInputDir() {
        setUp();

        // test case where input is a file
        assertEquals(this.testDir.getAbsolutePath(), Utilities.getInputDir(this.testFile.getAbsolutePath()));

        // test case where input is a directory
        assertEquals(this.testDir.getAbsolutePath(), Utilities.getInputDir(this.testDir.getAbsolutePath()));
    }

    @Test
    public void testConvertList2Map() {
        String[] arr = {"BLDG_123a", "0.0#1.0#2.0#3.0", "1.0#1.0", "testoutput.kml"};
        List<String[]> list = new ArrayList<>(Collections.singleton(arr));

        HashMap<String, String[]> map = Utilities.convertList2Map(list);

        assertTrue(map.containsKey("BLDG_123a"));
        String[] result = map.get("BLDG_123a");
        assertEquals("BLDG_123a", result[0]);
        assertEquals("0.0#1.0#2.0#3.0", result[1]);
        assertEquals("1.0#1.0", result[2]);
        assertEquals("testoutput.kml", result[3]);
    }

    @Test
    public void testStr2int() {
        int[] result = Utilities.str2int("1#5");
        assertEquals(1, result[0]);
        assertEquals(5, result[1]);
    }

    @Test
    public void testArr2str() {
        String[] arr = {"test", "BLDG_123a", "1.0#5.0"};
        assertEquals("test#BLDG_123a#1.0#5.0", Utilities.arr2str(arr));
    }

    @Test
    public void testCreateDir() {
        assertFalse(this.testDir.exists());
        assertEquals(this.testDir.getAbsolutePath(), Utilities.createDir(this.testDir.getAbsolutePath()));
        assertTrue(this.testDir.exists());
    }

    @Test
    public void testIsUnique() {
        // test case when not unique
        List<String> list1 = Arrays.asList("test", "gmlId", "123", "test");
        assertFalse(Utilities.isUnique(list1));

        // test case when unique
        List<String> list2 = Arrays.asList("test", "gmlId", "123", "testoutput.kml");
        assertTrue(Utilities.isUnique(list2));
    }
}
