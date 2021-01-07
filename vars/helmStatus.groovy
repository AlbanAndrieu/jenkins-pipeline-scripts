#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmStatus.groovy`"

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()

  vars.helmRelease = vars.get("helmRelease", vars.helmChartName ?: "test").trim()

  vars.skipStatus = vars.get("skipStatus", false).toBoolean()
  vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "fr-standalone-devops").trim()

  vars.helmStatusOutputFile = vars.get("helmStatusOutputFile", "helm-status-${vars.helmFileId}.log").trim()
  vars.skipStatusFailure = vars.get("skipStatusFailure", true).toBoolean()

  if (!vars.skipStatus) {
    try {
      //echo "Using ${HELM_REGISTRY_TMP_URL}"

      if (body) { body() }

      String helmStatusCmd = "helm status ${vars.helmRelease}"

      if (vars.KUBECONFIG?.trim()) {
        helmStatusCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.kubeNamespace?.trim()) {
        helmStatusCmd += " --namespace ${vars.kubeNamespace} "
      }

      // TODO Remove it when tee will be back
      helmStatusCmd += " 2>&1 > ${vars.helmStatusOutputFile} "

      helm = sh (script: helmStatusCmd, returnStatus: true)
      echo "HELM STATUS RETURN CODE : ${helm}"
      if (helm == 0) {
        echo "HELM STATUS SUCCESS"
      } else {
        echo "WARNING : Helm status failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmStatusOutputFile}\' "
        if (!vars.skipStatusFailure) {
          echo "HELM STATUS FAILURE"
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm status'
        } else {
          echo "HELM STATUS FAILURE skipped"
          //error 'There are errors in helm'
        }
      }

    } catch (exc) {
      echo "Warn: There was a problem with status helm " + exc.toString()
    }
  } else {
    echo "Helm status skipped"
  }
}
