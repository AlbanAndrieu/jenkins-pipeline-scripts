#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Allow to get events for cluster deployment.</h1>
 * Events for deployment status.
 *
 * <b>Note:</b> Allow to  get events for deployment to finish.
 *
 * @param skipEvents Do not get cluster events.
 */
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sEvents.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.HELM_CONFIG_HOME = vars.get('HELM_CONFIG_HOME', env.HELM_CONFIG_HOME ?: '/home/jenkins/.kube/').trim()
  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: "${vars.HELM_CONFIG_HOME}config").trim()

  vars.HELM_KUBECONTEXT = k8sCluster(vars)

  vars.HELM_NAMESPACE = helmNamespace(vars)

  vars.buildDir = vars.get('buildDir', "${pwd()}/").trim()

  vars.DOCKER_REGISTRY_ACR = vars.get('DOCKER_REGISTRY_ACR', env.DOCKER_REGISTRY_ACR ?: 'p21d13401013001.azurecr.io').toLowerCase().trim()
  vars.DOCKER_HELM_IMAGE_TAG = vars.get('DOCKER_HELM_IMAGE_TAG', '3.2.0-dev').trim()
  vars.DOCKER_HELM_IMAGE = vars.get('DOCKER_HELM_IMAGE', "${vars.DOCKER_REGISTRY_ACR}/global-bakery-external/helm:${vars.DOCKER_HELM_IMAGE_TAG}").trim()

  //vars.dockerKubeEventsCmd = vars.get("dockerKubeEventsCmd", "docker run --rm -v ${vars.HELM_CONFIG_HOME}:/root/.kube -v ${vars.buildDir}:/tmp --workdir /tmp ")
  //vars.dockerKubeEventsCmd += " ${vars.DOCKER_HELM_IMAGE} kubectl " // open /root/.kube/config: permission denied
  vars.dockerKubeEventsCmd = ' kubectl '

  vars.skipEvents = vars.get('skipEvents', false).toBoolean()
  vars.k8sFileId = vars.get('k8sFileId', vars.draftPack ?: '0').trim()

  vars.k8sEventsOutputFile = vars.get('k8sEventsOutputFile', "k8s-events-${vars.k8sFileId}.log").trim()
  vars.skipEventsFailure = vars.get('skipEventsFailure', true).toBoolean()

  if (!vars.skipEvents) {
    try {
      if (body) { body() }

      if (!vars.HELM_NAMESPACE?.trim()) {
        echo 'Namespace is mandatory'
      } else {
        String k8sEventsCmd = vars.dockerKubeEventsCmd
        //k8sEventsCmd += " "

        if (isDebugRun(vars)) {
          k8sEventsCmd += ' --debug '
        } // isDebugRun
        if (vars.KUBECONFIG?.trim()) {
          k8sEventsCmd += " --kubeconfig ${vars.KUBECONFIG} "
        //k8sEventsCmd +=" --kubeconfig /root/.kube/config "
        }
        if (vars.HELM_KUBECONTEXT?.trim()) {
          k8sEventsCmd += " --context ${vars.HELM_KUBECONTEXT} "
        }

        //if (!vars.HELM_NAMESPACE?.trim()) {
        //  echo "Namespace is mandatory"
        //} else {
        //  // Issue on new cluster : sorter.go:353] Field {.lastTimestamp} in [][][]reflect.Value is an unsortable type
        //  //sh "kubectl --namespace ${vars.HELM_NAMESPACE} get events --sort-by='{.lastTimestamp}' || true > ${vars.k8sEventsEventsOutputFile} 2>&1 "
        //  k8sEventsCmd += " get events --sort-by=.metadata.creationTimestamp || true "
        //}

        if (vars.HELM_NAMESPACE?.trim()) {
          k8sEventsCmd += " --namespace ${vars.HELM_NAMESPACE} "
        }

        k8sEventsCmd += ' get events --sort-by=.metadata.creationTimestamp '

        // TODO Remove it when tee will be back
        k8sEventsCmd += " > ${vars.k8sEventsOutputFile} 2>&1 "

        k8s = sh (script: k8sEventsCmd, returnStatus: true)
        echo "K8S EVENTS RETURN CODE : ${k8s}"
        if (k8s == 0) {
          echo 'K8S EVENTS SUCCESS'
        } else {
          echo "WARNING : K8s events failed, check output at \'${env.BUILD_URL}/artifact/${vars.k8sEventsOutputFile}\' "

          if (isManager(vars)) {
            def summary = manager.createSummary('images/48x48/error.gif')
            summary.appendText("<br/>Cluster errors detected : ${vars.HELM_NAMESPACE} for ${vars.kubeRessource} ${vars.kubeCondition} ${vars.kubeTimeout} : <a href='${env.BUILD_URL}artifact/${vars.k8sEventsOutputFile}/*view*/'>${vars.k8sEventsOutputFile}</a>", false, false, false, 'black')
          } // isManager

          if (!vars.skipEventsFailure) {
            echo 'K8S EVENTS FAILURE'
            currentBuild.result = 'UNSTABLE'
          //currentBuild.result = 'FAILURE'
          //TODO error 'There are errors in k8s events'
          } else {
            echo 'K8S EVENTS FAILURE skipped'
          //error 'There are errors in k8s'
          }
        }
      } // HELM_NAMESPACE
    } catch (exc) {
      echo 'Warn: There was a problem with k8sEvents : ' + exc
    } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: "${vars.k8sEventsOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'k8sEvents skipped'
  }
}
