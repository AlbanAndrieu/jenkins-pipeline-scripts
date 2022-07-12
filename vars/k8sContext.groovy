#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Allow to set the context that will be used.</h1>
 * Do kubectl config use-context.
 *
 * <b>Note:</b> Allow smooth transition between cluster.
 *
 * @param skipKubeContext Do not change context.
 * @return HELM_KUBECONTEXT The namespace we will use.
 */
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sContext.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.HELM_KUBECONTEXT = k8sCluster(vars)

  //vars.HELM_NAMESPACE = helmNamespace(vars)

  vars.skipKubeContext = vars.get('skipKubeContext', false).toBoolean()
  //vars.kubeContextId = vars.get("kubeContextId", vars.draftPack ?: "0").trim()
  //vars.kubeContextOutputFile = vars.get("kubeContextOutputFile", "k8s-namespace-${vars.kubeContextId}.log").trim()

  if (!vars.skipKubeContext) {
    try {
    //tee("${vars.kubeContextOutputFile}") {

      String k8sContextCmd = 'kubectl '

      if (isDebugRun(vars)) {
        k8sContextCmd += ' --debug '
      } // isDebugRun
      if (vars.KUBECONFIG?.trim()) {
        k8sContextCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }

      if (vars.HELM_KUBECONTEXT?.trim()) {
        sh """#!/bin/bash -l
        echo "Set context to : ${vars.HELM_KUBECONTEXT}"
        ${k8sContextCmd} config use-context ${vars.HELM_KUBECONTEXT}
        ${k8sContextCmd} config get-clusters
        """
      }
      sh "${k8sContextCmd} config get-contexts"

    //} // tee
    } catch (exc) {
      echo 'Warn: There was a problem with k8sContext : ' + exc
      error 'There are errors in changing k8sContext'
    //} finally {
    //  cleanEmptyFile(vars)
    //  archiveArtifacts artifacts: "${vars.kubeContextOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'k8sContext skipped'
  }

  return vars.HELM_KUBECONTEXT
}
