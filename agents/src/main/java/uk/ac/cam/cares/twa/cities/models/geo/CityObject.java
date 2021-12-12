package uk.ac.cam.cares.twa.cities.models.geo;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.ModelField;
import uk.ac.cam.cares.twa.cities.ModelMetadata;

/**
 * CityObject class represent a java model of CityObject module of CityGML. It retrieves CityObject
 * attributes and fills equivalent fields in the java model.
 */
@ModelMetadata(defaultGraph = SchemaManagerAdapter.CITY_OBJECT_GRAPH)
public class CityObject extends Model {

  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CREATION_DATE) protected String creationDate;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_DESCRIPTION) protected String description;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_ENVELOPE_TYPE) protected String EnvelopeType;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_GML_ID) protected String gmlId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LAST_MODIFICATION_DATE) protected String lastModificationDate;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_LINEAGE) protected String lineage;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_NAME) protected String name;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_NAME_CODESPACE) protected String nameCodespace;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID) protected int objectClassId;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_REASON_FOR_UPDATE) protected String reasonForUpdate;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_RELATIVE_TO_TERRAIN) protected String relativeToTerrain;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_RELATIVE_TO_WATER) protected String relativeToWater;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_TERMINATION_DATE) protected String terminationDate;
  @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_UPDATING_PERSON) protected String updatingPerson;

  @Getter @Setter @ModelField(
      value = SchemaManagerAdapter.ONTO_CITY_OBJECT_ID,
      graphName = SchemaManagerAdapter.GENERIC_ATTRIB_GARPH,
      innerType = GenericAttribute.class,
      backward = true)
  private ArrayList<GenericAttribute> genericAttributes;

  // @Getter @Setter @ModelField(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, graphName = "placeholder", backward = true)
  // private ArrayList<ExternalReference> externalReferences;

}
