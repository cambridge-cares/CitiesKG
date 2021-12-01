package uk.ac.cam.cares.twa.cities.model.geo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

/**
 * The ExternalReference class represents a java model of the ExternalReference module of CityGML.
 * It retrieves ExternalReference values and fills equivalent fields in the java model.
 * Returns an arrayList with these values
 */
public class ExternalReference {
    //Fields to be filled in
    private URI cityObjectId;
    private URI id;
    private String infoSys;
    private String name;
    private String URI;

    // Names of fields
    private static final String NAME = "name";
    private static final String URI_VAL = "URI";
    private static final String INFO_SYS = "infoSys";
    private static final String ID = "id";
    private static final String CITY_OBJECT_ID = "cityObjectId";

    /**External references graph name is now hardcoded,
     * but in the future will be retrieved by a GetGraphURI method
     * method in generic attrib that takes away unique id and adds this instead*/
    private static String EXTERNAL_REFERENCES_GRAPH_URI = "/externalreference/";

    /**Strings to be used in queries **/
    private static final String VALUE = "value";
    private static final String PREDICATE = "predicate";
    private static final String ONTO_CITY_GML = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#";
    private static final String OCGML = "ocgml";
    private static final String QM = "?";

    /**List of field names, hashmap of field names and fields**/
    private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
            (Arrays.asList(NAME, URI_VAL, INFO_SYS, ID, CITY_OBJECT_ID));
    private HashMap<String, Field> fieldMap = new HashMap<String, Field>();

    /** constructor. Adds fields to empty hashmap above.
     * Once you make a query, it returns attr values. Must identify which value goes to which field.
     * This hashmap helps with that.
     * */
    public ExternalReference() throws NoSuchFieldException {
        for (String field: FIELD_CONSTANTS){
            fieldMap.put(field, ExternalReference.class.getDeclaredField(field));
        }
    }


    /**
     * Creates a query to find the external ref values for a given CityObject
     * [In the future]: calls the getFetchExternalReferenceGraphURI method to find the correct graph
     * @param iriName IRI of the external reference instance.
     * @return QueryType object
     *e.g. Prefix ocgml:https....
     * where GRAPH extRefGraphUri{iriName ?predicate ?value}
     * select ?predicate ?value
     */
    //question: why are the ?predicate and ?value in the wherebuilder?
    // Why not just the graph uri?
    private Query getFetchExtRefScalarsQuery(String iriName){
        String extRefGraphUri = getFetchExternalReferencesGraphUri(iriName);

        WhereBuilder wb = new WhereBuilder()
                .addPrefix(OCGML, ONTO_CITY_GML) //
                .addWhere(NodeFactory.createURI(iriName), QM + PREDICATE, QM + VALUE); // I don't understand this part
        SelectBuilder sb = new SelectBuilder() //puts wherebuilder inside graph
                .addVar(QM + PREDICATE)
                .addVar(QM + VALUE)
                .addGraph(NodeFactory.createURI(extRefGraphUri), wb);
        return sb.build();
    }


    /**
     * Extracts the graph URI from the external reference IRI.
     * @param iriName IRI of the external reference instance,
     *   <http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/externalreference/UUID_03b2763f-73c6-4c46-88da-a872dd6943a7/>
     * @return graph part of the external references graph uri.
     */
    //What is the iriName? in Blazegrpah, I can only see
    // the ext ref predicates and the ext ref graph, eg
    // <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#infoSys>
    public static String getFetchExternalReferencesGraphUri(String iriName) {
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
    //Does the external reference also sometimes have the predicates Data_type and ID?
    public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient) throws IllegalAccessException {

        Query q = getFetchExtRefScalarsQuery(iriName); //form query to get ext ref scalars
        String queryResultString = kgClient.execute(q.toString());

        JSONArray queryResult = new JSONArray(queryResultString);

        //Go through ext ref scalar names, find the correct field to fill in from the field map
        if(!queryResult.isEmpty()){
            for (int index = 0; index < queryResult.length(); index++){
                JSONObject row = queryResult.getJSONObject(index);
                String predicate = row.getString(PREDICATE);
                String[] predicateArray = predicate.split("#"); // takes ending of long predicate
                predicate = predicateArray[predicateArray.length-1];


                if (predicate.equals(ID) || predicate.equals(CITY_OBJECT_ID)){
                    fieldMap.get(predicate).set(this, java.net.URI.create(row.getString(VALUE))); // set uri to field value
                }
                else {
                    fieldMap.get(predicate).set(this, row.getString(VALUE));
                }
            }
        }
    }


    /**
     * gets the value of ext ref field uriVal.
     * @return value of uriVal field
     */
    public String getURI() {
        return URI;
    }


    /**
     * gets the value of ext ref field id.
     * @return value of id field
     */
    public URI getId() {
        return id;
    }


    /**
     * gets the value of ext ref field cityObjectId.
     * @return value of cityObjectId field
     */
    public URI getCityObjectId() {
        return cityObjectId;
    }

    /**
     * gets the value of ext ref field name.
     * @return value of name field
     */
    public String getNameVal() {
        return name;
    }

    /**
     * gets the value of ext ref field infoSys.
     * @return value of infoSys field
     */
    public String getInfoSys() {
        return infoSys;
    }

}

