#!/usr/bin/env bash

if [ "$DOCKER_MACHINE_NAME" = "" ]; then
    log-warn "warning your DOCKER_MACHINE_NAME environment variable was not set. have you run: echo \"\$(docker-machine env <MACHINE-NAME)\"?"
    DOCKER_MACHINE_NAME=dev
fi

if [ "$DEBUG_JAVA" = "" ]; then
    DEBUG_JAVA=false
fi

if [ "$1" = "-debug" ]; then
    DEBUG_JAVA=true
fi

CWD=$(dirname $0)
if [ `basename $(pwd)` = 'scripts' ]; then
    cd ../
else
    cd `dirname $CWD`
fi

mkdir -p `pwd`/logs/core

GIT_SHA=`git rev-parse --short HEAD`

image="paradoxical/cassieq:${GIT_SHA}_dev"

echo ${image}
docker run -it \
    -e HOST_IPADDR=`docker-machine ip $DOCKER_MACHINE_NAME` \
    -e ENV_CONF="" \
    -p 8080:8080 \
    -p 8081:8081 \
    -p 1044:1044 \
    -p 1898:1898 \
    -v `pwd`/logs/core:/data/logs \
    ${image} "$@"
