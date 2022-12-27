# CEA Agent


## Description

The CEA agent can be used to interact with the [City Energy Analyst (CEA)](https://www.cityenergyanalyst.com/) and the data it produces on building energy demands and the electricity supply available if PV panels are placed on available surfaces.

The agent currently queries for building geometry and building usage stored in the knowledge graph, and the resulting output data is added to the named graph 'energyprofile'.

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


In order for the agent to run the CEA successfully, the queries below must return a result with an IRI of format `<{PREFIX}cityobject/{UUID}/>` where PREFIX is the prefix to IRIs in the namespace you are working with. 

For example:
 - `http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_b5a7b032-1877-4c2b-9e00-714b33a426f7/` - the PREFIX is: `http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/`

 - `http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG27700/sparql/building/UUID_7cb00a09-528b-4016-b3d6-80c5a9442d95/` - the PREFIX is `http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG27700/sparql/`.

Query for coordinate reference system (CRS) EPSG id of the namespace:

```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?CRS
WHERE
  { GRAPH <{PREFIX}databasesrs/>
      { <{PREFIX}> ocgml:srid  ?CRS}}
```
If no CRS is stored in the namespace please insert the data in the databasesrs graph. For example:
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

INSERT DATA
{
GRAPH <{PREFIX}databasesrs/> {
<{PREFIX}>	ocgml:srid	27700 .
<{PREFIX}>	ocgml:srsname "EPSG:27700" .
}}
```

Query for footprint of the building - the agent will try querying for the thematic surface with surface id 35 which corresponds to a ground surface:
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Footprint
WHERE
  { GRAPH <{PREFIX}surfacegeometry/>
      { ?surf  ocgml:cityObjectId  ?id ;
               ocgml:GeometryType  ?Footprint
        FILTER ( ! isBlank(?Footprint) )
   GRAPH <{PREFIX}thematicsurface/> 
       { ?id ocgml:buildingId <{PREFIX}building/{UUID}/>;
    		 ocgml:objectClassId ?groundSurfId.
   FILTER(?groundSurfId = 35) 
   }}
```
If unsuccessful, the agent will query all building surface geometries with the following query, where the ground surface geometries are selected by searching for the surface with the minimum constant height:
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Footprint
WHERE
  { GRAPH <{PREFIX}surfacegeometry/>
      { ?surf  ocgml:cityObjectId  <{PREFIX}building/{UUID}/> ;
               ocgml:GeometryType  ?Footprint
        FILTER ( ! isBlank(?Footprint) )
      }}
```

After getting the ground surface geometries of the building, the footprint geometry of the building is extracted by merging the ground surface geometries.

For building height, the following three different following queries are possible. Each are tried in this order until a result is retrieved. If all unsuccessful, a default value for height of 10.0m is set.
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Height
WHERE
  { GRAPH <{PREFIX}building/>
      { <{PREFIX}building/{UUID}/> ocgml:measuredHeigh  ?Height}}
```

```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Height
WHERE
  { GRAPH <{PREFIX}building/>
      { <{PREFIX}building/{UUID}/> ocgml:measuredHeight  ?Height}}
```
```
PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>

SELECT  ?Height
WHERE
  { GRAPH <{PREFIX}cityobjectgenericattrib/>
      { ?o  ocgml:attrName      "height" ;
            ocgml:realVal       ?Height ;
            ocgml:cityObjectId  <{PREFIX}cityobject/{UUID}/>}}

```

The agent queries for the building usage with the following query:
```
PREFIX ontobuiltenv: <https://www.theworldavatar.com/kg/ontobuiltenv/>

SELECT  ?BuildingUsage
WHERE
  { ?building  ontobuiltenv:hasOntoCityGMLRepresentation  {PREFIX}building/{UUID}/> ;
             ontobuiltenv:hasUsageCategory  ?usage .
    ?usage a ?BuildingUsage}
```

If no building usage is returned from the query, the default value of ```MULTI_RES``` building usage is set, consistent with the default building use type set by the CEA.


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
The 3dWebMapClient can be set up to visualise data produced by the CEA Agent (instructions to run are [here](https://github.com/cambridge-cares/CitiesKG/tree/develop/agents#3dcitydb-web-map-client)). The City Information Agent (CIA) is used when a building on the 3dWebMapClient is selected, to query data stored in the KG on the building. If the parameter "context=energy" is included in the url, the query endpoint of CEA will be contacted for energy data. eg `http://localhost:8000/3dwebclient/index.html?city=kingslynn&context=energy` (NB. this currently requires running web map client and CitiesKG/agents from [develop branch](https://github.com/cambridge-cares/CitiesKG/tree/develop/agents))

### Building usage mapping
The agent queries for the building usage type, which are stored with ```OntoBuiltEnv``` concepts, to pass to CEA as input.

In the CEA, there are 19 defined building usage types. In the ```OntoBuiltEnv``` ontology, there are 23 classes for building usage type (see left 2 columns of table below). After querying for the ```OntoBuiltEnv``` concepts, the agent will map the concepts to the CEA defined usage types as shown in the right 2 columns of the following mapping table:


| CEA available usage types | ```OntoBuiltEnv``` concepts |   | ```OntoBuiltEnv``` concepts | Mapped to CEA usage type |
|:-------------------------:|:---------------------------:|:-:|:---------------------------:|:------------------------:|
|         COOLROOM          |          Domestic           |   |          Domestic           |        MULTI_RES         |
|         FOODSTORE         |      SingleResidential      |   |      SingleResidential      |        SINGLE_RES        |
|            GYM            |      MultiResidential       |   |      MultiResidential       |        MULTI_RES         |
|         HOSPITAL          |      EmergencyService       |   |      EmergencyService       |         HOSPITAL         |
|           HOTEL           |         FireStation         |   |         FireStation         |         HOSPITAL         |
|        INDUSTRIAL         |        PoliceStation        |   |        PoliceStation        |         HOSPITAL         |
|            LAB            |         MedicalCare         |   |         MedicalCare         |         HOSPITAL         |
|          LIBRARY          |          Hospital           |   |          Hospital           |         HOSPITAL         |
|         MULTI_RES         |           Clinic            |   |           Clinic            |         HOSPITAL         |
|          MUSEUM           |          Education          |   |          Education          |        UNIVERSITY        |
|          OFFICE           |           School            |   |           School            |          SCHOOL          |
|          PARKING          |     UniversityFacility      |   |     UniversityFacility      |        UNIVERSITY        |
|        RESTAURANT         |           Office            |   |           Office            |          OFFICE          |
|          RETAIL           |     RetailEstablishment     |   |     RetailEstablishment     |          RETAIL          |
|          SCHOOL           |      ReligiousFacility      |   |      ReligiousFacility      |          MUSEUM          |
|        SERVERROOM         |     IndustrialFacility      |   |     IndustrialFacility      |        INDUSTRIAL        |
|        SINGLE_RES         |     EatingEstablishment     |   |     EatingEstablishment     |        RESTAURANT        |
|         SWIMMING          |    DrinkingEstablishment    |   |    DrinkingEstablishment    |        RESTAURANT        |
|        UNIVERSITY         |            Hotel            |   |            Hotel            |          HOTEL           |
|                           |       SportsFacility        |   |       SportsFacility        |           GYM            |
|                           |      CulturalFacility       |   |      CulturalFacility       |          MUSEUM          |
|                           |      TransportFacility      |   |      TransportFacility      |        INDUSTRIAL        |
|                           |        Non-Domestic         |   |        Non-Domestic         |        INDUSTRIAL        |

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

The agent also requires a postgreSQL database for the time series client to save data in. The address of the database used, as well as the SPARQL query and update endpoints (eg. `http://host.docker.internal:9999/blazegraph/namespace/kings-lynn/sparql`), need to be provided in:
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

### Building usage query route
Please provide the route to query for building usage type in ```usage.query.route``` in
```
./cea-agent/src/main/resources
    CEAAgentConfig.properties
```

If ```usage.query.route``` is not provided, the agent will attempt to query for building usage from the same endpoint as the building geometry query.

### Access Agent

The CEA agent also uses the [access agent](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/JPS_ACCESS_AGENT). 
Check that a mapping to a targetresourceid to pass to the access agent exists for the namespace being used for building geometry query. 
Currently included are:

- citieskg-berlin
- singaporeEPSG24500
- citieskg-singaporeEPSG4326
- citieskg-kingslynnEPSG3857
- citieskg-kingslynnEPSG27700
- citieskg-pirmasensEPSG32633

If not included, you will need to add a mapping to accessAgentRoutes in CEAAgent. 

#### For developers
In order to use a local Blazegraph, you will need to run the access agent locally and set the acessagent.properties storerouter endpoint url to your local Blazegraph, as well as add triples for your namespace to a local ontokgrouter as is explained [here](https://github.com/cambridge-cares/CitiesKG/tree/develop/agents#install-and-build-local-accessagent-for-developers). 
The route for building geometry queries to be passed to the access agent then needs to be provided in ```query.route.local``` in
```
./cea-agent/src/main/resources
    CEAAgentConfig.properties
```
The route should contain the port number your access agent is running on (eg. 48080) and the label set in your ontokgrouter (eg docker-kings-lynn). host.docker.internal is required to access localhost from a docker container.

eg. `uri.route.local=http://host.docker.internal:48080/docker-kings-lynn`

If you no longer want to use a local route, ensure you leave uri.route.local empty.

Check [here](https://www.dropbox.com/s/z5dkdg5puqkfjtw/RunningCEAAgentLocallyGuide.pdf?dl=0) for a detailed guide on running CEA Agent locally.

### Running

To build and start the agent, you simply need to spin up a container.
In Visual Studio Code, ensure the Docker extension is installed, then right-click docker-compose.yml and select 'Compose Up'. 
Alternatively, from the command line, and in the same directory as this README, run
```
docker-compose up -d
```
The agent is reachable at "agents/cea/{option}" on localhost port 58085 where option can be run,update or query.
