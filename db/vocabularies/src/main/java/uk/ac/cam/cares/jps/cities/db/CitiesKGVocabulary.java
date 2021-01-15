package uk.ac.cam.cares.jps.cities.db;

import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.vocab.BaseVocabularyDecl;
import com.bigdata.rdf.vocab.Vocabulary;
import com.bigdata.rdf.vocab.core.BigdataCoreVocabulary_v20160317;
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
    public final static String CFG_PATH = "config.properties";
    public final static String CFG_KEY_URIS = "db.uris";
    private final String CFG_ERR = "Could not load ";
    
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
        //try loading properties file
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CFG_PATH)) {
            Properties prop = new Properties();
            prop.load(input);

            //extract URIs from properties file
            JSONArray uris = new JSONArray(prop.getProperty(CFG_KEY_URIS));

            //add vocabulary declarations
            for (Object uri: uris) {
                URIImpl impl = new URIImpl(uri.toString());
                BaseVocabularyDecl decl = new BaseVocabularyDecl(impl);
                addDecl(decl);
            }

        } catch (IOException ex) {
            System.out.println(CFG_ERR + CFG_PATH);
        }

        super.addValues();
    }

}
