package org.citydb.database.adapter.blazegraph;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONArray;

/**
 * Config builder for Blazegraph with double-check locking private instance so that it can be
 * accessed by only by getInstance() method. This ensures thread safe singleton.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class BlazegraphConfigBuilder {

  private static final transient Logger log = Logger.getLogger(BlazegraphConfigBuilder.class);
  private static BlazegraphConfigBuilder instance;
  protected final String CFG_ERR = "Could not load ";
  private final Set<String> geoDataTypes;
  private final Set<String> uriStrings;

  /**
   * Private constructor: - initialises sets for RWStore and vocabularies config entries.
   */
  private BlazegraphConfigBuilder() {
    geoDataTypes = new HashSet<>();
    uriStrings = new HashSet<>();
  }

  /**
   * Method to get singleton of {@link BlazegraphConfigBuilder}.
   *
   * @return instance of {@link BlazegraphConfigBuilder}
   */
  public static BlazegraphConfigBuilder getInstance() {
    if (instance == null) {
      //synchronized block to remove overhead
      synchronized (BlazegraphConfigBuilder.class) {
        if (instance == null) {
          // if instance is null, initialize
          instance = new BlazegraphConfigBuilder();
          //load existing configuration:
          instance.loadURIs(BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_PATH);
          instance.loadDatatypes(BlazegraphAdapter.BLAZEGRAPH_CFG_PATH);
        }
      }
    }
    return instance;
  }

  /**
   * Adds {@link BlazegraphGeoDatatype} string in the format to the set of configuration strings.
   *
   * @param geoDataType {@link BlazegraphGeoDatatype} configuration string
   * @return instance of {@link BlazegraphConfigBuilder}, for method chaining
   */
  public BlazegraphConfigBuilder addGeoDataType(String geoDataType) {
    geoDataTypes.add(geoDataType);

    return instance;
  }

  /**
   * Adds URI string corresponding to a {@link BlazegraphGeoDatatype} to the set of configuration
   * strings.
   *
   * @param uriStr URI string for a {@link BlazegraphGeoDatatype}
   */
  public void addURIString(String uriStr) {
    uriStrings.add(uriStr);

  }

  /**
   * Builds config for custom Blazegraph vocabulary in {@link Properties} format.
   *
   * @return {@link BlazegraphGeoDatatype} for custom Blazegraph vocabulary
   */
  public Properties buildVocabulariesConfig() {
    JSONArray cfgVal = new JSONArray();
    Properties prop = new Properties();

    cfgVal.putAll(uriStrings);
    prop.setProperty(BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_KEY_URIS, cfgVal.toString());

    return prop;
  }

  /**
   * Builds config for Blazegraph RWStore in {@link Properties} format.
   *
   * @return {@link BlazegraphGeoDatatype} for Blazegraph RWStore
   */
  public Properties build() {
    BlazegraphConfig config = new BlazegraphConfig(geoDataTypes);
    return config.getConfig();
  }

  /**
   * Loads URI strings corresponding to a {@link BlazegraphGeoDatatype} to the set of configuration
   * strings from existing config.properties in {@link BlazegraphAdapter#BLAZEGRAPH_VOCAB_CFG_PATH}.
   * <p>
   * Used by {@link BlazegraphConfigBuilder#getInstance()}
   */
  private void loadURIs(String path) {
    String key;
    try {
      key = BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_KEY_URIS;
      Properties prop = loadProperties(path);

      JSONArray uris = new JSONArray(prop.getProperty(key));

      //add vocabulary declarations to existing uriStrings set
      for (Object uri : uris) {
        uriStrings.add(uri.toString());
      }
    } catch (IOException e) {
      log.error(CFG_ERR + BlazegraphAdapter.BLAZEGRAPH_VOCAB_CFG_PATH);
    }
  }

  /**
   * Loads {@link BlazegraphGeoDatatype} string to the set of configuration strings from existing
   * RWStore.properties in: {@link BlazegraphAdapter#BLAZEGRAPH_VOCAB_CFG_PATH}.
   * <p>
   * Used by {@link BlazegraphConfigBuilder#getInstance()}
   */
  private void loadDatatypes(String path) {
    try {
      Properties prop = loadProperties(path);
      //extract datatype configs and to existing geoDataTypes set
      for (Object cfg : loadProperties(path).stringPropertyNames().toArray()) {
        if (String.valueOf(cfg).matches(BlazegraphGeoDatatype.KEY_MAIN + "[0-9]")) {
          geoDataTypes.add(prop.getProperty(String.valueOf(cfg)));
        }
      }

    } catch (IOException e) {
      log.error(CFG_ERR + BlazegraphAdapter.BLAZEGRAPH_CFG_PATH);
    }
  }


  /**
   * General method to load {@link Properties} from a file at a given path. Used by: {@link
   * BlazegraphConfigBuilder#loadDatatypes(String)} and {@link BlazegraphConfigBuilder#loadURIs(String)}
   *
   * @param path to the {@link Properties} file.
   * @return {@link Properties} loaded from a given path.
   * @throws IOException when {@link Properties} could not be loaded from a path.
   */
  private Properties loadProperties(String path) throws IOException {
    //try loading properties file
    InputStream input = new FileInputStream(path);
    Properties prop = new Properties();
    prop.load(input);
    input.close();

    return prop;
  }

}
