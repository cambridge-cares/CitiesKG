package org.citydb.database.adapter.blazegraph;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class BlazegraphConfigBuilder {

    // Config builder for Blazegraph with double-check locking private instance so that it can be
    // accessed by only by getInstance() method. This ensures thread safe singleton.
    private static BlazegraphConfigBuilder instance;
    private final Set<String> geoDataTypes;
    private final Set<String> uriStrings;

    private BlazegraphConfigBuilder()
    {
        // private constructor
        geoDataTypes = new HashSet<>();
        uriStrings = new HashSet<>();
    }

    public static BlazegraphConfigBuilder getInstance()
    {
        if (instance == null)
        {
            //synchronized block to remove overhead
            synchronized (BlazegraphConfigBuilder.class)
            {
                if(instance==null)
                {
                    // if instance is null, initialize
                    instance = new BlazegraphConfigBuilder();
                }
            }
        }
        return instance;
    }

    public BlazegraphConfigBuilder addGeoDataType(String geoDataType) {
        geoDataTypes.add(geoDataType);

        return instance;
    }

    public BlazegraphConfigBuilder addURIString(String uriStr) {
        uriStrings.add(uriStr);

        return instance;
    }

    public Properties buildVocabulariesConfig() {
        JSONArray cfgVal = new JSONArray();
        Properties prop = new Properties();

        cfgVal.putAll(uriStrings);
        prop.setProperty(BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_KEY_URIS, cfgVal.toString());

        return prop;
    }

    public Properties build() {
        BlazegraphConfig config = new BlazegraphConfig(geoDataTypes);
        return config.getConfig();
    }

}
