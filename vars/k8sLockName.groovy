#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Allow to set then name of the lock that will be used.</h1>
 * Give a name.
 *
 * <b>Note:</b> Allow smooth control of lock between cluster and context.
 *
 * @param skipLockName Do not change context.
 * @return k8sLockName The lock name we will use.
 */
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sLockName.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.HELM_KUBECONTEXT = k8sCluster(vars)

  vars.HELM_NAMESPACE = helmNamespace(vars)

  vars.skipLockName = vars.get('skipLockName', false).toBoolean()
  vars.k8sFileId = vars.get('k8sFileId', vars.draftPack ?: '0').trim()
  vars.k8sLockName = vars.get('k8sLockName', vars.HELM_NAMESPACE ?: '0').trim()

  vars.k8sLockOutputFile = vars.get('k8sLockOutputFile', "k8s-lock-${vars.k8sFileId}.log").trim()
  //vars.skipLockNameFailure = vars.get("skipLockNameFailure", true).toBoolean()

  if (!vars.skipLockName) {
    try {
      if (body) { body() }

      if (!vars.HELM_NAMESPACE?.trim()) {
        echo 'Namespace is mandatory'
      } else {
        vars.k8sLockName = "lock_K8S_${vars.HELM_KUBECONTEXT}_${vars.HELM_NAMESPACE}"
      } // HELM_NAMESPACE
    } catch (exc) {
      echo 'Warn: There was a problem with locking k8s ' + exc
    //} finally {
    //  cleanEmptyFile(vars)
    //  archiveArtifacts artifacts: "${vars.k8sLockOutputFile}, ${vars.k8sLockEventsOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'K8s lock skipped'
  }

  return vars.k8sLockName.trim()
}
