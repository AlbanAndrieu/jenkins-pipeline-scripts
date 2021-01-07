#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmLog.groovy`"

  vars = vars ?: [:]

  //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
  def DEBUG_RUN = vars.get("DEBUG_RUN", true).toBoolean()

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get("helmDir", "./packs").toLowerCase().trim()
  vars.helmChartName = vars.get("helmChartName", vars.draftPack ?: "packs").toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get("helmRelease", vars.helmChartName ?: "test").trim()

  //vars.customRepoName = vars.get("customRepoName", "custom").trim()

  vars.skipLog = vars.get("skipLog", false).toBoolean()
  vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "fr-standalone-devops").trim()
  //vars.kubeRessource = vars.get("kubeRessource", "job").trim()  // job deployment
  //vars.kubeCondition = vars.get("kubeCondition", "complete").trim()  // complete available
  //vars.kubeTimeout = vars.get("kubeTimeout", "600s").trim()
  vars.kubeNodePort = vars.get("kubeNodePort", "30037").trim()
  vars.kubeServiceName = vars.get("kubeServiceName", "helm-sample").trim()

  vars.helmLogOutputFile = vars.get("helmLogOutputFile", "helm-log-${vars.helmFileId}.log").trim()
  vars.skipLogFailure = vars.get("skipLogFailure", true).toBoolean()

  if (!vars.skipLog) {
    try {
      //echo "Using ${HELM_REGISTRY_TMP_URL}"

      if (body) { body() }

      if (DEBUG_RUN) {
        sh """#!/bin/bash -l
        helm env --namespace ${vars.kubeNamespace} || true
        """
      }

      String helmLogCmd = "export KUBECONFIG=${vars.KUBECONFIG}; ls -lrta ${pwd()}"

      if (!vars.kubeNamespace?.trim()) {
        echo "Namespace is mandatory"
      }

      //writeFile(file: "${pwd()}@tmp/wait_resource.sh", text: libraryResource('wait_resource.sh'))
      //echo "Checking : ${vars.helmRelease} - wait_resource.sh ${vars.kubeNamespace} ${vars.kubeRessource} ${vars.kubeCondition} ${vars.kubeTimeout}"
      //sh("chmod a+x ${pwd()}@tmp/wait_resource.sh")

      sh """#!/bin/bash -l
      kubectl get pod -n ${vars.kubeNamespace} -l app.kubernetes.io/name=${vars.kubeServiceName} -o name | cut -d '/' -f 2 | xargs -n1 -I {} kubectl exec -it {} -n kube-system curl 127.0.0.1:${vars.kubeNodePort}
      """

      helmLogCmd += "kubectl "

      //if (vars.KUBECONFIG?.trim()) {
      //  helmLogCmd +=" --kubeconfig ${vars.KUBECONFIG} "
      //}

      echo "get pod -n ${vars.kubeNamespace} -l app.kubernetes.io/name=${vars.kubeServiceName} -o name"

      helmLogCmd +="get pod -n ${vars.kubeNamespace} -l app.kubernetes.io/name=${vars.kubeServiceName} -o name "

      // TODO Remove it when tee will be back
      helmLogCmd += " 2>&1 > ${vars.helmLogOutputFile} "

      helm = sh (script: helmLogCmd, returnStatus: true)
      echo "HELM LOG RETURN CODE : ${helm}"
      if (helm == 0) {
        echo "HELM LOG SUCCESS"
      } else {
        echo "WARNING : Helm log failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmLogOutputFile}\' "
        if (!vars.skipLogFailure) {
          echo "HELM LOG FAILURE"
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm log'
        } else {
          echo "HELM LOG FAILURE skipped"
          //error 'There are errors in helm'
        }
      }

    } catch (exc) {
      echo "Warn: There was a problem with logging helm " + exc.toString()
    }
  } else {
    echo "Helm log skipped"
  }
}
