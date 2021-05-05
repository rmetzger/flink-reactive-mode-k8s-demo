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

# optional dashboard on Minikube
minikube dashboard
# dashboard on real cluster
kubectl proxy
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
kubectl -n kubernetes-dashboard describe secret $(kubectl -n kubernetes-dashboard get secret | grep admin-user | awk '{print $1}')


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
mvn exec:java -Dexec.mainClass="org.apache.flink.DataGen" -Dexec.args="topic 1 kafka-service:9092"

# delete workbench
kubectl delete pod workbench

# make kafka available locally:
kubectl port-forward kafka-broker0-78fb8799f7-twdf6 9092:9092


# scale automatically
kubectl autoscale deployment flink-taskmanager --min=1 --max=15 --cpu-percent=35

# remove autoscaler
kubectl delete horizontalpodautoscalers flink-taskmanager


# prometheus
kubectl port-forward prometheus-server-667df6d57c-77dms 9090
# grafana
kubectl port-forward grafana-5df66b4d87-rkd7n 3000


# scales:
# 3 taskmanagers: < 50000
# 4 taskmanagers: 55000
# 
# 9 Taskmanagers: 75000
```