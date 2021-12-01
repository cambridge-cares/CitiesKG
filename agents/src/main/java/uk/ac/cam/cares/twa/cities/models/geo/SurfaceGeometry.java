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

public class SurfaceGeometry  extends Model {

  private URI cityObjectId;
  private URI id;
  private String ImplicitGeometryType;
  private int isComposite;
  private int isReverse;
  private int isSolid;
  private int isTriangulated;
  private int isXlink;
  private URI parentId;
  private URI rootId;
  private String SolidType;
  private String GeometryType;
  private String gmlId;

  private ArrayList<SurfaceGeometry> surfaceGeometries;
  private ArrayList<String> surfaceGeometriesIris;

  private static final ArrayList<String> FIELD_CONSTANTS = new ArrayList<>
      (Arrays.asList(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, SchemaManagerAdapter.ONTO_ID,
          SchemaManagerAdapter.ONTO_GEOMETRY_IMPLICIT, SchemaManagerAdapter.ONTO_IS_COMPOSITE,
          SchemaManagerAdapter.ONTO_IS_REVERSE, SchemaManagerAdapter.ONTO_IS_SOLID,
          SchemaManagerAdapter.ONTO_IS_TRIANGULATED, SchemaManagerAdapter.ONTO_IS_XLINK,
          SchemaManagerAdapter.ONTO_PARENT_ID, SchemaManagerAdapter.ONTO_ROOT_ID,
          SchemaManagerAdapter.ONTO_GEOMETRY_SOLID, SchemaManagerAdapter.ONTO_GEOMETRY,
          SchemaManagerAdapter.ONTO_GML_ID));

  protected HashMap<String, Field> fieldMap = new HashMap<>();

  public SurfaceGeometry() throws NoSuchFieldException {
    assignFieldValues(FIELD_CONSTANTS, fieldMap);
  }

  protected void assignScalarValueByRow(JSONObject row, HashMap<String, Field> fieldMap, String predicate)
      throws IllegalAccessException {
    if (predicate.equals(SchemaManagerAdapter.ONTO_IS_COMPOSITE
        .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_REVERSE
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_SOLID
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_TRIANGULATED
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_IS_XLINK
        .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this, row.getInt(VALUE));
    } else if (predicate.equals(SchemaManagerAdapter.ONTO_CITY_OBJECT_ID
        .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_ID
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_PARENT_ID
            .replace(OCGML + ":", "")) ||
        predicate.equals(SchemaManagerAdapter.ONTO_ROOT_ID
            .replace(OCGML + ":", ""))) {
      fieldMap.get(predicate).set(this,  URI.create(row.getString(VALUE)));
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
