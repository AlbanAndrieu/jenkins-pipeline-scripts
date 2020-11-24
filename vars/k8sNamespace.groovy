#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/k8sNamespace.groovy"

  vars = vars ?: [:]

  vars.KUBE_NAMESPACE_URL = vars.get("KUBE_NAMESPACE_URL", env.KUBE_NAMESPACE_URL ?: "http://fr1cslfrbm0059.misys.global.ad/download/kubectl").trim()
  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()
  vars.kubeContext = vars.get("kubeContext", env.KUBECONTEXT ?: "treasury-trba").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "namespace-dev.json").trim()

  // See https://kubernetes.io/docs/tasks/administer-cluster/namespaces-walkthrough/

  try {
      sh """#!/bin/bash -l
      kubectl plugin list || true
      kubectl get namespaces
      kubectl create -f ${vars.KUBE_NAMESPACE_URL}l/${vars.kubeNamespace}
      kubectl config view
      kubectl config use-context ${vars.kubeContex}
      kubectl get namespaces --show-labels
      """
  } catch (exc) {
    echo "Warn: There was a problem with k8s " + exc.toString()
  } finally {
    archiveArtifacts artifacts: "k8s.yml, **/k8s-*.log, ${vars.KUBECONFIG}", onlyIfSuccessful: false, allowEmptyArchive: true
  }
}
