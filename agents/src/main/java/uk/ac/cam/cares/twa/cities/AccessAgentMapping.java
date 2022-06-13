package uk.ac.cam.cares.twa.cities;

public class AccessAgentMapping {

    enum AccessAgentMappingEnum {
        BERLIN("citieskg-berlin"),
        SINGAPORE_EPSG_24500("singaporeEPSG24500"),
        SINGAPORE_EPSG_4326("citieskg-singaporeEPSG4326");

        private final String TARGET_RESOURCE_ID;

        AccessAgentMappingEnum(String targetResourceID) {
            this.TARGET_RESOURCE_ID = targetResourceID;
        }
    }

    public static String getTargetResourceID(String endpoint) {
        switch(endpoint) {
            case "http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/": return AccessAgentMappingEnum.BERLIN.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/": return AccessAgentMappingEnum.SINGAPORE_EPSG_24500.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/": return AccessAgentMappingEnum.SINGAPORE_EPSG_4326.TARGET_RESOURCE_ID;
            default: return null;
        }
    }
}