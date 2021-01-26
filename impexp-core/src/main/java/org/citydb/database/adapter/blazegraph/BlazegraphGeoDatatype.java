package org.citydb.database.adapter.blazegraph;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Class representing geo-datatype RWStore config entry for Blazegraph as described at:
 * {@link <a href="https://github.com/blazegraph/database/wiki/GeoSpatial#custom-geospatial-datatypes">}
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class BlazegraphGeoDatatype {

    public static final String KEY_MAIN = "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialDatatypeConfig.";
    private final String geodatatype;

    /**
     * Constructor:
     * - creates a geo-datatype string from {@link Node}
     *
     * @param dbobject geo-literal object to be converted to the geo-datatype string
     */
    public BlazegraphGeoDatatype(Node dbobject) {
        String KEY_CONFIG = "config";
        String KEY_URI = "uri";
        String KEY_FIELDS = "fields";
        String KEY_VALUE_TYPE = "valueType";
        String KEY_MULTIPLIER = "multiplier";
        String KEY_SERVICE_MAPPING = "serviceMapping";
        String VAL_MULTIPLIER = "100000";
        String VAL_DOUBLE = "DOUBLE";
        String VAL_X = "X";
        String VAL_Y = "Y";
        String VAL_Z = "Z";
        JSONObject datatypeCfg = new JSONObject();
        JSONObject config = new JSONObject();
        JSONArray fields = new JSONArray();

        int coords = Lists.partition(
                Arrays.asList(dbobject.getLiteral().getLexicalForm().split("#")), 3)
                .size();

        for (int c = 0; c < coords; c++) {
            for (int f = 0; f < 3; f++) {
                JSONObject field = new JSONObject();

                if (f == 0) {
                    field.put(KEY_SERVICE_MAPPING, VAL_X + c);
                } else if (f == 1) {
                    field.put(KEY_SERVICE_MAPPING, VAL_Y + c);
                } else {
                    field.put(KEY_SERVICE_MAPPING, VAL_Z + c);
                }
                field.put(KEY_MULTIPLIER, VAL_MULTIPLIER);
                field.put(KEY_VALUE_TYPE, VAL_DOUBLE);
                fields.put(field);
            }
        }
        config.put(KEY_FIELDS, fields);
        config.put(KEY_URI, dbobject.getLiteral().getDatatypeURI());
        datatypeCfg.put(KEY_CONFIG, config);

        geodatatype = datatypeCfg.toString();
    }

    /**
     * Returns geo-datatype string.
     * Used by: {@link GeometryConverterAdapter}
     *
     * @return geo-datatype RWStore config entry for Blazegraph
     */
    public String getGeodatatype() {
        return geodatatype;
    }

}
