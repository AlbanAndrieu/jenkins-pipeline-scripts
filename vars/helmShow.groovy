#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmShow.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'packs').toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  //vars.helmRelease = vars.get("helmRelease", vars.helmChartName ?: "test").trim()

  //vars.customRepoName = vars.get("customRepoName", "custom").trim()

  vars.skipShow = vars.get('skipShow', false).toBoolean()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.helmShowOutputFile = vars.get('helmShowOutputFile', "helm-show-${vars.helmFileId}.log").trim()
  vars.skipShowFailure = vars.get('skipShowFailure', true).toBoolean()

  if (!vars.skipShow) {
    try {
      //echo "Using ${HELM_REGISTRY_TMP_URL}"

      if (body) { body() }

      String helmShowCmd = "helm show all ${vars.helmDir}/${vars.helmChartName}"

      if (vars.KUBECONFIG?.trim()) {
        helmShowCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.kubeNamespace?.trim()) {
        helmShowCmd += " --namespace ${vars.kubeNamespace} "
      }

      // TODO Remove it when tee will be back
      helmShowCmd += " 2>&1 > ${vars.helmShowOutputFile} "

      helm = sh (script: helmShowCmd, returnStatus: true)
      echo "HELM SHOW RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM SHOW SUCCESS'
      } else {
        echo "WARNING : Helm show failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmShowOutputFile}\' "
        if (!vars.skipShowFailure) {
          echo 'HELM SHOW FAILURE'
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm show'
        } else {
          echo 'HELM SHOW FAILURE skipped'
        //error 'There are errors in helm'
        }
      }
    } catch (exc) {
      echo 'Warn: There was a problem with showing helm ' + exc
    }
  } else {
    echo 'Helm show skipped'
  }
}
