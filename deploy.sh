#!/usr/bin/env bash

set -x

if [ "$TRAVIS_BRANCH" == "master" ]; then
  mvn clean deploy --settings settings.xml -DskipTests
fi