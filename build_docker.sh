#!/bin/bash

cd reactive-mode-demo-jobs
mvn clean install
cd ..

docker build -t rmetzger/flink:1.13.0-reactive-demo .
docker push rmetzger/flink:1.13.0-reactive-demo
