package uk.ac.cam.cares.twa.cities;

import java.util.Arrays;

public class AccessAgentMapping {

    /**
     * Stores targetResourceId of each namespace as registered in ontokgrouter
     */
    enum AccessAgentMappingEnum {
        BERLIN("citieskg-berlin"),
        SINGAPORE_EPSG_24500("singaporeEPSG24500"),
        SINGAPORE_EPSG_4326("citieskg-singaporeEPSG4326"),
        KINGSLYNN_EPSG_3857("citieskg-kingslynnEPSG3857"),
        KINGSLYNN_EPSG_27700("citieskg-kingslynnEPSG27700"),
        PIRMASENS_EPSG_32633("citieskg-pirmasensEPSG32633"),
        JURONGISLAND_EPSG_24500("jriEPSG24500");

        private final String TARGET_RESOURCE_ID;

        AccessAgentMappingEnum(String targetResourceID) {
            this.TARGET_RESOURCE_ID = targetResourceID;
        }
    }

    /**
     * Returns targetResourceId for use with AccessAgent
     *
     * @param iriString iri of object to be queried
     * @return targetResourceId of endpoint that iri belongs to
     */
    public static String getTargetResourceID(String iriString) {
        String namespaceEndpoint = getNamespaceEndpoint(iriString);
        switch(namespaceEndpoint) {
            case "http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/": return AccessAgentMappingEnum.BERLIN.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/": return AccessAgentMappingEnum.SINGAPORE_EPSG_24500.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/": return AccessAgentMappingEnum.SINGAPORE_EPSG_4326.TARGET_RESOURCE_ID; //return "http://localhost:48888/singaporeEPSG4326";
            case "http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG3857/sparql/": return AccessAgentMappingEnum.KINGSLYNN_EPSG_3857.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG27700/sparql/": return AccessAgentMappingEnum.KINGSLYNN_EPSG_27700.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/pirmasensEPSG32633/sparql/": return AccessAgentMappingEnum.PIRMASENS_EPSG_32633.TARGET_RESOURCE_ID;
            case "http://www.theworldavatar.com:83/citieskg/namespace/jriEPSG24500/sparql/": return AccessAgentMappingEnum.JURONGISLAND_EPSG_24500.TARGET_RESOURCE_ID;
            default: return null;
        }
    }

    /**
     * Extracts endpoint from iri
     * For the additional GFA implementation, the both iris have different length, so we need to differentiate them
     * @param iriString iri of object to be queried
     * @return Endpoint that iri belongs to
     **/
    public static String getNamespaceEndpoint(String iriString) {
        String[] splitUri = iriString.split("/");
        if (splitUri[splitUri.length - 1].equals("ontozone")){
            return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 1)) + "/";
        }
        return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2)) + "/";
    }
}