#!/bin/bash

docker build -t rmetzger/flink:1.13.0-reactive-timeouts-v2-57d9b95fac95a649bd8295c294b77aeee5df12fc-kafka .
docker push rmetzger/flink:1.13.0-reactive-timeouts-v2-57d9b95fac95a649bd8295c294b77aeee5df12fc-kafka
