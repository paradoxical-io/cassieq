io.paradoxical.cassieq  
=====

![Build status](https://travis-ci.org/paradoxical-io/cassieq.svg?branch=master)

CassieQ is a distributed queue built on cassandra. Yes, we know, queues on cassandra are an anti-pattern,
but CassieQ leverages lightweight transactions and message bucketizing to avoid issues with queues based off deletes.

CassieQ provides:

- at least once delivery
- invisiblity of messages
- simple API of get/ack
- highly scaleable
- authentication
- queue statistics (via graphite)

## To run

```
docker run -it \
    -e KEYSPACE="" \
    -e CONTACT_POINTS="" \
    -e USERNAME="" \
    -e PASSWORD="" \
    paradoxical/cassieq
```

If you don't want to use environment variables, you can mount a volume to `/data/conf` and provide your own
yaml

## Bootstrapping required tables

```
docker run -it \
    -e KEYSPACE="" \
        -e CONTACT_POINTS="" \
        -e USERNAME="" \
        -e PASSWORD="" \
    paradoxical/cassieq bootstrap
```

This will build out the required tables in your keyspace. 

If your user has permissions to create a keyspace you can run

```
docker run -it \
    -e KEYSPACE="" \
        -e CONTACT_POINTS="" \
        -e USERNAME="" \
        -e PASSWORD="" \
    paradoxical/cassieq bootstrap -createKeyspace
```

## Available env vars

Nested properties are only enabled if the parent is enabled

- CLUSTER_NAME
- KEYSPACE
- CONTACT_POINTS
- AUTH_PROVIDER - defaults to "plainText"
- USERNAME
- PASSWORD
- CONSISTENCY_LEVEL - defaults to LOCAL_QUOURUM
- CASSANDRA_PORT - defaults to 9042
- USE_SSL - "true" or "false"
  - SSL_PORT - defaults to 9043
  - DATA_CENTER - uses this data center as a load balancing policy
- USE_METRICS_GRAPHITE - "true" or "false"
  - GRAPHITE_URL 
  - GRAPHITE_PREFIX 
- LOGSTASH_CUSTOM_APP_NAME

## Why make a queue on cassandra?

Cassandra is a great datastore that is massively horizontally scaleable. It also exists at a lot of organizations
already.  Being able to use a horizontally scaleable data store means you can ingest incredible amounts of messages.
 
Also by providing a simple docker container that houses an REST web api, you can scale out the queue by tossing 
more docker instances at your cassandra queue.

CassieQ is fully encapsulated and only needs to know your cassandra information. Future work will include 
passing the cassandra cluster credentials and connection information via docker env vars and auto populating
the tracking tables for queues.


## Admin panel

The admin API is available at `host:8081/admin`

## API

We have bundled a java client to talk to a simple rest api. The api supports

- Queue create
- Put a message
- Get a message
- Ack a message

Getting a message gives you a pop reciept that encodes the message index AND its version. This means that you can prevent multiple ackers of a message
and do conditional atomic actions based on that message version.

Execute cassieq local
====

```
./scripts/run-core.sh
```