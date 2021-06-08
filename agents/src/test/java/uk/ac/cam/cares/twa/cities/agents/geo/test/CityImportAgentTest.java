package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;

public class CityImportAgentTest extends TestCase {

    public void testNewCityImportAgent() {
        CityImportAgent agent;

        try {
            agent = new CityImportAgent();
            assertNotNull(agent);
        }  catch (Exception e) {
            fail();
        }

    }

    public void testListenToImport() {
        //@Todo: implementation
    }

    public void testImportFiles() {
        //@Todo: implementation
    }

    public void testSplitFile() {
        //@Todo: implementation
    }

    public void testImportChunk() {
        //@Todo: implementation
    }

    public void testStartBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testImportToLocalBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testStopBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testWriteErrorLog() {
        //@Todo: implementation
    }

    public void testExportToNquads() {
        //@Todo: implementation
    }

    public void testChangeUrlsInNQuadsFile() {
        //@Todo: implementation
    }

    public void testUploadNQuadsFileToBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testArchiveImportFiles() {
        //@Todo: implementation
    }

    public void testValidateListenInput() {
        //@Todo: implementation
    }

    public void testValidateActionInput() {
        //@Todo: implementation
    }

}