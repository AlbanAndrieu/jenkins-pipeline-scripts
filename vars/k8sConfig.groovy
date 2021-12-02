#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sConfig.groovy`'

  vars = vars ?: [:]

  vars.KUBE_NAMESPACE_URL = vars.get('KUBE_NAMESPACE_URL', env.KUBE_NAMESPACE_URL ?: 'http://albandrieu.com/download/kubectl').trim()
  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()
  vars.kubeContext = vars.get('kubeContext', env.KUBECONTEXT ?: 'microk8s-cluster').trim()
  //vars.kubeNamespaceConfigFile  = vars.get("kubeNamespaceConfigFile", env.KUBENAMESPACE_FILE ?: "namespace-jenkins.json").trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'frontend').trim()

  // See https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters/

  vars.skipKubeConfig = vars.get('skipKubeConfig', false).toBoolean()
  vars.kubeConfigId = vars.get('kubeConfigId', vars.draftPack ?: '0').trim()
  vars.kubeConfigOutputFile = vars.get('kubeConfigOutputFile', "k8s-namespace-${vars.kubeConfigId}.json").trim()
  vars.kubeConfigYmlOutputFile = vars.get('kubeConfigYmlOutputFile', "k8s-config-${vars.kubeConfigId}.yml").trim()
  vars.kubeClusterInfoOutputFile = vars.get('kubeClusterInfoOutputFile', "k8s-cluster-info-${vars.kubeConfigId}.json").trim()

  if (!vars.skipKubeConfig) {
    try {
      //tee("${vars.kubeConfigOutputFile}") {
      sh """#!/bin/bash -l
          kubectl --kubeconfig=config-jenkins config set-cluster development --server=https://127.0.0.1:16443 --insecure-skip-tls-verify
          kubectl --kubeconfig=config-jenkins config set-credentials developer
          kubectl --kubeconfig=config-jenkins config set-context dev-frontend --cluster=development --namespace=frontend --user=developer
          kubectl --kubeconfig=config-jenkins config set-context dev-storage --cluster=development --namespace=storage --user=developer
          kubectl --kubeconfig=${params.KUBECONFIG} cluster-info || true
          kubectl --kubeconfig=${params.KUBECONFIG} cluster-info dump 2>&1 > ${vars.kubeClusterInfoOutputFile} || true
          kubectl --kubeconfig=${params.KUBECONFIG} config  view --minify
          kubectl --kubeconfig=${params.KUBECONFIG} config  view > ${vars.kubeConfigYmlOutputFile}
          """

      sh """#!/bin/bash -l
          kubectl --kubeconfig=config-jenkins config set-cluster development --server=https://127.0.0.1:16443 --insecure-skip-tls-verify
          kubectl --kubeconfig=config-jenkins config set-credentials developer
          kubectl --kubeconfig=config-jenkins config set-context dev-frontend --cluster=development --namespace=frontend --user=developer
          kubectl --kubeconfig=config-jenkins config set-context dev-storage --cluster=development --namespace=storage --user=developer

          kubectl --kubeconfig=${params.KUBECONFIG} cluster-info || true
          kubectl --kubeconfig=${params.KUBECONFIG} cluster-info dump 2>&1 > ${vars.kubeClusterInfoOutputFile} || true
          kubectl --kubeconfig=${params.KUBECONFIG} config  view --minify
          kubectl --kubeconfig=${params.KUBECONFIG} config  view > ${vars.kubeConfigYmlOutputFile}
          """

      if (!vars.kubeContext?.trim()) {
        sh """#!/bin/bash -l
            echo "Set context to : ${vars.kubeContext}"
            kubectl --kubeconfig=${params.KUBECONFIG} config use-context ${vars.kubeContext}
            """
      }
      sh "kubectl --kubeconfig=${params.KUBECONFIG} config get-contexts"
      if (!vars.kubeNamespace?.trim()) {
        sh """#!/bin/bash -l
            echo "Set namespace to : ${vars.kubeNamespace}"
            kubectl --kubeconfig=${params.KUBECONFIG} config set-context --current --namespace=${vars.kubeNamespace}
            """
      }

      sh "kubectl --kubeconfig=${params.KUBECONFIG} get namespaces --show-labels"

    //kubectl describe pod
    //} // tee
    } catch (exc) {
      echo 'Warn: There was a problem with k8s config ' + exc
      println hudson.console.ModelHyperlinkNote.encodeTo(env.BUILD_URL + '/artifact/${vars.kubeConfigYmlOutputFile}', "${vars.kubeConfigYmlOutputFile}")
      println hudson.console.ModelHyperlinkNote.encodeTo(env.BUILD_URL + '/artifact/${vars.kubeClusterInfoOutputFile}', "${vars.kubeClusterInfoOutputFile}")
    } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: "k8s-*.yml, **/k8s-*.log, ${vars.KUBECONFIG}, ${vars.kubeConfigOutputFile}, ${vars.kubeClusterInfoOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'KubeConfug skipped'
  }
}
