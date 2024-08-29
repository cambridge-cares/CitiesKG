#!/bin/bash

cd ~/ckg_demo/CitiesKG
sudo docker-compose up &

cd ~/ckg_demo/blazegraph
java -Xmx8g -classpath "json-20190722.jar:vocabularies-1.0.1.jar:blazegraph.jar" -server com.bigdata.rdf.sail.webapp.NanoSparqlServer 9999 berlin RWStore.properties &

