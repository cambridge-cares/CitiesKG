CEA Agent
===========

Description
-------------
The CEA agent can be used to interact with the City Energy Analyst (CEA) and the data it produces.
The CEAAgent provides three endpoints:
- http://localhost:58085/agents/cea/run - Provide an array of cityobject IRIs in the request parameters for the CEA to be run on
- http://localhost:58085/agents/cea/update - Provide data from CEA to update KG with (request sent automatically by cea/run)
- http://localhost:58085/agents/cea/query - Provide an array of cityobject IRIs in the request parameters to retrieve the energy profile for

Build Instructions
------------------
The docker image uses the world avatar maven repository ( https://maven.pkg.github.com/cambridge-cares/TheWorldAvatar/). 
You'll need to provide  your credentials (github username/personal access token) in single-word text files located:
```
./credentials/
    repo_username.txt
    repo_password.txt
```

The agent also requires a postgreSQL database for the time series client to save data in. The database used and the triplestore endpoint need to be provided in 
```
./cea-agent/src/main/resources
    timeseriesclient.properties
```

The username and password for the postgreSQL database need to be provided in:
```
./credentials/
    repo_username.txt
    repo_password.txt
```

The agent also requires the access agent to be running. The triplestore access route to be passed to the access agent needs to be provided in uri.route.local in:
```
./cea-agent/src/main/resources
    CEAAgentConfig.properties
```

To build and start the agent, you simply need to spin up a container.
In Visual Studio Code, ensure the Docker extension is installed, then right-click docker-compose.yml and select 'Compose Up'. 
Alternatively, from the command line, and in the same directory as this README, run
```
docker-compose up -d
```
The agent is reachable at "agents/cea/{option}" on localhost port 58085 where option can be run,update,query.
