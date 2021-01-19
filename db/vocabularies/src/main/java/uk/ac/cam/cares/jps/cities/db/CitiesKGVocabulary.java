package uk.ac.cam.cares.jps.cities.db;

import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.vocab.BaseVocabularyDecl;
import com.bigdata.rdf.vocab.Vocabulary;
import com.bigdata.rdf.vocab.core.BigdataCoreVocabulary_v20160317;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.openrdf.model.impl.URIImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * 
 * A {@link Vocabulary} covering the CitiesKG data from {@link <a href="https://www.cares.cam.ac.uk/research/cities/"</a>}.
 * Use the vocabulary by adding a property to your journal file per below.
 * 
 * <code>
 * com.bigdata.rdf.store.AbstractTripleStore.vocabularyClass=uk.ac.cam.cares.jps.cities.blazegraph.vocabularies.CitiesKGVocabulary
 * </code>
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 * 
 */
public class CitiesKGVocabulary extends BigdataCoreVocabulary_v20160317 {
    private static final transient Logger log = Logger.getLogger(CitiesKGVocabulary.class);
    public final static String CFG_PATH = "config.properties";
    public final static String CFG_KEY_URIS = "db.uris";
    protected final String CFG_ERR = "Could not load ";

    /**
     * De-serialization ctor.
     */
    public CitiesKGVocabulary() {

        super();

    }

    /**
     * Used by {@link AbstractTripleStore#create()}.
     *
     * @param namespace
     *            The namespace of the KB instance.
     */
    public CitiesKGVocabulary(final String namespace) {

        super(namespace);

    }

    @Override
    protected void addValues() {
        String path;
        String key;
        try {
            path = CFG_PATH;
            key = CFG_KEY_URIS;
            JSONArray uris = getPropertyFromPath(path, key);

            //add vocabulary declarations
            for (Object uri: uris) {
                URIImpl impl = new URIImpl(uri.toString());
                BaseVocabularyDecl decl = new BaseVocabularyDecl(impl);
                addDecl(decl);
            }
        } catch (IOException e) {
            log.error(CFG_ERR + CFG_PATH);
        }

        super.addValues();
    }

    /**
     * Method to read and extract URI vocabulary items from
     * configuration properties file.
     *
     * Used by {@link CitiesKGVocabulary#addValues()}
     *
     * @param path - path to configuration file
     * @param key - key for the URIs value in the configuration file
     * @throws IOException if configuration file could not be loaded
     * @return JSON array of URIs
     */
    private JSONArray getPropertyFromPath(String path, String key) throws IOException {

        //try loading properties file
        InputStream input = getClass().getClassLoader().getResourceAsStream(path);
        Properties prop = new Properties();
        prop.load(input);

        //extract URIs from properties file

        return new JSONArray(prop.getProperty(key));
    }

}
