package uk.ac.cam.cares.twa.cities.models.geo;

import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.Model;

import java.net.URI;

public class OntoCityGMLModel extends Model {

  private String ID_MISMATCH_ERROR_TEXT = "'id' property of OntoCityGML object not the same as its IRI.";

  @FieldAnnotation(SchemaManagerAdapter.ONTO_ID) protected URI id;

  public void setId(URI value) {
    // Permit setting to null so clearAll doesn't throw an error.
    if(value != null && !value.toString().equals(getIri())) {
      throw new JPSRuntimeException(ID_MISMATCH_ERROR_TEXT);
    }
  }

  public URI getId() {
    return URI.create(getIri());
  }

}
