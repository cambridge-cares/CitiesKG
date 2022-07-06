# CEA Agent


## Description

The CEA agent can be used to interact with the [City Energy Analyst (CEA)](https://www.cityenergyanalyst.com/) and the data it produces on building energy demands and the electricity supply available if PV panels are placed on available surfaces.

The agent reads data from quads stored in a namespace in a Blazegraph workbench. Geometry data is currently queried from OntoCityGml graphs and the resulting output data is added to the named graph 'energyprofile'.

The CEA Agent provides three endpoints:

### 1. Run
Available at http://localhost:58085/agents/cea/run

This requires JSON to be sent in the request body including an array of cityobject **iris** (in future will also optionally accept a namespace) and a **targetUrl** (to send the update request to).
Example request:
```
{ "iris" :
["http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_b5a7b032-1877-4c2b-9e00-714b33a426f7/"],
"targetUrl" :  "http://host.docker.internal:58085/agents/cea/update"}
```


In order for the agent to run the CEA successfully, the following queries must return a result with an IRI of format `<{blazegraph url + namespace}/sparql/cityobject/{UUID}/>`:

For coordinate reference system (CRS) of the namespace:

```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?CRS
WHERE
  { GRAPH <{blazegraph url + namespace}/sparql/databasesrs/>
      { <{blazegraph url + namespace}/sparql/> ocgml:srid  ?CRS}}
```

For building surfaces, which the footprint is selected from:
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Footprint
WHERE
  { GRAPH <{blazegraph url + namespace}/sparql/surfacegeometry/>
      { ?surf  ocgml:cityObjectId  <{blazegraph url + namespace}/sparql/building/{UUID}/> ;
               ocgml:GeometryType  ?Footprint
        FILTER ( ! isBlank(?Footprint) )
      }}
```

For building height, two different queries are possible. For IRIs containing kings-lynn-open-data the following query is used.
```

PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Height
WHERE
  { GRAPH <{blazegraph url + namespace}/sparql/building/>
      { <{blazegraph url + namespace}/sparql/building/{UUID}/> ocgml:measuredHeight  ?Height}}
```
Otherwise:
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Height
WHERE
  { GRAPH <{blazegraph url + namespace}/sparql/cityobjectgenericattrib/>
      { ?o  ocgml:attrName      "height" ;
            ocgml:realVal       ?Height ;
            ocgml:cityObjectId  <{blazegraph url + namespace}/sparql/cityobject/{UUID}/>}}

```
If both unsuccessful, a default value for height of 10.0m is set.


### 2. Update

Available at http://localhost:58085/agents/cea/update 

Provide data from CEA to update KG with (request sent automatically by cea/run)

### 3. Query

Available at http://localhost:58085/agents/cea/query

Provide an array of cityobject IRIs in the request parameters to retrieve the energy profile for.
Example request:
```
{"iris": 
["http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_b5a7b032-1877-4c2b-9e00-714b33a426f7/"]
}
```

Example response:
```
{
    "path": "/agents/cea/query",
    "iris": [
        "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_b5a7b032-1877-4c2b-9e00-714b33a426f7/"
    ],
    "acceptHeaders": "*/*",
    "method": "POST",
    "requestUrl": "http://localhost:58085/agents/cea/query",
    "energyprofile": [
        {
            "Annual heating_demand": "46715.11 kWh",
            "Annual PV_supply_wall_east": "1805.54 kWh",
            "PV_area_wall_east": "40.14 m^2",
            "Annual grid_demand": "4828.71 kWh",
            "PV_area_wall_south": "89.36 m^2",
            "PV_area_roof": "54.29 m^2",
            "Annual PV_supply_wall_south": "3906.91 kWh",
            "Annual electricity_demand": "4828.71 kWh",
            "Annual PV_supply_roof": "6604.36 kWh"
        }
    ],
    "body": "{\"iris\": \r\n[\"http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_b5a7b032-1877-4c2b-9e00-714b33a426f7/\"]\r\n}",
    "contentType": "application/json"
}

```
The 3dWebMapClient can be set up to visualise data produced by the CEA Agent (instructions to run are [here](https://github.com/cambridge-cares/CitiesKG/tree/develop/agents#3dcitydb-web-map-client)). The City Information Agent (CIA) is used when a building on the 3dWebMapClient is selected, to query data stored in the KG on the building. If the parameter "context=energy" is included in the url, the query endpoint of CEA will be contacted for energy data. eg `http://localhost:8000/3dwebclient/index.html?city=kingslynn&context=energy` (NB. this currently requires running web map client and CitiesKG/agents from [develop branch](https://github.com/cambridge-cares/CitiesKG/tree/develop/agents) (**Update when released**)

## Build Instructions

### maven

The docker image uses the world avatar maven repository ( https://maven.pkg.github.com/cambridge-cares/TheWorldAvatar/). 
You'll need to provide your credentials (github username/personal access token) in single-word text files located:
```
./credentials/
    repo_username.txt
    repo_password.txt
```

### postgreSQL

The agent also requires a postgreSQL database for the time series client to save data in. The address of the database used, as well as the SPARQL query and update endpoints, need to be provided in:
```
./cea-agent/src/main/resources
    timeseriesclient.properties
```

The username and password for the postgreSQL database need to be provided in single-word text files in:
```
./credentials/
    postgres_username.txt
    postgres_password.txt
```

### Access Agent

The agent also requires the [access agent](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/JPS_ACCESS_AGENT) to be running. 
Check that a mapping to a targetresourceid to pass to the access agent exists for the namespace being used. 
Currently included are:

- citieskg-berlin
- singaporeEPSG24500
- citieskg-singaporeEPSG4326
- citieskg-kingslynnEPSG3857
- citieskg-kingslynnEPSG27700

If not included, you will need to add a mapping to accessAgentRoutes in CEAAgent. 

#### For developers
In order to use a local Blazegraph, you will need to set your access agent up to run locally and add triples to a local ontokgrouter as is explained [here](https://github.com/cambridge-cares/CitiesKG/tree/develop/agents#install-and-build-local-accessagent-for-developers). 
The route to be passed to the access agent then needs to be provided in uri.route.local in:
```
./cea-agent/src/main/resources
    CEAAgentConfig.properties
```
eg. `uri.route.local=http://host.docker.internal:48080/docker-kings-lynn`

If you no longer want to use a local route, ensure you leave uri.route.local empty.

### Running

To build and start the agent, you simply need to spin up a container.
In Visual Studio Code, ensure the Docker extension is installed, then right-click docker-compose.yml and select 'Compose Up'. 
Alternatively, from the command line, and in the same directory as this README, run
```
docker-compose up -d
```
The agent is reachable at "agents/cea/{option}" on localhost port 58085 where option can be run,update or query.
