#!/bin/bash

kubectl delete --wait=true -f zookeeper-deployment.yaml

kubectl delete --wait=true -f kafka-service.yaml
kubectl delete --wait=true -f kafka-deployment.yaml

kubectl delete --wait=true -f flink-configuration-configmap.yaml
kubectl delete --wait=true -f jobmanager-application.yaml
kubectl delete --wait=true -f jobmanager-rest-service.yaml
kubectl delete --wait=true -f jobmanager-service.yaml
kubectl delete --wait=true -f taskmanager-job-deployment.yaml

kubectl delete --wait=true horizontalpodautoscalers flink-taskmanager
