# flink-reactive-mode-k8s-demo

## Prepare Minikube

```

# build image
docker build -t rmetzger/flink:1.13.0-reactive-timeouts-d382d0429ce544fde89aa80b2b0e6f82f91eb991-topspeed .

# publish image
docker push rmetzger/flink:1.13.0-reactive-timeouts-d382d0429ce544fde89aa80b2b0e6f82f91eb991-topspeed


brew install minikube

# if existing install is broken:
brew unlink minikube
brew link minikube

# start minikube
minikube start


# some prep
minikube ssh 'sudo ip link set docker0 promisc on'

# provide job jar
minikube mount `pwd`/TopSpeedWindowing.jar:/flink-job


# launch
kubectl apply -f flink-configuration-configmap.yaml
kubectl apply -f jobmanager-application.yaml
kubectl apply -f jobmanager-rest-service.yaml
kubectl apply -f jobmanager-service.yaml
kubectl apply -f taskmanager-job-deployment.yaml

# remove
kubectl delete -f flink-configuration-configmap.yaml
kubectl delete -f jobmanager-application.yaml
kubectl delete -f jobmanager-rest-service.yaml
kubectl delete -f jobmanager-service.yaml
kubectl delete -f taskmanager-job-deployment.yaml

# connect
kubectl proxy

# scale
kubectl scale --replicas=3 deployments/flink-taskmanager
```