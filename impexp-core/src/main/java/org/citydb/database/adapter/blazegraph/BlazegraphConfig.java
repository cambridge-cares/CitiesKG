package org.citydb.database.adapter.blazegraph;

import java.util.Set;

public class BlazegraphConfig {

    private String config;

    BlazegraphConfig(Set<String> geoDataTypes)
    {
        //@TODO replace this header with the real one.
        // Bellow samlple dumped from: http://www.geoweb-studien.nat.fau.de/wp-content/uploads/2018/06/RWStore.properties.txt
        config = "# Dump data in target.\n" +
                    "com.bigdata.journal.AbstractJournal.file=wikidata.jnl\n" +
                    "com.bigdata.journal.AbstractJournal.bufferMode=DiskRW\n" +
                    "com.bigdata.service.AbstractTransactionService.minReleaseAge=1\n" +
                    "\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.quads=false\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.statementIdentifiers=false\n" +
                    "\n" +
                    "# Don't use truth maintenance right yet.\n" +
                    "com.bigdata.rdf.sail.truthMaintenance=false\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.textIndex=false\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.axiomsClass=com.bigdata.rdf.axioms.NoAxioms\n" +
                    "\n" +
                    "# Use our private vocabularies\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.vocabularyClass=org.wikidata.query.rdf.blazegraph.WikibaseVocabulary$V002\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.inlineURIFactory=org.wikidata.query.rdf.blazegraph.WikibaseInlineUriFactory\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.extensionFactoryClass=org.wikidata.query.rdf.blazegraph.WikibaseExtensionFactory\n" +
                    "\n" +
                    "# Suggested settings from https://phabricator.wikimedia.org/T92308\n" +
                    "com.bigdata.btree.writeRetentionQueue.capacity=4000\n" +
                    "com.bigdata.btree.BTree.branchingFactor=128\n" +
                    "# 200M initial extent.\n" +
                    "com.bigdata.journal.AbstractJournal.initialExtent=209715200\n" +
                    "com.bigdata.journal.AbstractJournal.maximumExtent=2097152000\n" +
                    "# Bump up the branching factor for the lexicon indices on the default kb.\n" +
                    "com.bigdata.namespace.wdq.lex.com.bigdata.btree.BTree.branchingFactor=800\n" +
                    "com.bigdata.namespace.wdq.lex.ID2TERM.com.bigdata.btree.BTree.branchingFactor=1600\n" +
                    "com.bigdata.namespace.wdq.lex.TERM2ID.com.bigdata.btree.BTree.branchingFactor=256\n" +
                    "# Bump up the branching factor for the statement indices on the default kb.\n" +
                    "com.bigdata.namespace.wdq.spo.com.bigdata.btree.BTree.branchingFactor=2048\n" +
                    "com.bigdata.namespace.wdq.spo.OSP.com.bigdata.btree.BTree.branchingFactor=128\n" +
                    "com.bigdata.namespace.wdq.spo.SPO.com.bigdata.btree.BTree.branchingFactor=1200\n" +
                    "# larger statement buffer capacity for bulk loading.\n" +
                    "com.bigdata.rdf.sail.bufferCapacity=10000000\n" +
                    "# Override the #of write cache buffers to improve bulk load performance. Requires enough native heap!\n" +
                    "com.bigdata.journal.AbstractJournal.writeCacheBufferCount=30000\n" +
                    "# Enable small slot optimization!\n" +
                    "com.bigdata.rwstore.RWStore.smallSlotType=1024\n" +
                    "# See https://jira.blazegraph.com/browse/BLZG-1385 - reduce LRU cache timeout\n" +
                    "com.bigdata.journal.AbstractJournal.historicalIndexCacheCapacity=2000\n" +
                    "com.bigdata.journal.AbstractJournal.historicalIndexCacheTimeout=5000\n" +
                    "# Geospatial ON\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.geoSpatial=true\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialDefaultDatatype=http\\://www.opengis.net/ont/geosparql#wktLiteral\n" +
                    "com.bigdata.rdf.store.AbstractTripleStore.geoSpatialIncludeBuiltinDatatypes=false\n";

        Object[] list = geoDataTypes.toArray();

        for (int i = 0; i < geoDataTypes.size(); i++) {
            config = config.concat(BlazegraphGeoDatatype.KEY_MAIN + i + "=" + list[i] + "\n");
        }
    }

    public String getConfig() {
        return config;
    }
}
