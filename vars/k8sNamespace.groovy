#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sNamespace.groovy`'

  vars = vars ?: [:]

  vars.KUBE_NAMESPACE_URL = vars.get('KUBE_NAMESPACE_URL', env.KUBE_NAMESPACE_URL ?: 'http://albandrieu.com/download/kubectl').trim()
  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()
  vars.kubeContext = vars.get('kubeContext', env.KUBECONTEXT ?: 'microk8s-cluster').trim()
  vars.kubeNamespaceConfigFile  = vars.get('kubeNamespaceConfigFile', env.KUBENAMESPACE_FILE ?: 'namespace-jenkins.json').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  // See https://kubernetes.io/docs/tasks/administer-cluster/namespaces-walkthrough/

  vars.skipKubeNamespace = vars.get('skipKubeNamespace', false).toBoolean()
  vars.kubeNamespaceId = vars.get('kubeNamespaceId', vars.draftPack ?: '0').trim()
  vars.kubeNamespaceOutputFile = vars.get('kubeNamespaceOutputFile', "k8s-namespace-${vars.kubeNamespaceId}.json").trim()

  echo 'Check credentials at : https://rancher.albandrieu.com/'
  println hudson.console.ModelHyperlinkNote.encodeTo('https://rancher.albandrieu.com/', 'rancher')

  if (!vars.skipKubeNamespace) {
    try {
      tee("${vars.kubeNamespaceOutputFile}") {
        sh """#!/bin/bash -l
          #kubectl plugin list || true
          kubectl --kubeconfig=${params.KUBECONFIG} get namespaces || true
          kubectl --kubeconfig=${params.KUBECONFIG} create -f ${vars.KUBE_NAMESPACE_URL}/${vars.kubeNamespaceConfigFile} || true
          kubectl --kubeconfig=${params.KUBECONFIG} get namespaces --show-labels || true
          """

          if (!vars.kubeNamespace?.trim()) {
          sh """#!/bin/bash -l
            kubectl --kubeconfig=${params.KUBECONFIG} config set-context --current --namespace=${vars.kubeNamespace}
            kubectl --kubeconfig=${params.KUBECONFIG} config get-contexts || true
            kubectl --kubeconfig=${params.KUBECONFIG} get pods -n ${vars.kubeNamespace} || true
            kubectl --kubeconfig=${params.KUBECONFIG} get svc -n ${vars.kubeNamespace} || true
            """
          } // kubeNamespace
        } // tee
        } catch (exc) {
      echo 'Warn: There was a problem with k8s ' + exc
      println hudson.console.ModelHyperlinkNote.encodeTo(env.BUILD_URL + '/artifact/k8s-config.yml', 'k8s-config.yml')
        } finally {
      archiveArtifacts artifacts: "k8s-*.yml, **/k8s-*.log, ${vars.KUBECONFIG}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'KubeNamespace skipped'
  }
}
