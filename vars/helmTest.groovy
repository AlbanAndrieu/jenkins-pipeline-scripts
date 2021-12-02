#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmTest.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'packs').toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get('helmRelease', vars.helmChartName ?: 'test').trim()

  vars.customRepoName = vars.get('customRepoName', 'custom').trim()

  vars.skipTest = vars.get('skipTest', false).toBoolean()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.helmTestOutputFile = vars.get('helmTestOutputFile', "helm-test-${vars.helmFileId}.log").trim()
  vars.skipTestFailure = vars.get('skipTestFailure', true).toBoolean()

  if (!vars.skipTest) {
    try {
      //echo "Using ${HELM_REGISTRY_TMP_URL}"

      if (body) { body() }

      String helmTestCmd = 'helm test'

      helmTestCmd += " ${vars.helmRelease}  "
      //helmTestCmd += " ${vars.helmChart} ${vars.customRepoName}/${vars.helmRelease} "

      if (vars.KUBECONFIG?.trim()) {
        helmTestCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.kubeNamespace?.trim()) {
        helmTestCmd += " --namespace ${vars.kubeNamespace} "
      }

      helmTestCmd += vars.helmSetOverride

      // TODO Remove it when tee will be back
      helmTestCmd += " 2>&1 > ${vars.helmTestOutputFile} "

      helm = sh (script: helmTestCmd, returnStatus: true)
      echo "HELM TEST RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM TEST SUCCESS'
      } else {
        echo "WARNING : Helm test failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmTestOutputFile}\' "
        if (!vars.skipTestFailure) {
          echo 'HELM TEST FAILURE'
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm test'
        } else {
          echo 'HELM TEST FAILURE skipped'
        //error 'There are errors in helm'
        }
      }
    } catch (exc) {
      echo 'Warn: There was a problem with testing helm ' + exc
    }
  } else {
    echo 'Helm test skipped'
  }
}
