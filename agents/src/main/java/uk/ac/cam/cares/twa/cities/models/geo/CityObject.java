package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * Model representing OntoCityGML CityObject objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(nativeGraphName = SchemaManagerAdapter.CITY_OBJECT_GRAPH)
public class CityObject extends Model {

  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_CREATION_DATE) protected String creationDate;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_DESCRIPTION) protected String description;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_ENVELOPE_TYPE) protected EnvelopeType EnvelopeType;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_GML_ID) protected String gmlId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LAST_MODIFICATION_DATE) protected String lastModificationDate;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_LINEAGE) protected String lineage;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_NAME) protected String name;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_NAME_CODESPACE) protected String nameCodespace;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID) protected Integer objectClassId;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_REASON_FOR_UPDATE) protected String reasonForUpdate;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_RELATIVE_TO_TERRAIN) protected String relativeToTerrain;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_RELATIVE_TO_WATER) protected String relativeToWater;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_TERMINATION_DATE) protected String terminationDate;
  @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_UPDATING_PERSON) protected String updatingPerson;

  @Getter @Setter @FieldAnnotation(
      value = SchemaManagerAdapter.ONTO_CITY_OBJECT_ID,
      graphName = SchemaManagerAdapter.GENERIC_ATTRIB_GARPH,
      innerType = GenericAttribute.class,
      backward = true)
  private ArrayList<GenericAttribute> genericAttributes;

  // @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, graphName = "placeholder", backward = true)
  // private ArrayList<ExternalReference> externalReferences;

  /**
   * fills in the scalar fields of a city object instance.
   */
  public void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
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
   * @param queryResult sends the query to the right endpoint.
   * @param lazyLoad if true only fills genericAttributesIris field; if false also fills genericAttributes field.
   */
  public void fillGenericAttributes(String queryResult, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    JSONArray queryResultJSON = new JSONArray(new JSONObject(queryResult).getString("result"));

    if (!queryResult.isEmpty()) {
      genericAttributes = new ArrayList<>();
      genericAttributeIris = new ArrayList<>();

      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResultJSON.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        genericAttributeIris.add(elementIri);

        if (!lazyLoad) {
          GenericAttribute genericAttribute = new GenericAttribute();
          genericAttribute.fillScalars(queryResult);
          genericAttributes.add(genericAttribute);
        }
      }
    }
  }



  /**
   * fills in external refs linked to the city object.
   * @param queryResult sends the query to the right endpoint.
   * @param lazyLoad if true only fills externalReferencesIris field; if false also fills externalReferences field.
   */
  public void fillExternalReferences(String queryResult, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    JSONArray queryResultJSON = new JSONArray(new JSONObject(queryResult).getString("result"));

    if (!queryResult.isEmpty()) {
      externalReferences = new ArrayList<>();
      externalReferencesIris = new ArrayList<>();

      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResultJSON.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        externalReferencesIris.add(elementIri);

        if (!lazyLoad) {
          ExternalReference externalReference = new ExternalReference();
          externalReference.fillScalars(queryResult);
          externalReferences.add(externalReference);
        }
      }
    }
  }
}