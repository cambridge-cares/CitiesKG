# Semantic 3D City Agents

The Semantic City Agents is an intelligent automation for Dynamic Geospatial Knowledge Graphs. 
It contains 3 agents: CityImportAgent, CityExportAgent and DistanceAgent. All 3 agnets works with the semantic cities database - [Cities Knowledge Graphs](http://www.theworldavatar.com:83/citieskg/#query).

## System requirements

* Windows 10 or Mac
* Java JRE or JDK >= 1.8
* IntelliJ IDE Ultimate version (for the developers)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system. 
These detailed instructions describes the setup on a windows machine, for Mac OS it is equivalent.  


### Prerequisites

* Java JRE or JDK >= 1.8
* [Tomcat 9](https://www.liquidweb.com/kb/installing-tomcat-9-on-windows/)
* [Maven](https://maven.apache.org/) - Dependency Management
* Python 3

JDK or JRE will need to be installed on the Windows Server before you can configure Tomcat 9 on the server.
If Java and Maven are installed correctly and set as [environment variables](https://docs.oracle.com/en/database/oracle/machine-learning/oml4r/1.5.1/oread/creating-and-modifying-environment-variables-on-windows.html#GUID-DD6F9982-60D5-48F6-8270-A27EC53807D0), you should be able to check the version information using the following commands on the IDE terminal or CMD window.

In order to use those commands via CLI (Command Line Interface), Maven, Java, Tomcat and Python should be added to *Path* in system variable. And additionally JAVA_HOME should also be configured.


For checking Java version, type this command on CMD:
```
java -version
```
You will see this:
```
java version "1.8.0_291"
Java(TM) SE Runtime Environment (build 1.8.0_291-b10)
Java HotSpot(TM) 64-Bit Server VM (build 25.291-b10, mixed mode)
```
For checking Maven version, type this command on CMD:
```
mvn -version
```
You will see this for example:
```
Apache Maven 3.8.1 (05c21c65bdfed0f71a2f2ada8b84da59348c4c5d)
Maven home: <installation-path>\apache-maven-3.8.1\bin\..
Java version: 1.8.0_291, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk1.8.0_291\jre
Default locale: en_SG, platform encoding: Cp1252
OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"
```


For [Tomcat 9]((https://www.liquidweb.com/kb/installing-tomcat-9-on-windows/)), check the link and learn how to deploy the artifact to tomcat server.

After the tomcat installation, you can find the folder "C:\Program Files\Apache Software Foundation\Tomcat 9.0". 
This folder is by default not accessible, in order to make it executable, you need to click on the folder and open the folder to view once.  



### Install and Build


1. The build requires two dependencies, which are provided through the installation of two local jars to the .m2 repository. Go the main project directory "CitiesKG" (not "agents") and execute the initialization step to install the two local jars.

```
cd <main project directory>

mvn initialize
```

2. If the initialization is done successful, you should be able to run the following to create the war package

```
mvn clean install -DskipTests
```

In case building the .war file fails due to the missing `JPS_AWS.jar`, please build this locally first via running the following command within the [JPS_AWS] repository:
```
mvn clean install -DskipTests
```

3. There is one dependency `blazegraph-jar-2.1.5.jar` which needs to be provided directly on the server, as it has been declared as following in the agents/pom.xml:
```
    <dependency>
      <groupId>com.blazegraph</groupId>
      <artifactId>blazegraph-jar</artifactId>
      <version>2.1.5</version>
      <scope>provided</scope>
    </dependency>

```

This dependency can be either found in your .m2 repository or downloaded from this [website](https://search.maven.org/search?q=g:com.blazegraph%20AND%20a:blazegraph-jar&core=gav).
Make sure that you download the correct version 2.1.5, otherwise the POM file of the agents need to be adjusted.

After you get the correct artifact *blazegraph-jar-2.1.5.jar*, you can place it into the folder on your server in *C:\Program Files\Apache Software Foundation\Tomcat 9.0\lib*, 
this folder contains all the libraries provided by the server. 

If this step has not been done, the CityImportAgent might not work. 

4. Additional dependencies used by agents are JPS_BASE_LIB and JPS_AWS are provided by the *TheWorldAvatar* (TWA) project. Both dependencies need to be compiled and installed to the .m2 repository.

Clone the TWA project from the [TWA repository](https://github.com/cambridge-cares/TheWorldAvatar) and checkout to develop branch, commit 610cfcc8492f65821039a04795625a783e6050e6.
The most recent master and develop branches have refactored class names for KnowledgeBaseClientInterface and RemoteKnowledgeBaseClient, and will not work with these agents.

Run the command *mvn clean install -DskipTests* on the corresponding directories where the POM file is found.
* JPS_BASE_LIB project can be found in the [JPS_BASE_LIB directory](https://github.com/cambridge-cares/TheWorldAvatar/tree/develop/JPS_BASE_LIB) from the *master* branch (stable version)
* JPS_AWS project can be found in the [AsynchronousWatcherService](https://github.com/cambridge-cares/TheWorldAvatar/tree/develop/AsynchronousWatcherService) directory from the *develop* branch. Before run the install command, please comment out the JPS_BASE dependency in the POM file. 


### Deployment (for users)

If the build is successful, you should be able to find the war artifact under ${projectDir}/${tomcatPath}/webapps/agents##0.1.0.war

Start the tomcat service by clicking on the executable *C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\Tomcat9w.exe*
and click on *Start*. After that, you can see the startup page on the browser under [http://localhost:8080].

As CityImportAgent will execute a python script from java program, it requires the access to the system variable.
The tomcat server needs to be configured as following:

On the logOn tab of the Tomcat properties windows, select *Log on as: Local System account* and check the box *Allow service to interact with Desktop*

After starting the tomcat server, you can place the agent .war artifact into the directory *C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps*.

### Deployment (for developers)

Import the `agents` module as Maven project into IntelliJ IDE. Configure a Run/Debug Configuration on IntelliJ IDE with *tomcat local server* and choose the correct parameter.

If Maven has been installed and recognized by IDE correctly, on the *Deployment* tab, 
you can add an artifact and choose *agents:war exploded* . Make sure the *Application context* is */agents*

After that, you can start the tomcat server and deploy the artifact directly from the IDE.

### Run the tests (for users and developers)

You can send the HTTP request via IDE or PostMan. You can find examples HTTP requests for each agent in the directory *src\main\resources*

Please make sure that the specified target namespace is available, i.e. for local deployment/testing Blazegraph needs to be started and the specified namespace potentially to be created beforehand.

HTTPRequest for CityImportAgent:
```
POST http://localhost:8080/agents/import/source
Content-Type: application/json

{"directory":"C:\\tmp\\import",
  "targetURL": "http://192.168.10.111:9999/blazegraph/namespace/testdata/sparql"}
```

Executing this request, will create a directory at the specified location. When placing any `.gml` file into this folder, this file will automatically be imported into the specified triple store. For further details, please see the "Semantic 3D City Agents - an intelligent automation for Dynamic Geospatial Knowledge Graphs" [preprint].

Please note that splitting of large files into smaller chunks to improve performance will not work if the `.gml` file contains the `core:` namespace tag in front of CityGML features. Please remove those manually beforehand.

## 3DCityDB-Web-Map-Client

We use the 3DCityDB-Web-Map-Client to visualise *CityExportAgent* exported .kml data. We extended original code with new functions and interface elements for displaying analytical capabilities of the *DistanceAgent*. The extended Web-Map-client version can be found in CitiesKG project directory `/CitiesKG/3dcitydb-web-map-1.9.0/`.

### Documentation

A complete and comprehensive documentation on the 3DCityDB-Web-Map-Client is available online here (https://github.com/3dcitydb/3dcitydb-web-map) and here (https://3dcitydb-docs.readthedocs.io/en/release-v4.2.3/webmap/index.html).

 ### Getting Started

In order to use the extended 3DCityDB-Web-Map-Client for city agents make sure that:

* your browser support WebGL (visit http://get.webgl.org/ for checking it).
* open source JavaScript runtime environment Node.js is installed on your machine (visit https://nodejs.org/en/ to download the latest version). 
* the extended web-map-client does not have node_modules folder thus, download original web-map-client via the following GitHub link (https://github.com/3dcitydb/3dcitydb-web-map/releases) and copy node_modules folder in `/CitiesKG/3dcitydb-web-map-1.9.0/`.

To run the web-map-client, in a shell environment navigate to the folder where *server.js* file is located `/CitiesKG/3dcitydb-web-map-1.9.0/` and simply run the following command to launch the server:

```
 node server.js
```

The web-map-client is now available via the URL (http://localhost:8080/3dwebclient/index.html). Place the .kml file in `/CitiesKG/3dcitydb-web-map-1.9.0/3dwebclient/` and add the web link of the .kml file in `URL(*)` input field of the web-map-client Toolbox widget. In the input field `Name(*)`, a proper layer name must be specified as well. After clicking AddLayer, the .kml file will be visualised in the web-map-client.


## Contributing

Please read [CONTRIBUTING.md](https://www.theworldavatar.com/citieskg/contributing/) for details on our code of conduct.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the (https://www.theworldavatar.com/citieskg/releases).

## Authors and Contributors

* **Arkadiusz Chadzynski** - *CityImportAgent*
* **Shiying Li** - *CityExporterAgent*
* **Ayda Grišiūtė** - *DistanceAgent*

See also the list of [contributors](https://www.theworldavatar.com/citieskg/contributors) who participated in this project.

## License

This project is licensed under the XYZ  License - see the [LICENSE.md](LICENSE.md) file for details


[JPS_AWS]: https://github.com/cambridge-cares/TheWorldAvatar/tree/develop/AsynchronousWatcherService
[http://localhost:8080]: http://localhost:8080
[Preprint]: https://como.ceb.cam.ac.uk/preprints/283/
