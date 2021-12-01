package uk.ac.cam.cares.twa.cities.models.geo;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.jena.query.Query;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.twa.cities.Model;

public class ThematicSurface extends Model {

  private URI buildingId;
  private URI buildingInstallationId;
  private URI id;
  private URI lod2MultiSurfaceId;
  private URI lod3MultiSurfaceId;
  private URI lod4MultiSurfaceId;
  private int objectClassId;
  private URI roomId;

  private ArrayList<SurfaceGeometry> surfaceGeometries;
  private ArrayList<String> surfaceGeometriesIris;

  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(SchemaManagerAdapter.ONTO_BUILDING_ID, SchemaManagerAdapter.ONTO_BUILDING_INSTALLATION_ID,
          SchemaManagerAdapter.ONTO_ID, SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID,
          SchemaManagerAdapter.ONTO_LOD3_MULTI_SURFACE_ID, SchemaManagerAdapter.ONTO_LOD4_MULTI_SURFACE_ID,
          SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, SchemaManagerAdapter.ONTO_ROOM_ID));

  protected HashMap<String, Field> fieldMap = new HashMap<>();

  public ThematicSurface() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
  }

  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
      throws IllegalAccessException {
    if (predicate.equals(SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID
        .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this, row.getInt(VALUE));
    } else {
      fieldMap.get(predicate).set(this,  URI.create(row.getString(VALUE)));
    }
  }

  /**
   * fills in generic attributes linked to the city object.
   * @param iriName cityObject IRI
   * @param kgClient sends the query to the right endpoint.
   * @param lazyLoad if true only fills genericAttributesIris field; if false also fills genericAttributes field.
   */
  public void fillSurfaceGeometries(String iriName, KnowledgeBaseClientInterface kgClient, Boolean lazyLoad)
      throws NoSuchFieldException, IllegalAccessException {

    Query q = getFetchIrisQuery(iriName, SchemaManagerAdapter.ONTO_CITY_OBJECT_ID);
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
