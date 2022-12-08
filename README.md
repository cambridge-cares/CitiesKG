3D City Database Importer/Exporter
==================================

The 3D City Database Importer/Exporter is a Java based front-end for the [3D City Database](https://github.com/3dcitydb/3dcitydb). It allows for high-performance loading and extracting 3D city model data.

* Full support for CityGML versions 2.0.0 and 1.0.0
* Support for CityGML Application Domain Extensions (ADEs) through
  software extensions
* Support for Oracle Locator/Spatial and PostgreSQL/PostGIS
* Reading/writing CityGML instance documents of arbitrary file size
* Export of KML/COLLADA/glTF models including tiling schemas for 
  visualization and interactive exploration of large city models
  in Digital Earth Browsers, 3D GIS, and computer graphics software
* Generic KML information balloons
* Export of thematic object data into tables. Supported data formats are
  CSV and Microsoft Excel
* Resolving and preservation of forward and backwards XLinks in 
  CityGML datasets
* Full support of 3D Coordinate Reference Systems (CRS) and 3D 
  coordinate transformations; support for user-defined CRS 
* Coordinate transformations for CityGML exports
* XML validation of CityGML instance documents
* Multithreaded programming facilitating high-performance CityGML 
  processing

The 3D City Database Importer/Exporter comes with both a Graphical User Interface (GUI) and a Command Line Interface (CLI). The CLI 
allows for employing the tool in batch processing workflows and third party applications.

License
-------
The 3D City Database Importer/Exporter is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0). See the `LICENSE` file for more details.

Note that releases of the software before version 3.3.0 continue to be licensed under GNU LGPL 3.0. To request a previous release of the 3D City Database Importer/Exporter under Apache License 2.0 create a GitHub issue.

Latest release
--------------
The latest stable release of the 3D City Database Importer/Exporter is 4.2.3.

