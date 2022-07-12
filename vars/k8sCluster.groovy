#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Allow to get the cluster/context that will be used.</h1>
 *
 * <b>Note:</b> Will be use by k8sContext and helm cmd1.
 *
 * @param skipKubeContext Do not change context.
 * @return HELM_KUBECONTEXT The namespace we will use.
 */
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sCluster.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.HELM_CONFIG_HOME = vars.get('HELM_CONFIG_HOME', env.HELM_CONFIG_HOME ?: '/home/jenkins/.kube/').trim()
  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: "${vars.HELM_CONFIG_HOME}config").trim()
  vars.HELM_KUBECONTEXT = vars.get('HELM_KUBECONTEXT', params.HELM_KUBECONTEXT ?: (env.HELM_KUBECONTEXT ?: 'treasury-trba1')).trim() // treasury-trba1 to devops-fr

  // FYI :
  // helmCmd += " --kube-context ${vars.HELM_KUBECONTEXT} "
  // k8sCmd += " --context ${vars.HELM_KUBECONTEXT} "

  if (!isHelmContext(vars)) {
    if ( env.BRANCH_NAME ==~ /release\/1\.8\..*/ ) {
      vars.HELM_KUBECONTEXT = 'treasury-trba1'
    } else if ( helmBranchTempEnable(vars) ) {
      vars.HELM_KUBECONTEXT = 'devops-fr'
    } else if ( BRANCH_NAME ==~ /develop/ ) {
      vars.HELM_KUBECONTEXT = 'devops-fr' // When triggered by other job
    } else {
      vars.HELM_KUBECONTEXT = 'treasury-trba1'
    }
  } // params.HELM_KUBECONTEXT is empty

  return vars.HELM_KUBECONTEXT
}
