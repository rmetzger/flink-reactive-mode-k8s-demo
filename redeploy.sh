#!/bin/bash

set -x

./undeploy.sh
./deploy.sh


#JM_POD=`kubectl get pods --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}' | grep "jobm"`

#kubectl wait --for=condition=ready pod $JM_POD
#kubectl port-forward $JM_POD 8081