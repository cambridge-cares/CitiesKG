package uk.ac.cam.cares.twa.cities.models.geo;

import java.net.URI;


import lombok.Getter;
import lombok.Setter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import uk.ac.cam.cares.twa.cities.models.FieldAnnotation;
import uk.ac.cam.cares.twa.cities.models.ModelAnnotation;

/**
 * Model representing OntoCityGML ExternalReference objects.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
@ModelAnnotation(nativeGraphName = SchemaManagerAdapter.EXTERNAL_REFERENCES_GRAPH)
public class ExternalReference {

    @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID) private Integer cityObjectId;
    @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_INFO_SYS) private String infoSys;
    @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_NAME)  private String name;
    @Getter @Setter @FieldAnnotation(SchemaManagerAdapter.ONTO_URI) private URI uri;

}