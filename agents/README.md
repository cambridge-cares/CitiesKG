# Semantic 3D City Agents

The Semantic City Agents is an intelligent automation for Dynamic Geospatial Knowledge Graphs. 
It contains 3 agents: CityImportAgent, CityExportAgent and DistanceAgent. All 3 agnets works with the semantic cities database - [Cities Knowledge Graphs](http://www.theworldavatar.com:83/citieskg/#query).

System requirements
-------------------
* Windows 10
* Java JRE or JDK >= 1.8
* IntelliJ IDE Ultimate version (for the developers)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* Java JRE or JDK >= 1.8
* Tomcat 9 
* Maven build system

If Java and Maven are installed correctly and set as system variables, you should be able to check the version information using the following commands on the IDE terminal or CMD window.
```
java -version
```
You will see this:
```
java version "1.8.0_291"
Java(TM) SE Runtime Environment (build 1.8.0_291-b10)
Java HotSpot(TM) 64-Bit Server VM (build 25.291-b10, mixed mode)
```

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

### Install and Build

1. The build requires two dependencies, which are provided through the installation of two local jars to the local .m2 repoistory:

```
cd <project directory>

mvn initialize
```

2. If the build is successful, you should be able to run the following to create the war package

```
mvn clean install -DskipTests
```

If the build is successful, you should be able to find the war package under ${projectDir}/${tomcatPath}/webapps/agents##0.1.0.war

## Running the tests

Explain how to run the automated tests for this system

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

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
