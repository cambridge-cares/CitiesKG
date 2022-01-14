package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import java.util.Arrays;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;

/**
 * ExternalReference class represent a java model of ExternalReference module of CityGML.
 * It retrieves ExternalReference values and fills equivalent fields in the java model.
 * Returns an arrayList with these values
 */
public class ExternalReference {
    private String iriName = "iriName";
    private URI cityObjectId;
    private URI id;
    private String infoSys;
    private String name;
    private String uriVal;

    /**
     * External references graph name is now hardcoded,
     * but in the future will be retrieved by a GetGraphURI method
     */
    private String EXTERNAL_REFERENCES_GRAPH_URI = "/cityobjectgenericattrib/";

    private static final String EXT_REF_NAME = "extRefName";
    private static final String URI_VAL = "uriVal";
    private static final String INFO_SYS = "infoSys";
    private static final String ID = "id";
    private static final String CITY_OBJECT_ID = "cityObjectId";

    /**
     * constructs an empty external reference instance and fills in the external reference IRI field.
     * @param iriName
     * @return - nothing - just fills in the field
     */
    public void ExternalReference(String iriName) {
        this.iriName = iriName;
    }

    /**
     * Creates a query to find the external ref values for a given CityObject
     * [In the future]: calls the getFetchExternalReferenceGraphURI method to find the correct graph
     * @param iriName IRI of the external reference instance.
     * @return QueryType object
     */

    private Query getFetchExtRefScalarsQuery(String iriName){
        String graphUri = EXTERNAL_REFERENCES_GRAPH_URI;

        //placeholder to not throw an error but query building needs to be written here
        Query query = new Query();
        return query;
    }


    /**
     * Extracts the graph URI from the external reference IRI.
     * @param iriName IRI of the external reference instance.
     * @return uri of the external references graph.
     */
    public String getFetchExternalReferencesGraphUri(String iriName) {
        String[] splitUri = iriName.split("/");
        String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
        return namespace + EXTERNAL_REFERENCES_GRAPH_URI;
    }


    /**
     * fills in the scalar fields of an external reference instance.
     * @param iriName IRI of the external reference instance.
     * @param kgClient sends the query to the right endpoint.
     * @return nothing - just fills in fields
     */
    public void fillScalars(String iriName, StoreClientInterface kgClient) {

        Query q = getFetchExtRefScalarsQuery(iriName);
        String queryResultString = kgClient.execute(q.toString());

        JSONArray queryResult = new JSONArray(queryResultString);

        uriVal = queryResult.getJSONObject(0).getString(URI_VAL);
        name = queryResult.getJSONObject(0).getString(EXT_REF_NAME);
        infoSys = queryResult.getJSONObject(0).getString(INFO_SYS);
        id = URI.create(queryResult.getJSONObject(0).getString(ID));
        cityObjectId = URI.create(queryResult.getJSONObject(0).getString(CITY_OBJECT_ID));

    }


}