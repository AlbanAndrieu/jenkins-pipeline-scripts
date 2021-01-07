#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/k8sWait.groovy`"

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()

  vars.skipWait = vars.get("skipWait", false).toBoolean()
  vars.k8sFileId = vars.get("k8sFileId", vars.draftPack ?: "0").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "fr-standalone-devops").trim()
  vars.kubeRessource = vars.get("kubeRessource", "deployment").trim()  // job deployment
  vars.kubeCondition = vars.get("kubeCondition", "available").trim()  // complete available
  vars.kubeTimeout = vars.get("kubeTimeout", "600s").trim()

  vars.k8sWaitEventsOutputFile = vars.get("k8sWaitEventsOutputFile", "k8s-events-${vars.k8sFileId}.log").trim()
  vars.k8sWaitOutputFile = vars.get("k8sWaitOutputFile", "k8s-wait-${vars.k8sFileId}.log").trim()
  vars.skipWaitFailure = vars.get("skipWaitFailure", true).toBoolean()

  if (!vars.skipWait) {
    try {

      if (body) { body() }

      if (!vars.kubeNamespace?.trim()) {
        echo "Namespace is mandatory"

        sh "kubectl -n ${vars.kubeNamespace}  get events --sort-by='{.lastTimestamp}' 2>&1 > ${vars.k8sWaitEventsOutputFile}"

        writeFile(file: "${pwd()}/wait_resource.sh", text: libraryResource('wait_resource.sh'))
        sh("ls -lrta ${pwd()}/")
        sh("chmod a+x ${pwd()}/wait_resource.sh")
        echo "${pwd()}/wait_resource.sh"

        echo "Checking : wait_resource.sh ${vars.kubeNamespace} ${vars.kubeRessource} ${vars.kubeCondition} ${vars.kubeTimeout}"
        String k8sWaitCmd = "export KUBECONFIG=${vars.KUBECONFIG}; ${pwd()}/wait_resource.sh ${vars.kubeNamespace} ${vars.kubeRessource} ${vars.kubeCondition} ${vars.kubeTimeout}"

        //k8sWaitCmd += vars.k8sSetOverride

        // TODO Remove it when tee will be back
        k8sWaitCmd += " 2>&1 > ${vars.k8sWaitOutputFile} "

        k8s = sh (script: k8sWaitCmd, returnStatus: true)
        echo "K8S WAIT RETURN CODE : ${k8s}"
        if (k8s == 0) {
          echo "K8S WAIT SUCCESS"
        } else {
          echo "WARNING : K8s wait failed, check output at \'${env.BUILD_URL}/artifact/${vars.k8sWaitOutputFile}\' "
          if (!vars.skipWaitFailure) {
            echo "K8S WAIT FAILURE"
            //currentBuild.result = 'UNSTABLE'
            currentBuild.result = 'FAILURE'
            error 'There are errors in k8s wait'
          } else {
            echo "K8S WAIT FAILURE skipped"
            //error 'There are errors in k8s'
          }
        }
      }
    } catch (exc) {
      echo "Warn: There was a problem with waiting k8s " + exc.toString()
    } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: "${vars.k8sWaitOutputFile}, ${vars.k8sWaitEventsOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo "K8s wait skipped"
  }

  k8sCleaning(vars)
}
