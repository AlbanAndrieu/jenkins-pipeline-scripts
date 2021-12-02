#!/bin/bash

displayLogs() {
  kubectl get all --context "$CONTEXT" --namespace "$NAMESPACE"
  PROBLEMS=$(kubectl get all --context "$CONTEXT" --namespace "$NAMESPACE" | awk '$2 ~ "/" {print $1" "$2}' | grep -v NAME | awk '{ split($2,A,"/"); if (A[1]!=A[2]) print $1}')
  for elem in $PROBLEMS; do
    echo "ELEMENT DESCRIPTION: [$elem]....................."
    kubectl describe $elem --context "$CONTEXT" --namespace "$NAMESPACE" || true
    IS_POD=$(echo $elem | grep "^pod/" | wc -l)
    if [[ $IS_POD -eq 1 ]]; then
      echo "POD LOGS: [$elem]....................."
      kubectl logs $elem --context "$CONTEXT" --namespace "$NAMESPACE" || true
      kubectl logs $elem --context "$CONTEXT" --namespace "$NAMESPACE" --previous || true
    fi
  done
}

CONTEXT="$1"
NAMESPACE="$2"
RESOURCE="$3"
CHECK_CONDITION="$4"
WAIT="$5"
[[ -z $WAIT ]] && WAIT=600s

RESOURCE_LIST=$(kubectl wait --timeout=$WAIT --for=condition=$CHECK_CONDITION --all $RESOURCE --context "$CONTEXT" --namespace "$NAMESPACE" 2>&1)
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
