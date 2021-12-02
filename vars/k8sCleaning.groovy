#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sCleaning.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()

  vars.skipK8sCleaning = vars.get('skipK8sCleaning', true).toBoolean()
  vars.k8sFileId = vars.get('k8sFileId', vars.draftPack ?: '0').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.k8sWaitOutputFile = vars.get('k8sWaitOutputFile', "k8s-clean-${vars.k8sFileId}.log").trim()
  //vars.skipK8sCleaningFailure = vars.get("skipK8sCleaningFailure", true).toBoolean()

  if (!vars.skipK8sCleaning) {
    try {
      if (body) { body() }

      if (!vars.kubeNamespace?.trim()) {
        echo 'Namespace is mandatory'
      }

      sh '''#!/bin/bash -l
      echo "Cleaning for jenkins : "
      kubectl get pods -o name --selector=jenkins=albandri-slave --all-namespaces | xargs -I {} kubectl delete {}
      kubectl get pods -o name --selector=jenkins=almonde-slave --all-namespaces | xargs -I {} kubectl delete {}
      kubectl get pods -o name --selector=jenkins=risk-slave --all-namespaces | xargs -I {} kubectl delete {}
      '''
    } catch (exc) {
      echo 'Warn: There was a problem with cleaning k8s ' + exc
    }
  } else {
    echo 'Kube cleaning skipped'
  }
}
