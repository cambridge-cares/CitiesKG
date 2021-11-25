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

1. The build requires two dependencies, which are provided through the installation of two local jars to the .m2 repoistory. Go the main project directory "CitiesKG" (not "agents") and execute the initialization step to install the two local jars.

```
cd <main project directory>

mvn initialize
```

2. If the build is successful, you should be able to run the following to create the war package

```
mvn clean install -DskipTests
```

3. There is one dependency *blazegraph-jar-2.1.5.jar* need to be provided directly on the server, as it has been declared as following in the agents/pom.xml:
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

### Deployment (for users)

If the build is successful, you should be able to find the war artifact under ${projectDir}/${tomcatPath}/webapps/agents##0.1.0.war

Start the tomcat service by clicking on the executable *C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\Tomcat9w.exe*
and click on *Start*. After that, you can see the login page on the browser under *localhost:8080*.

As CityImportAgent will execute a python script from java program, it requires the access to the system variable.
The tomcat server needs to be configured as following:

On the logOn tab of the Tomcat properties windows, select *Log on as: Local System account* and check the box *Allow service to interact with destop*

After starting the tomcat server, you can place the agent war artifact into the directory *C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps*.

### Deployment (for developers)

Configure a Run/Debug Configuration on IntelliJ IDE with *tomcat local server*. Choose the correct parameter

If Maven has been installed and recognized by IDE correctly, on the *Deployment* tab, 
you can add an artifact and choose *agents:war exploded* . Make sure the *Application context* is */agents*

After that, you can start the tomcat server and deploy the artifact directly from the IDE

### Run the tests (for users and developers)

You can send the HTTPrequest via IDE or PostMan. You can find the examples of HTTPRequest for each agent in the directory *src\main\resources*

HTTPRequest for CityImportAgent:

```
POST http://localhost:8080/agents/import/source
Content-Type: application/json

{"directory":"C:\\tmp\\import",
  "targetURL": "http://192.168.10.111:9999/blazegraph/namespace/testdata/sparql"}
```


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
