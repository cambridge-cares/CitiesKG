package uk.ac.cam.cares.twa.cities.models.osid;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;

@ModelAnnotation(defaultGraphName = "identifiers")
public class UPRN extends Model {

  @Getter @Setter
  @FieldAnnotation("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoOSID.owl#hasValue")
  private BigInteger value;

  @Getter @Setter
  @FieldAnnotation("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoOSID.owl#hasLatitudeLongitude")
  private CoordinateType latLonCoordinate; // EPSG:4326

  @Getter @Setter
  @FieldAnnotation("http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoOSID.owl#hasEastingNorthing")
  private CoordinateType eastingNorthingCoordinate; // EPSG:27700

  @Getter @Setter
  @FieldAnnotation(value = "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoOSID.owl#intersectsFeature", innerType = CityObject.class)
  private ArrayList<CityObject> intersects;

  /**
   * Constructs a UPRN in a context according to JSON data provided by an Ordnance Survey API. Does not populate
   * {@code intersects} and {@code represents} fields. The IRI of the UPRN is built from the context graph namespace.
   */
  public static UPRN loadFromJson(ModelContext context, JSONObject jsonData) {
    JSONObject properties = jsonData.getJSONObject("properties");
    String iri = context.graphNamespace + "identifiers/" + properties.getBigInteger("UPRN");
    // Check does not already exist
    UPRN existing = context.optGetModel(UPRN.class, iri);
    if(existing != null) return existing;
    // Construct new UPRN
    UPRN uprn = context.createNewModel(UPRN.class, iri);
    uprn.setValue(properties.getBigInteger("UPRN"));
    uprn.setLatLonCoordinate(new CoordinateType(
        properties.getDouble("Latitude"), properties.getDouble("Longitude"), CoordinateType.LATLON_DATATYPE));
    uprn.setEastingNorthingCoordinate(new CoordinateType(
        properties.getDouble("XCoordinate"), properties.getDouble("YCoordinate"),
        "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoOSID.owl#east-north"));
    return uprn;
  }

}
