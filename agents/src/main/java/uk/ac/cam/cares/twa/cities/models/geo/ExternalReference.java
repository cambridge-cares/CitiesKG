package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * ExternalReference class represent a java model of ExternalReference module of CityGML.
 * It retrieves ExternalReference values and fills equivalent fields in the java model.
 */
@ModelAnnotation(nativeGraphName = SchemaManagerAdapter.EXTERNAL_REFERENCES_GRAPH)
public class ExternalReference extends Model {

    @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_INFO_SYS) protected String infoSys;
    @Getter @Setter  @FieldAnnotation(SchemaManagerAdapter.ONTO_NAME) protected String name;
    @Getter @Setter  @FieldAnnotation(SchemaManagerAdapter.ONTO_URI) protected String URI;
    @Getter @Setter  @FieldAnnotation(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID) protected URI cityObjectId;

}