Download a Java-based executable installer for the software [here](https://github.com/3dcitydb/importer-exporter/releases/download/v4.2.3/3DCityDB-Importer-Exporter-4.2.3-Setup.jar). Previous
 releases are available from the [releases section](https://github.com/3dcitydb/importer-exporter/releases).

System requirements
-------------------
* Java JRE or JDK >= 1.8
* [3D City Database](https://github.com/3dcitydb/3dcitydb) on
  - Oracle DBMS >= 10G R2 with Spatial or Locator option
  - PostgreSQL DBMS >= 9.6 with PostGIS extension >= 2.3
  
The 3D City Database Importer/Exporter can be run on any platform providing appropriate Java support. 

Documentation and literature
----------------------------
A complete and comprehensive documentation on the 3D City Database and the Importer/Exporter tool is available [online](https://3dcitydb-docs.readthedocs.io/en/release-v4.2.3/index.html).

An Open Access paper on the 3DCityDB has been published in the International Journal on Open Geospatial Data, Software and Standards 3 (5), 2018: [Z. Yao, C. Nagel, F. Kunde, G. Hudra, P. Willkomm, A. Donaubauer, T. Adolphi, T. H. Kolbe: 3DCityDB - a 3D geodatabase solution for the management, analysis, and visualization of semantic 3D city models based on CityGML](https://doi.org/10.1186/s40965-018-0046-7). Please use this reference when citing the 3DCityDB project.

Contributing
------------
* To file bugs found in the software create a GitHub issue.
* To contribute code for fixing filed issues create a pull request with the issue id.
* To propose a new feature create a GitHub issue and open a discussion.

Installing and running
----------------------
The easiest way to get the Importer/Exporter running on your computer is to download an installer from the [releases section](https://github.com/3dcitydb/importer-exporter/releases). The installers are named `3DCityDB-Importer-Exporter-<version>-Setup.jar` and are packaged as executable JAR file. So double-clicking the JAR file should run the installer. The installer will guide you through the steps of the installation process.

After installation, start scripts are available in the `bin` subfolder of the installation directory. During setup you can additionally choose to create shortcuts on your desktop and in the start menu of your preferred OS.

Simply execute the start script suitable for your platform:
   - `3DCityDB-Importer-Exporter.bat` (Microsoft Windows family)
   - `3DCityDB-Importer-Exporter` (UNIX/Linux family, macOS)

On most platforms, double-clicking the start script or its shortcut launches the application.

Building
--------
The Importer/Exporter uses [Gradle](https://gradle.org/) as build system. To build the application from source, clone the repository to your local machine and run the following command from the root of the repository. 

    > gradlew installDist
    
The script automatically downloads all required dependencies for building and running the Importer/Exporter. So make sure you are connected to the internet. The build process runs on all major operating systems and only requires a Java 8 JDK or higher to run.

In case building fails (e.g. due to "Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain"), try downgrading gradle to version 6.9.1. Therefore, install gradle 6.9.1 following this [guide] and subsequently run:

    > gradle wrapper
    > gradlew installDist

If the build was successful, you will find the Importer/Exporter package under `impexp-client/build/install`. To launch the application, simply use the starter scripts in the `bin` subfolder.

You may also choose to build an installer for the Importer/Exporter with the following command.

    > gradlew buildInstaller

The installer package will be available under `impexp-client/build/distributions`.

GDAL Library Java bindings
--------
GDAL version: 3.4.0
Java bindings (https://gdal.org/api/java/index.html)
Java bindings for **Windows** (https://trac.osgeo.org/gdal/wiki/GdalOgrInJavaBuildInstructions)
1. Install the GDAL core components from this link: https://www.gisinternals.com/query.html?content=filelist&file=release-1916-x64-gdal-3-4-1-mapserver-7-6-4.zip
2. Change the environment variables:
3. Append to PATH  - C:\Program Files\GDALGDAL_DATA - C:\Program Files\GDAL\gdal-data
   GDAL_DRIVER_PATH  - C:\Program Files\GDAL\gdalplugins
   PROJ_LIB: C:\Program Files\GDAL\projlib
   Change PROJ_LIB to the corresponding GDAL folder
4. Test to see if GDAL is installed by opening Command Prompt and type in:   gdalinfo --version
5. Copy and paste the following dlls from C:\Program Files\GDAL into your Java
     JDK bin folder (example: C:\Program Files\Java\jdk1.8.0_171\bin)
     In some version, if you don’t find <gdalalljni.dll>, you could copy and paste the following dlls
     gdalconstjni.dll
     gdaljni.dll
     ogrjni.dll
     osrjni.dll
6. Add the gdal java binding into the configuration file of your program.

Java bindings for **Mac** (https://trac.osgeo.org/gdal/wiki/GdalOgrInJavaBuildInstructionsUnix)
1. Install the Java JDK, SWIG and Ant compilation tools
2. Download GDAL source code from https://gdal.org/download.html
3. Compile GDAL source code (The following is an example of compile, pay attention to the path of your **gdal, Java and JDK version**):
   cd gdal
   ./configure --with-threads --disable-static --without-grass --with-jasper=/usr/local/lib --with-libtiff=/usr/local/lib --with-jpeg=/usr/local/lib --with-gif=/usr/local/lib --with-png=/usr/local/lib --with-geotiff=/usr/local/lib --with-pcraster=internal --with-geos=/usr/local/lib --with-static-proj4=/usr/local/lib --with-expat=/usr/local/lib --with-curl=/usr/local/lib --with-netcdf=/usr/local/lib --with-hdf5=/usr/local/lib --with-opencl --with-libz=internal --without-python --with-java --with-jvm-lib=/Library/Java/JavaVirtualMachines/openjdk-**x.x.x**.jdk/Contents/Home
   make
   sudo make install
4. Use make install to dynamic link library _libgdalalljni.20.dylib_ and _libgdalalljni.dylib_, copy to /usr/local/lib:
   cd swig/java
   make CFLAGS="-I/Library/Java/JavaVirtualMachines/openjdk-**x.x.x**.jdk/Contents/Home/include -I//Library/Java/JavaVirtualMachines/openjdk-**x.x.x**.jdk/Contents/Home/include/darwin"
   sudo make install
5. _make_ command will generate what we need **gdal.jar**, which is a dependency package we need for Java development and must be added to the project.
6. Copied _libgdalalljni.20.dylib_ and _libgdalalljni.dylib_ to /Library/Java/Extensions

Cooperation partners and supporters  
-----------------------------------

The 3D City Database Importer/Exporter has been developed by and with the support from the following cooperation partners:

* [Chair of Geoinformatics, Technical University of Munich](https://www.gis.bgu.tum.de/)
* [virtualcitySYSTEMS GmbH, Berlin](http://www.virtualcitysystems.de/)
* [M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen](http://www.moss.de/)

More information
----------------
[OGC CityGML](http://www.opengeospatial.org/standards/citygml) is an open data model and XML-based format for the storage and exchange of semantic 3D city models. It is an application schema for the [Geography Markup Language version 3.1.1 (GML3)](http://www.opengeospatial.org/standards/gml), the extendible international standard for spatial data exchange issued by the Open Geospatial Consortium (OGC) and the ISO TC211. The aim of the development of CityGML is to reach a common definition of the basic entities, attributes, and relations of a 3D city model.

CityGML is an international OGC standard and can be used free of charge.

# Project Title

Cities Knowledge Graph

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

```
Give examples
```

### Installing

A step by step series of examples that tell you how to get a development env running

Say what the step will be

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo

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

## Authors

* **Arkadiusz Chadzynski** - *Initial work*
* **Ненад Крџавац** - *Semantic Web expertise*
* **Pieter Herthogs** - *Domain expertise*

See also the list of [contributors](https://www.theworldavatar.com/citieskg/contributors) who participated in this project.

## License

This project is licensed under the XYZ  License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Hat tip to anyone whose code was used
* A great city is not to be confounded with a populous one. *- Aristotle*
* This project is funded by the National Research Foundation (NRF), Prime Ministers Office, Singapore under its Campus for Research Excellence and Technological Enterprise (CREATE) programme. 



[guide]: https://gradle.org/install/