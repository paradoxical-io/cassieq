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

The admin API is available at `host:8081/admin`. You will need to access the admin panel to administer accounts 
and get account access URLs for actions on endpoints.

## API

We have bundled a java client to talk to a simple rest api. The api supports

- Queue create
- Delete queue
- Put a message
- Get a message
- Ack a message
- Update a message
- Create account
- Delete account
- Create permissions

Getting a message gives you a pop reciept that encodes the message index AND its version. This means that you can prevent multiple ackers of a message
and do conditional atomic actions based on that message version.

## Recipes

### Requeuing messages to defer to another consumer

In general CassieQ is very similiar to normal queues with `put`, and `ack` semantics of messages. However, CassieQ is slightly different
 in the semantics of a `nack` with requeue, i.e. a `defer` to another consumer.
 
In CassieQ terms a `nack` with `requeue` (i.e. *send this message to someone else*) is done by updating the message invisibility time to 0.

The original pop reciept is now invalid, and only the person who claims the new one will be able to work on it, effectively asking for it to be safely requeued.

### Heartbeat long processing messages

If you however, have claimed a message for X time, and want to continually work on the message and extend the message lease, you can also update the message with 
an invisiblity time in the future. This gives you the concept of a message heartbeat.  Each time you do an update, you'll be given a new pop reciept, which represents 
that version of the message. You must use the new reciept to ack the message since the previous message version is now invalid.

## Authentication

CassieQ has three forms of authentication
 
- Global account access via key
- Endpoint signing by key
- Granular access via permission claims by account
 
### Claims

Claims by account is the *preferred* method. To generate a claim for a user, you can create a named account key for that group (such as "ui-members") and create
permission based url params for them. This may look of the form:

```
?auth=puag&sig=NygRs9GBh9n_i2s7KTMof0us-RXm5nt3RnlWKb3N15A
```

Which can be appended to any API url.  The auth is of a simple form (concatenation of single character based permissions) which in this scenario indicates

```
PutMessage (p)
UpdateMessage (u)
AckMessage (a)
GetQueueInformation (g)
```

For a full list of granular permissions go to the admin panel permissions api.  

The advantage to transparent signature based auth is that you can pass the url to multiple teams and share common auth without leaking secrets.  To revoke access, 
delete the key that was used to generate the signature.

### Global

This generates global access by account.

Global account access is via header parameters. Add your account key as the authorization header with the value

```
Key: <your key>
```

This is not the recommended authentication case but is supported and is currently the only way to experiment with the 
swagger API.  Put your access key into the swagger api key section for access.

### Endpoint singing by key

This also provides global access by account but doesn't expose your access key publicly.  

Generate auth tokens by signing using Hmac256 the following (newlines using `\n`)

```
AccountName
RequestMethod
/RequestPath
```

For example

```
TestAccount
GET
/api/v1/accounts/TestAccount/queues/foo/messages/next
```

And taking this signed value and putting it in the auth header as 

```
Signed: <signature>
```

Execute cassieq local
====

```
./scripts/run-core.sh
``` 
