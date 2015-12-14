#! /bin/bash

docker stop cassandra

docker rm cassandra

db=/srv/cassandra-db


#    -v $db:/var/lib/cassandra \

docker run -d \
    --name cassandra \
    -p 9042:9042 \
    -p 9160:9160 \
    -e CASSANDRA_CLUSTER_NAME=local \
    -e CASSANDRA_DC=local \
    cassandra:2.2.4