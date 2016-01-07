#!/bin/bash

url="http://localhost:8080/api/v1"

queueName="test"

function produce(){
  while [ 1 ]; do;
    now=`date`
    http POST "$url/queues/$queueName/messages" data="$now"
  done
}

function consume(){
    while [ 1 ]; do;
        popReceipt=`http -b GET "$url/queues/$queueName/messages/next" | jq ".popReceipt" | sed -e "s#\"##g"`

        http DELETE "$url/queues/$queueName/messages?popReceipt=$popReceipt"
    done
}