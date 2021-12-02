package uk.ac.cam.cares.twa.cities.models.geo;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import org.apache.jena.query.Query;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.twa.cities.Model;

public class Surface extends Model {
  @Getter @Setter
  private ArrayList<SurfaceGeometry> surfaceGeometries;
  @Getter @Setter
  private ArrayList<String> surfaceGeometriesIris;

  /**
   * fills in generic attributes linked to the city object.
   * @param iriName cityObject IRI
   * @param kgClient sends the query to the right endpoint.
   * @param lazyLoad if true only fills genericAttributesIris field; if false also fills genericAttributes field.
   */
  public void fillSurfaceGeometries(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    Query q = getFetchIrisQuery(iriName, SchemaManagerAdapter.ONTO_ROOT_ID);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      surfaceGeometries = new ArrayList<>();
      surfaceGeometriesIris = new ArrayList<>();

      for (int index = 0; index < queryResult.length(); index++) {
        String elementIri = queryResult.getJSONObject(index).getString(COLLECTION_ELEMENT_IRI);
        surfaceGeometriesIris.add(elementIri);

        if (!lazyLoad) {
          SurfaceGeometry surfaceGeometry = new SurfaceGeometry();
          surfaceGeometry.fillScalars(elementIri, kgClient);
          surfaceGeometries.add(surfaceGeometry);
        }
      }
    }
  }
}
