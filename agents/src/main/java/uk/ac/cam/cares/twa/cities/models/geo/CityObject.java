package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.query.Query;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.twa.cities.Model;

/**
 * CityObject class represent a java model of CityObject module of CityGML.
 * It retrieves CityObject attributes and fills equivalent fields in the java model.
 */
public class CityObject extends Model {

  @Getter @Setter private String creationDate;
  @Getter @Setter private String description;
  @Getter @Setter private String EnvelopeType;
  @Getter @Setter private String gmlId;
  @Getter @Setter private URI id;
  @Getter @Setter private String lastModificationDate;
  @Getter @Setter private String lineage;
  @Getter @Setter private String name;
  @Getter @Setter private String nameCodespace;
  @Getter @Setter private int objectClassId;
  @Getter @Setter private String reasonForUpdate;
  @Getter @Setter private String relativeToTerrain;
  @Getter @Setter private String relativeToWater;
  @Getter @Setter private String terminationDate;
  @Getter @Setter private String updatingPerson;
  @Getter @Setter private ArrayList<GenericAttribute> genericAttributes;
  @Getter @Setter private ArrayList<String> genericAttributeIris;
  @Getter @Setter private ArrayList<ExternalReference> externalReferences;
  @Getter @Setter private ArrayList<String> externalReferencesIris;


  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(SchemaManagerAdapter.ONTO_CREATION_DATE, SchemaManagerAdapter.ONTO_DESCRIPTION,
          SchemaManagerAdapter.ONTO_ENVELOPE_TYPE, SchemaManagerAdapter.ONTO_GML_ID, SchemaManagerAdapter.ONTO_ID,
          SchemaManagerAdapter.ONTO_LAST_MODIFICATION_DATE, SchemaManagerAdapter.ONTO_LINEAGE,
          SchemaManagerAdapter.ONTO_NAME, SchemaManagerAdapter.ONTO_NAME_CODESPACE, SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID,
          SchemaManagerAdapter.ONTO_REASON_FOR_UPDATE, SchemaManagerAdapter.ONTO_RELATIVE_TO_TERRAIN,
          SchemaManagerAdapter.ONTO_RELATIVE_TO_WATER, SchemaManagerAdapter.ONTO_TERMINATION_DATE, SchemaManagerAdapter.ONTO_UPDATING_PERSON));

  //protected HashMap<String, Field> fieldMap = new HashMap<>();


  public CityObject() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
  }


  /**
   * fills in the scalar fields of a generic attribute instance.
   */
  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
      throws IllegalAccessException {
    if (predicate.equals(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID)){
      fieldMap.get(predicate).set(this, row.getInt(VALUE));
    }
    else if (predicate.equals(SchemaManagerAdapter.ONTO_ID)){
      fieldMap.get(predicate).set(this, URI.create(row.getString(VALUE)));
    } else {
      fieldMap.get(predicate).set(this, row.getString(VALUE));
    }
  }


  /**
   * fills in generic attributes linked to the city object.
   * @param iriName cityObject IRI
   * @param kgClient sends the query to the right endpoint.
   * @param lazyLoad if true only fills genericAttributesIris field; if false also fills genericAttributes field.
   */
  public void fillGenericAttributes(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    String[] splitUri = iriName.split("/");
    String genericAttGraphIri = String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length-2)) + "/cityobjectgenericattrib/";

    Query q = getFetchIrisQuery(iriName, SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, genericAttGraphIri);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      genericAttributes = new ArrayList<>();
      genericAttributeIris = new ArrayList<>();

      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResult.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        genericAttributeIris.add(elementIri);

        if (!lazyLoad) {
          GenericAttribute genericAttribute = new GenericAttribute();
          genericAttribute.fillScalars(elementIri, kgClient);
          genericAttributes.add(genericAttribute);
        }
      }
    }
  }


  /**
   * fills in external refs linked to the city object.
   * @param iriName cityObject IRI
   * @param kgClient sends the query to the right endpoint.
   * @param lazyLoad if true only fills externalReferencesIris field; if false also fills externalReferences field.
   */
  public void fillExternalReferences(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyLoad)
          throws NoSuchFieldException, IllegalAccessException {

    Query q = getFetchIrisQuery(iriName, SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, "placeholder"); //replaced with proper graph Iri.
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      externalReferences = new ArrayList<>();
      externalReferencesIris = new ArrayList<>();

      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResult.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        externalReferencesIris.add(elementIri);

        if (!lazyLoad) {
          ExternalReference externalReference = new ExternalReference();
          externalReference.fillScalars(elementIri, kgClient);
          externalReferences.add(externalReference);
        }
      }
    }
  }
}
