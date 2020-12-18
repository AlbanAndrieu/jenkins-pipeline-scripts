#!/bin/bash

displayLogs()
{
    kubectl get all -n"$NAMESPACE"
    PROBLEMS=$(kubectl get all -n"$NAMESPACE"| awk '$2 ~ "/" {print $1" "$2}'|grep -v NAME | awk '{ split($2,A,"/"); if (A[1]!=A[2]) print $1}')
    for elem in $PROBLEMS
    do
        echo "ELEMENT DESCRIPTION: [$elem]....................."
        kubectl describe $elem -n"$NAMESPACE" || true
        IS_POD=$(echo $elem| grep "^pod/" |wc -l)
        if [[ $IS_POD -eq 1 ]]; then
            echo "POD LOGS: [$elem]....................."
            kubectl logs $elem -n"$NAMESPACE" || true
            kubectl logs $elem -n"$NAMESPACE" --previous || true
        fi
    done
}

NAMESPACE="$1"
RESOURCE="$2"
CHECK_CONDITION="$3"
WAIT="$4"
[[ -z $WAIT ]] && WAIT=600s

RESOURCE_LIST=$(kubectl wait --timeout=$WAIT --for=condition=$CHECK_CONDITION --all $RESOURCE --namespace "$NAMESPACE" 2>&1)
RET=$?
if [ $RET -ne 0 ]; then
    if [ "$RESOURCE_LIST" = "error: no matching resources found" ]; then
        echo "No $RESOURCE exist. No need to stop deployment"
        exit 0
    fi
    echo "all $RESOURCE not ready"
    displayLogs
    exit 1
else
    echo "all $RESOURCE ready"
fi
