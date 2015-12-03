#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" == "master" ]; then
    mvn clean deploy --settings settings.xml -DskipTests

    SHA_SHORT=`git rev-parse --short HEAD`

    docker tag -f paradoxical/cassieq:${SHA_SHORT}_dev paradoxical/cassieq

    docker login -e ${DOCKER_EMAIL} -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}
    docker push paradoxical/cassieq:${SHA_SHORT}_dev
    docker push paradoxical/cassieq
fi