call java  -Xmx32g -classpath "vocabularies/json-20190722.jar;vocabularies/vocabularies-1.0.1.jar;blazegraph.jar" -server com.bigdata.rdf.sail.webapp.NanoSparqlServer 9999 berlin RWStore.properties