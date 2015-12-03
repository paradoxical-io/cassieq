#!/usr/bin/env bash

if ( type cassandra-loader &> /dev/null ); then
	if [ "$DOCKER_MACHINE_NAME" = "" ]; then
	    log-warn "warning your DOCKER_MACHINE_NAME environment variable was not set. have you run: echo \"\$(docker-machine env <MACHINE-NAME)\"?"
	    DOCKER_MACHINE_NAME=dev
	fi

	cassandra-loader -f db/scripts -ip `docker-machine ip $DOCKER_MACHINE_NAME` -k cassieq -p 9042 -createKeyspace -recreateDatabase

else
	echo "Error, did you not dot source this file?"
	echo "Example:"
	echo "    . $0"
fi