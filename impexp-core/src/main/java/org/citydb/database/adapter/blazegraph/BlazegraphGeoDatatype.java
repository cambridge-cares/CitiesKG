package org.citydb.database.adapter.blazegraph;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class BlazegraphGeoDatatype {

    public static final String KEY_MAIN = "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialDatatypeConfig.";
    private final String KEY_CONFIG = "config";
    private final String KEY_URI = "uri";
    private final String KEY_FIELDS = "fields";
    private final String KEY_VALUE_TYPE = "valueType";
    private final String KEY_MULTIPLIER = "multiplier";
    private final String KEY_SERVICE_MAPPING = "serviceMapping";
    private final String VAL_MULTIPLIER = "100000";
    private final String VAL_DOUBLE = "DOUBLE";
    private final String VAL_X = "X";
    private final String VAL_Y = "Y";
    private final String VAL_Z = "Z";
    private String geodatatype;

    BlazegraphGeoDatatype(Node dbobject) {
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
                    field.put(KEY_SERVICE_MAPPING, VAL_X);
                } else if (f == 1) {
                    field.put(KEY_SERVICE_MAPPING, VAL_Y);
                } else {
                    field.put(KEY_SERVICE_MAPPING, VAL_Z);
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

    public String getGeodatatype() {
        return geodatatype;
    }

}
