#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}


def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/k8sNamespace.groovy`"

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()
  vars.kubeContext = vars.get("kubeContext", env.KUBECONTEXT ?: "treasury-trba").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "fr-standalone-devops").trim()
  vars.kubeNamespaceConfigFile  = vars.get("kubeNamespaceConfigFile", env.KUBENAMESPACE_FILE ?: "namespace-${vars.kubeNamespace}.json").trim()
  vars.kubeNamespaceLimitRangeConfigFile  = vars.get("kubeNamespaceLimitRangeConfigFile", env.KUBENAMESPACE_FILE ?: "namespace-${vars.kubeNamespace}-limitrange.yaml").trim()

  // See https://kubernetes.io/docs/tasks/administer-cluster/namespaces-walkthrough/

  vars.skipKubeNamespaceTerminating = vars.get("skipKubeNamespaceTerminating", false).toBoolean()
  vars.kubeNamespaceId = vars.get("kubeNamespaceId", vars.draftPack ?: "0").trim()
  vars.kubeNamespaceOutputFile = vars.get("kubeNamespaceOutputFile", "k8s-namespace-terminating-${vars.kubeNamespaceId}.json").trim()

  //if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
  //    vars.skipKubeNamespaceTerminating = true
  //}

  if (!vars.skipKubeNamespaceTerminating) {
    try {
        //tee("${vars.kubeNamespaceOutputFile}") {

          // See https://blog.zwindler.fr/2020/03/23/supprimer-un-namespace-bloque-a-terminating/
          if (!vars.kubeNamespace?.trim()) {
            sh """#!/bin/bash -l
            (
            NAMESPACE=${vars.kubeNamespace}
            kubectl proxy &
            kubectl get namespace $NAMESPACE -o json |jq '.spec = {"finalizers":[]}' >temp.json
            curl -k -H "Content-Type: application/json" -X PUT --data-binary @temp.json 127.0.0.1:8001/api/v1/namespaces/$NAMESPACE/finalize
            )
            """
          } // kubeNamespace

        //} // tee
    } catch (exc) {
      echo "Warn: There was a problem with k8s " + exc.toString()
    } finally {
      archiveArtifacts artifacts: "k8s-*.yml, **/k8s-*.log, ${vars.KUBECONFIG}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo "KubeNamespace skipped"
  }
}
