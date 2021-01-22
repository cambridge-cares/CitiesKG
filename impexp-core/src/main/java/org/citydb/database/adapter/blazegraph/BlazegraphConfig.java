package org.citydb.database.adapter.blazegraph;

import java.util.Properties;
import java.util.Set;

/**
 * Class representing RWStore config for Blazegraph.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class BlazegraphConfig {
    
    private final Properties config;

    /**
     * Constructor
     * - builds config in the required format.
     *
     * @param geoDataTypes Set of {@link BlazegraphGeoDatatype} strings.
     */
    BlazegraphConfig(Set<String> geoDataTypes) {
        Object[] list = geoDataTypes.toArray();
        config = new Properties();
        //set general properties
        config.setProperty("com.bigdata.journal.AbstractJournal.file","citiesKG.jnl");
        config.setProperty("com.bigdata.journal.AbstractJournal.bufferMode","DiskRW");
        config.setProperty("com.bigdata.service.AbstractTransactionService.minReleaseAge","1");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.quads","true");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.statementIdentifiers","false");
        config.setProperty("com.bigdata.rdf.sail.truthMaintenance","false");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.textIndex","false");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.axiomsClass","com.bigdata.rdf.axioms.NoAxioms");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.vocabularyClass","uk.ac.cam.cares.jps.cities.db.CitiesKGVocabulary");
        config.setProperty("com.bigdata.btree.writeRetentionQueue.capacity","4000");
        config.setProperty("com.bigdata.btree.BTree.branchingFactor","128");
        config.setProperty("com.bigdata.journal.AbstractJournal.initialExtent","209715200");
        config.setProperty("com.bigdata.journal.AbstractJournal.maximumExtent","2097152000");
        config.setProperty("com.bigdata.namespace.wdq.lex.com.bigdata.btree.BTree.branchingFactor","800");
        config.setProperty("com.bigdata.namespace.wdq.lex.ID2TERM.com.bigdata.btree.BTree.branchingFactor","1600");
        config.setProperty("com.bigdata.namespace.wdq.lex.TERM2ID.com.bigdata.btree.BTree.branchingFactor","256");
        config.setProperty("com.bigdata.namespace.wdq.spo.com.bigdata.btree.BTree.branchingFactor","2048");
        config.setProperty("com.bigdata.namespace.wdq.spo.OSP.com.bigdata.btree.BTree.branchingFactor","128");
        config.setProperty("com.bigdata.namespace.wdq.spo.SPO.com.bigdata.btree.BTree.branchingFactor","1200");
        config.setProperty("com.bigdata.rdf.sail.bufferCapacity","10000000");
        config.setProperty("com.bigdata.journal.AbstractJournal.writeCacheBufferCount","3000");
        config.setProperty("com.bigdata.rwstore.RWStore.smallSlotType","1024");
        config.setProperty("com.bigdata.journal.AbstractJournal.historicalIndexCacheCapacity","2000");
        config.setProperty("com.bigdata.journal.AbstractJournal.historicalIndexCacheTimeout","5000");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.geoSpatial","true");
        config.setProperty("com.bigdata.rdf.store.AbstractTripleStore.geoSpatialIncludeBuiltinDatatypes","true");
        //set {@link BlazegraphGeoDatatype} strings
        for (int i = 0; i < geoDataTypes.size(); i++) {
            config.setProperty(BlazegraphGeoDatatype.KEY_MAIN + i, (String) list[i]);
        }
    }

    /**
     * Returns config in the required format.
     *
     * @return config in RWStore.properties format.
     */
    public Properties getConfig() {
        return config;
    }
}
