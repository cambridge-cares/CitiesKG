Introduction 
=============
While the source code of Cities Knowledge Graph (CKG) project is ready to use, we also provide the dockerfile for deploying the agents, 
which allows the user to easily run the CKG project anywhere without go through the complicated setup process.

Prerequisties
-------------
The system is tested with windows system 
- Install Docker Desktop with WSL2 []  
- Git clone the CKG project
After a fresh pulling from the CKG repository, few preparation steps need to be done:
- Adding credential for maven repository: Add .m2 folder
- Download the plot data for the frontend visualization
- Add node_modules to 3dcitydb-web-map-1.9.0 directory


Initialize the environment
--------------------------

Then use [initialize.sh](the link to source code) to spin up the container for the first time and update the routing information in the access agent
[docker-compose.yml](the link to source code)

Deploying local Agents
----------------------



Building the CKG agents
-----------------------
