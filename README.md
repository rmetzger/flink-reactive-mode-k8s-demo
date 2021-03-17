# flink-reactive-mode-k8s-demo


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

# optional dashboard
minikube dashboard

# install metrics server (needed for autoscaler)
minikube addons enable metrics-server

kubectl create namespace reactive
kubectl config set-context --current --namespace=reactive

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

# connect (doesn't work?!)
kubectl proxy

# connect to Flink UI
kubectl port-forward flink-jobmanager-rp4zv 8081

# scale manually
kubectl scale --replicas=3 deployments/flink-taskmanager

# probably based on: https://www.magalix.com/blog/kafka-on-kubernetes-and-deploying-best-practice
# start zookeeper
kubectl apply -f zookeeper-service.yaml
kubectl apply -f zookeeper-deployment.yaml

# start kafka
kubectl apply -f kafka-service.yaml
kubectl apply -f kafka-deployment.yaml

# launch a container for running the data generator
kubectl run workbench --image=ubuntu:21.04 -- sleep infinity

# connect to workbench
kubectl exec --stdin --tty workbench -- bash

# prep
apt update
apt install -y maven git htop nano iputils-ping wget net-tools
git clone https://github.com/rmetzger/flink-reactive-mode-k8s-demo.git
mvn clean install

# run data generator
mvn exec:java -Dexec.mainClass="org.apache.flink.DataGen" -Dexec.args="--topic topic --bootstrap kafka-service:9092"

# delete workbench
kubectl delete pod workbench

# make kafka available locally:
kubectl port-forward kafka-broker0-78fb8799f7-twdf6 9092:9092


# scale automatically
kubectl autoscale deployment flink-taskmanager --min=1 --max=6 --cpu-percent=30

# remove autoscaler
kubectl delete horizontalpodautoscalers flink-taskmanager
```