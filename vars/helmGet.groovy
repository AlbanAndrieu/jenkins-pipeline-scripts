#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmGet.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'packs').toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get('helmRelease', vars.helmChartName ?: 'test').trim()

  //vars.customRepoName = vars.get("customRepoName", "custom").trim()

  vars.skipGet = vars.get('skipGet', false).toBoolean()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.helmGetOutputFile = vars.get('helmGetOutputFile', "helm-get-${vars.helmFileId}.log").trim()
  vars.skipGetFailure = vars.get('skipGetFailure', true).toBoolean()

  if (!vars.skipGet) {
    try {
      //echo "Using ${HELM_REGISTRY_TMP_URL}"

      if (body) { body() }

      String helmGetCmd = "helm get all ${vars.helmRelease}"

      if (vars.KUBECONFIG?.trim()) {
        helmGetCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.kubeNamespace?.trim()) {
        helmGetCmd += " --namespace ${vars.kubeNamespace} "
      }

      // TODO Remove it when tee will be back
      helmGetCmd += " 2>&1 > ${vars.helmGetOutputFile} "

      helm = sh (script: helmGetCmd, returnStatus: true)
      echo "HELM GET RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM GET SUCCESS'
      } else {
        echo "WARNING : Helm get failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmGetOutputFile}\' "
        if (!vars.skipGetFailure) {
          echo 'HELM GET FAILURE'
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm get'
        } else {
          echo 'HELM GET FAILURE skipped'
        //error 'There are errors in helm'
        }
      }
    } catch (exc) {
      echo 'Warn: There was a problem with get helm ' + exc
    }
  } else {
    echo 'Helm get skipped'
  }
}
