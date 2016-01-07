#!/usr/bin/env bash

function start-graphana(){
    docker run -d \
                -p 3050:3000 \
                -v /opt/graphana:/opt/graphana \
                --name graphana \
                grafana/grafana
}

function start-graphite(){
    docker run -d \
                -p 2003:2003 \
                -p 8126:8126 \
                -p 8500:80 \
                -p 8125:8125/udp \
                --name graphite \
                hopsoft/graphite-statsd
}

start-graphana
start-graphite