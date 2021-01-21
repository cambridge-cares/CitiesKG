package org.citydb.database.adapter.blazegraph;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Config builder for Blazegraph with double-check locking private instance so that it can be
 * accessed by only by getInstance() method. This ensures thread safe singleton.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class BlazegraphConfigBuilder {
    private static final transient Logger log = Logger.getLogger(BlazegraphConfigBuilder.class);
    protected final String CFG_ERR = "Could not load ";
    private static BlazegraphConfigBuilder instance;
    private final Set<String> geoDataTypes;
    private final Set<String> uriStrings;

    private BlazegraphConfigBuilder() {
        // private constructor
        geoDataTypes = new HashSet<>();
        uriStrings = new HashSet<>();
    }

    public static BlazegraphConfigBuilder getInstance() {
        if (instance == null) {
            //synchronized block to remove overhead
            synchronized (BlazegraphConfigBuilder.class) {
                if(instance==null) {
                    // if instance is null, initialize
                    instance = new BlazegraphConfigBuilder();
                    //load existing configuration:
                    instance.loadURIs();
                    instance.loadDatatypes();
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

    private void loadURIs() {
        String path;
        String key;
        try {
            path = BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_PATH;
            key = BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_KEY_URIS;
            Properties prop = loadProperties(path);

            JSONArray uris = new JSONArray(prop.getProperty(key));

            //add vocabulary declarations to existing uriStrings set
            for (Object uri: uris) {
                uriStrings.add(uri.toString());
            }
        } catch (IOException e) {
            log.error(CFG_ERR + BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_PATH);
        }
    }

    private void loadDatatypes() {
        String path;
        try {
            path = BlazegraphAdapter.BLAZEGRAPH_CFG_PATH;
            Properties prop = loadProperties(path);
            //extract datatype configs and to existing geoDataTypes set
            for (Object cfg: loadProperties(path).stringPropertyNames().toArray()) {
                if (String.valueOf(cfg).matches(BlazegraphGeoDatatype.KEY_MAIN+"[0-9]")) {
                    geoDataTypes.add(prop.getProperty(String.valueOf(cfg)));
                }
            }

        } catch (IOException e) {
            log.error(CFG_ERR + BlazegraphAdapter.BLAZEGRAPH_CFG_PATH);
        }
    }


    private Properties loadProperties(String path) throws IOException {
        //try loading properties file
        InputStream input = new FileInputStream(path);
        Properties prop = new Properties();
        prop.load(input);

        return prop;
    }

}
