#!/usr/bin/env bash

set -x

if [ "$TRAVIS_BRANCH" == "master" ]; then
  mvn clean deploy --settings settings.xml -DskipTests

  docker tag -f paradoxical/cassieq:${TRAVIS_COMMIT}_dev paradoxical/cassieq

  docker login -e ${DOCKER_EMAIL} -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}
  docker push paradoxical/cassieq:${TRAVIS_COMMIT}_dev
  docker push paradoxical/cassieq
fi