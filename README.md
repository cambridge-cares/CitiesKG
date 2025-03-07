
<p align="center"><a href="https://fcl.ethz.ch/research/research-projects/cities-knowledge-graph.html" target="_blank" rel="noopener noreferrer"><img width="50%" src="web/media/CKG_Logo.png" alt="Cities Knowledge Graph logo"></a></p>

# Cities Knowledge Graph

## Introduction 

This research aims to harness rapidly growing and diversifying data streams to improve the planning and design of cities. We have developed an innovative digital platform, 
known as the [Cities Knowledge Graph (CKG)](https://fcl.ethz.ch/research/research-projects/cities-knowledge-graph.html), designed to combine data and share knowledge about cities, and to inject new precision and responsiveness to static instruments of planning, 
such as the city master-plan. 

Cities Knowledge Graph platform is an augmented version of the [3D City Database](https://github.com/3dcitydb/3dcitydb) platform. 
The augmented [Importer/Exporter](https://github.com/3dcitydb/importer-exporter) is a Java based front-end and is extended to work with RDF-based graph database in addition to relational database. 
The example RDF-based database is called [Blazegraph](https://github.com/blazegraph), which is an ultra high-performance graph database supporting RDF and SPARQL APIs.
It allows for high-performance loading and extracting 3D city model data. 

Cities Knowledge Graph is an Intra-CREATE collaborative project under the urban systems theme. The project brings together expertise from Cambridge CARES (the Cambridge Centre for Advanced Research and Education in Singapore, established by the University of Cambridge) as the host institution of the project, and SEC (the Singapore-ETH Centre, established by ETH Zürich). The team is led by principal investigators from the University of Cambridge and ETH Zürich.

This research is supported by the National Research Foundation, Prime Minister’s Office, Singapore under its external pageCampus for Research Excellence and Technological Enterprise (CREATE) programme.

Project video on YouTube: https://www.youtube.com/watch?v=ZHqEi9pEAnk&t=19s 

## Structure of project folder 

- **Augmented Importer/Exporter**: Folders starting with "impexp-" as part of CityImportAgent, contains the source code for importing city model in form of CityGML into DB and extract it in form of CityGML or KML for visualisation
- **Utils folder**: containing the python scripts which are responsible for importing semantic urban information into the DB. 
- **3dcitydb-web-map-1.9.0**: The tool comes with Graphical User Interface (GUI) with customized components for CKG demo 
- **agents folder**: containing the implementation of various agents including CityInformationAgent, CityImportAgent, CityExportAgent, etc. 
- **access_agent_setup**: containing the code for updating the routing path for access agent

## Deployment of the agents and GUI locally (On-premises)
The backend of the project is developed in Java and can be running locally inside of tomcat server. The frontend including Graphical user interface can be deployed locally within NodeJS server. 
For building and running, more details are referred to [here](https://github.com/cambridge-cares/CitiesKG/blob/233-dockerize-the-cia-agent/agents/README.md)


## Deployment of the project with docker
For deployment of the project into a new laptop, our researcher also create docker file which allows you to build docker image and run the container without any installation hassle. 
It allows you to test and try out our demo faster. For Windows OS, you would need to download Docker Desktop. For Linux OS, you can download Docker server. 

To spin up the dockers, you could run the following code in the main directory. The [docker-compose.yml](https://github.com/cambridge-cares/CitiesKG/blob/233-dockerize-the-cia-agent/docker-compose.yml) 
contains the definition of the containers / services:
```
docker-compose up 
```
After any changes in the code, you need to rebuild the docker image. Before rebuild, you need to remove the old image first: 
```
docker-compose up --build
```


## Authors and Contributors

* **Pieter Herthogs** - Project Leader
* **Shiying Li** - Software Developer - *CityExporterAgent*, *CKG Demonstration*, *CityInformationAgent*
* **Arkadiusz Chadzynski** - *CityImportAgent*
* **Ayda Grišiūtė** - *DistanceAgent*, *CityInformationAgent*



## License

This project is licensed under the XYZ  License - see the [LICENSE.md](LICENSE.md) file for details

[JPS_AWS]: https://github.com/cambridge-cares/TheWorldAvatar/tree/develop/AsynchronousWatcherService
[http://localhost:8080]: http://localhost:8080
[Preprint]: https://como.ceb.cam.ac.uk/preprints/283/