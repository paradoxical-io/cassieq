io.paradoxical.cassieq  
=====

![Build status](https://travis-ci.org/paradoxical-io/cassieq.svg?branch=master)

CassieQ is a distributed queue built on cassandra. Yes, we know, queues on cassandra are an anti-pattern,
but CassieQ leverages lightweight transactions and message bucketizing to avoid issues with queues based off deletes.

CassieQ provides:

- At least once delivery
    - You may get a message more than once. It is important for the client consumer to be able to handle this scenario
- Invisiblity of messages
    - When a message is consumed, you can say how long it should not be re-visible for. When it becomes revisible (after that time)
   it can be redelivered to another consumer. You can also specify initial invisibility when putting messages in, so they won't be available
   for a consumer until that time.
- Simple API 
    - A simple REST api. Java client available
- Highly scaleable due to backing of cassandra
- Durable
    - If the API accepts a message, it is persisted and will not be lost.  
- Secured with granularty by account and specific actions 
- Queue statistics (via graphite)
    - The only native stat is queue size, however granular metrics regarding consume/ack/publish rates are all published as graphite metrics
- DLQ support
    - DLQ's can be chained for more complex topologies
- Max message delivery
    - Avoid poison messages with max delivery counts

## Get started

To get started go see our [wiki](https://github.com/paradoxical-io/cassieq/wiki)

Execute cassieq local
====

```
./scripts/run-core.sh
``` 
