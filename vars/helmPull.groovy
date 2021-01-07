#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmPull.groovy`"

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()

  //vars.HELM_PROJECT = vars.get("HELM_PROJECT", env.HELM_PROJECT ?: "nabla").trim()

  //vars.HELM_REGISTRY_TMP = vars.get("HELM_REGISTRY_TMP", env.HELM_REGISTRY_TMP ?: "registry.hub.docker.com").toLowerCase().trim()
  //vars.HELM_REGISTRY_REPO_URL = vars.get("HELM_REGISTRY_REPO_URL", env.HELM_REGISTRY_REPO_URL ?: "https://${vars.HELM_REGISTRY_TMP}/chartrepo/${vars.HELM_PROJECT}").trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get("helmDir", "./packs").toLowerCase().trim()
  vars.helmChartName = vars.get("helmChartName", vars.draftPack ?: "packs").toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get("helmRelease", vars.helmChartName ?: "test").trim()

  vars.customRepoName = vars.get("customRepoName", "custom").trim()
  vars.isDevel = vars.get("isDevel", true).toBoolean()

  vars.skipPull = vars.get("skipPull", false).toBoolean()
  vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "fr-standalone-devops").trim()

  vars.helmPullOutputFile = vars.get("helmPullOutputFile", "helm-pull-${vars.helmFileId}.log").trim()
  vars.skipPullFailure = vars.get("skipPullFailure", true).toBoolean()

  if (!vars.skipPull) {
    try {
      if (body) { body() }

      String helmPullCmd = "helm pull ${vars.customRepoName}/${vars.helmChartName}"
      //String helmPullCmd = "helm pull ${vars.HELM_REGISTRY_REPO_URL}/${vars.helmChartName}"

      if (vars.KUBECONFIG?.trim()) {
        helmPullCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.kubeNamespace?.trim()) {
        helmPullCmd += " --namespace ${vars.kubeNamespace} "
      }
      if (vars.isDevel.toBoolean()) {
        helmPullCmd += " --devel "
      }

      // TODO Remove it when tee will be back
      helmPullCmd += " 2>&1 > ${vars.helmPullOutputFile} "

      helm = sh (script: helmPullCmd, returnStatus: true)
      echo "HELM PULL RETURN CODE : ${helm}"
      if (helm == 0) {
        echo "HELM PULL SUCCESS"
      } else {
        echo "WARNING : Helm pull failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmPullOutputFile}\' "
        if (!vars.skipPullFailure) {
          echo "HELM PULL FAILURE"
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm pull'
        } else {
          echo "HELM PULL FAILURE skipped"
          //error 'There are errors in helm'
        }
      }

    } catch (exc) {
      echo "Warn: There was a problem with pull helm " + exc.toString()
    }
  } else {
    echo "Helm pull skipped"
  }
}
