package uk.ac.cam.cares.twa.cities.models.geo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
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

    private URI cityObjectId;
    private URI id;
    private String infoSys;
    private String name;
    private String URI;

    private static final String CITY_OBJECT_ID = "cityObjectId";
    private static final String ID = "id";
    private static final String INFO_SYS = "infoSys";
    private static final String NAME = "name";
    private static final String URI_VAL = "URI";

    private String ONTO_CITY_GML;
    private static final String EXTERNAL_REFERENCES_GRAPH_URI = "/externalreference/";
    private static final String VALUE = "value";
    private static final String PREDICATE = "predicate";
    private static final String OCGML = "ocgml";
    private static final String QM = "?";

    private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
            (Arrays.asList(NAME, URI_VAL, INFO_SYS, ID, CITY_OBJECT_ID));

    private HashMap<String, Field> fieldMap = new HashMap<>();


    public ExternalReference() throws NoSuchFieldException {
        for (String field: FIELD_CONSTANTS){
            fieldMap.put(field, ExternalReference.class.getDeclaredField(field));
        }
        readConfig();
    }


    /**
     * reads variable values relevant for GenericAttribute class from config.properties file.
     */
    private void readConfig() {
        ResourceBundle config = ResourceBundle.getBundle("config");
        ONTO_CITY_GML = config.getString("uri.ontology.ontocitygml");
    }


    /**
     * builds a query to get the external ref instance scalars.
     * @param iriName IRI of the external reference instance.
     * @return Query object for retrieving all Externalreference attribute values.
     */
    private Query getFetchExtRefScalarsQuery(String iriName){
        String extRefGraphUri = getFetchExternalReferencesGraphUri(iriName);

        WhereBuilder wb = new WhereBuilder()
                .addPrefix(OCGML, ONTO_CITY_GML) //
                .addWhere(NodeFactory.createURI(iriName), QM + PREDICATE, QM + VALUE);
        SelectBuilder sb = new SelectBuilder()
                .addVar(QM + PREDICATE)
                .addVar(QM + VALUE)
                .addGraph(NodeFactory.createURI(extRefGraphUri), wb);
        return sb.build();
    }


    /**
     * Extracts the graph URI from the external reference IRI.
     * @param iriName IRI of the external reference instance
     * @return graph part of the external references graph uri.
     */
    public static String getFetchExternalReferencesGraphUri(String iriName) {
        String[] splitUri = iriName.split("/");
        String namespace = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2));
        return namespace + EXTERNAL_REFERENCES_GRAPH_URI;
    }


    /**
     * fills in the scalar fields of an external reference instance.
     * @param iriName IRI of the external reference instance.
     * @param kgClient sends the query to the right endpoint.
     */
    public void fillScalars(String iriName, KnowledgeBaseClientInterface kgClient) throws IllegalAccessException {

        Query q = getFetchExtRefScalarsQuery(iriName);
        String queryResultString = kgClient.execute(q.toString());
        JSONArray queryResult = new JSONArray(queryResultString);

        if(!queryResult.isEmpty()){
            for (int index = 0; index < queryResult.length(); index++){
                JSONObject row = queryResult.getJSONObject(index);
                String predicate = row.getString(PREDICATE);
                String[] predicateArray = predicate.split("#");
                predicate = predicateArray[predicateArray.length-1];

                if (predicate.equals(ID) || predicate.equals(CITY_OBJECT_ID)){
                    fieldMap.get(predicate).set(this, java.net.URI.create(row.getString(VALUE)));
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

