package org.citydb.database.adapter.blazegraph;

import java.util.HashSet;
import java.util.Set;

public class BlazegraphConfigBuilder {

    // Config builder for Blazegraph with double-check locking private instance so that it can be
    // accessed by only by getInstance() method. This ensures thread safe singleton.
    private static BlazegraphConfigBuilder instance;
    private final Set<String> geoDataTypes;

    private BlazegraphConfigBuilder()
    {
        // private constructor
        geoDataTypes = new HashSet<>();
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

    public void addGeoDataType(String geoDataType) {
        geoDataTypes.add(geoDataType);
    }

    public String build() {
        BlazegraphConfig config = new BlazegraphConfig(geoDataTypes);
        return config.getConfig();
    }

}
