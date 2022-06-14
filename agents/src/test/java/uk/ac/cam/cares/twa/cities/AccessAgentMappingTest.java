package uk.ac.cam.cares.twa.cities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessAgentMappingTest {

    @Test
    void getTargetResourceID() {
        assertEquals("citieskg-berlin",
                AccessAgentMapping.getTargetResourceID("http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/cityobject/UUID_123/"));
        assertEquals("singaporeEPSG24500",
                AccessAgentMapping.getTargetResourceID("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/cityobject/UUID_123/"));
        assertEquals("citieskg-singaporeEPSG4326",
                AccessAgentMapping.getTargetResourceID("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_123/"));
        assertNull(AccessAgentMapping.getTargetResourceID("http://www.theworldavatar.com:83/citieskg/namespace/test/sparql/cityobject/UUID_123/"));
    }

    @Test
    public void testGetNamespaceEndpoint() {
        String iriString1 = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        assertEquals("http://localhost:9999/blazegraph/namespace/berlin/sparql/", AccessAgentMapping.getNamespaceEndpoint(iriString1));

        String iriString2 = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3";
        assertEquals("http://localhost:9999/blazegraph/namespace/berlin/sparql/", AccessAgentMapping.getNamespaceEndpoint(iriString2));
    }
}