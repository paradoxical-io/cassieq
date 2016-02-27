#!/usr/bin/env bash

if [ "$DOCKER_MACHINE_NAME" = "" ]; then
    log-warn "warning your DOCKER_MACHINE_NAME environment variable was not set. have you run: echo \"\$(docker-machine env <MACHINE-NAME)\"?"
    DOCKER_MACHINE_NAME=dev
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

VERSION="1.2"
JARNAME="cassandra.loader-${VERSION}-runner.jar"
MAVEN_URL="https://repo1.maven.org/maven2/io/paradoxical/cassandra.loader/${VERSION}/${JARNAME}"
RUNNER_LOCATION="$DIR/$JARNAME"

if [ ! -f $RUNNER_LOCATION ]; then
    wget -O $RUNNER_LOCATION $MAVEN_URL;
fi

java -jar ${RUNNER_LOCATION} \
           -f db/scripts \
           -ip `docker-machine ip $DOCKER_MACHINE_NAME` \
           -k cassieq \
           -p 9042 \
           -createKeyspace -recreateDatabase