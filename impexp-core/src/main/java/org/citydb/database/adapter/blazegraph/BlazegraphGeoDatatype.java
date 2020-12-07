package org.citydb.database.adapter.blazegraph;

import org.apache.jena.graph.Node;
import org.json.JSONArray;
import org.json.JSONObject;

public class BlazegraphGeoDatatype {

    private final String KEY_MAIN = "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialDatatypeConfig.";
    private final int sequenceNum = 0;
    private final String KEY_CONFIG = "config";
    private final String KEY_URI = "uri";
    private final String KEY_FIELDS = "fields";
    private final String KEY_VALUE_TYPE = "valueType";
    private final String KEY_MULTIPLIER = "100000";
    private final String KEY_SERVICE_MAPPING = "serviceMapping";
    private final String VAL_DOUBLE = "DOUBLE";
    private final String VAL_LONG = "LONG";
    private final String VAL_X = "DOUBLE";
    private final String VAL_Y = "X";
    private final String VAL_Z = "Z";


    BlazegraphGeoDatatype(Node dbobject) {
        JSONObject datatypeCfg = new JSONObject();
        JSONObject config = new JSONObject();
        JSONArray fields = new JSONArray();
        config.put(KEY_URI, dbobject.getLiteral().getDatatypeURI());
    }


    /*
    { "config":
        { "uri": "http://my-lat-lon-starttime-endtime-dt",
                "fields": [
            { "valueType": "DOUBLE", "multiplier": "100000", "serviceMapping": "LATITUDE" },
            { "valueType": "DOUBLE", "multiplier": "100000", "serviceMapping": "LONGITUDE" },
            { "valueType": "LONG", "serviceMapping": "starttime" },
            { "valueType": "LONG", "serviceMapping": "endtime" }
      ]
        }
    }
    */
}
