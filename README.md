io.paradoxical.cassieq  
=====

![Build status](https://travis-ci.org/paradoxical-io/cassieq.svg?branch=master)

CassieQ is a distributed queue built on cassandra. Yes, we know, queues on cassandra are an anti-pattern,
but only when you use secondary indexes, and lots of polls and scans which CassieQ _doesn't_ do.

CassieQ provides:

- at least once delivery
- invisiblity of messages
- simple API of get/ack
- highly scaleable

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

## How does CassieQ work?

Messages are put into a queue and can be read from the queue with a visiblity timeout. This means
that if the message isn't acked within the visiblity timeout it becomes visible again.  Most other queue systems
deal with this by detecting severed connections but since angenlahir is http based and connectionless we can't rely on that.


CassieQ works with 3 pointers into a queue.

- A reader bucket pointer
- A repair bucket pointer
- An invisiblity pointer

These three pointers will be discussed in each section

In order to scale and efficiently act as a queue we need to leverage cassandra partitioning capabilities.
Queues are actually messages bucketized into a fixed size group called a bucket.   
Each message is assigned a monotonically increasing id that maps itself into a bucket. 

For example, if the bucket is size 20 and you have id 21, that maps into bucket 1 (21/20).  

Messages are always put into the bucket they correlate to, regardless if previous buckets are full.

The reader has a pointer to its active bucket (the reader buket pointer) and scans the bucket for unacked visible messages.  If the bucket is full
it tombstones the bucket indicating that the bucket is closed for processing.  If the bucket is NOT full, but all messages
in the bucket are consumed (or being processed) AND the monotonic pointer has already advanced to the next bucket,t he current 
bucket is also tombstoned. This means no more messages will ever show up in the current bucket... sort of

### Repairing delayed writes

There is a condition that you can have a delayed write. For example, assume you generate monotonic ids in this sequence:

```
Id 19
Id 20
Write 20
Write 19
```

In this scenario id 20 advances the monotonic bucket to bucket 1 (given buckets are size 20).  That means the reader tombstones
bucket 0. But what happenes to message 19? We don't want to lose it, but as far as the reader is concnered its moved onto bucket 1 and off of bucket 0.

This is where the concept of a repair worker comes into play. The repair worker's job is to slowly follow the reader and wait for tombstoned buckets. It 
has its own pointer (the repair bucket pointer)

If a bucket is tombstoned the repair worker will wait for a configured timeout for _out of order missing messages_ to appear. This means if a slightly
delayed write occurs then the repair worker will actually pick it up and then _republish it_ to the last active bucket.

This means we don't necessarily guarantee FIFO, however we do guarantee messages will appear.

### Invisibility

Now the question comes up as how to deal with invsibility of messages. For this there is a separate pointer tracking 
the last invisible pointer.  When a read comes in, we first check the invsiblity pointer to see if that message is now visible.

If it is, we can return it. If not, get the next available message.  

If the current invisible pointer is already acked then we need to find the next invisible pointer. This next invisible pointer is the first
non-acked non-visible message. If there isn't one in the current bucket, the invisibility pointer moves to the next bucket until it finds one 
or no messages exist.

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