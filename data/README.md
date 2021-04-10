# graph-with-quarkus data

This project uses NEO4J.

## Running the the backend

```shell script
docker run --publish=7474:7474 --publish=7687:7687 -e 'NEO4J_AUTH=neo4j/secret' neo4j:4.0.0
```
