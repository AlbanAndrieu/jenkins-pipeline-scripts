#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmDelete.groovy"

  vars = vars ?: [:]

  String HELM_REGISTRY_CREDENTIAL = vars.get("HELM_REGISTRY_CREDENTIAL", env.HELM_REGISTRY_CREDENTIAL ?: "ad.jenkins-bm").trim()
  //String HELM_ORGANISATION = vars.get("HELM_ORGANISATION", env.HELM_ORGANISATION ?: "fusion-risk").trim()
  String HELM_PROJECT = vars.get("HELM_PROJECT", env.HELM_PROJECT ?: "fusion-risk").trim()

  String HELM_REGISTRY_STABLE_URL = vars.get("HELM_REGISTRY_STABLE_URL", env.HELM_REGISTRY_STABLE_URL ?: "https://kubernetes-charts.storage.googleapis.com/").toLowerCase().trim()

  String HELM_REGISTRY = vars.get("HELM_REGISTRY", env.HELM_REGISTRY ?: "registry.misys.global.ad").toLowerCase().trim()
  String HELM_REGISTRY_URL = vars.get("HELM_REGISTRY_URL", env.HELM_REGISTRY_URL ?: "https://${HELM_REGISTRY}/api/chartrepo/${HELM_PROJECT}/charts").trim()

  String HELM_REGISTRY_TMP = vars.get("HELM_REGISTRY_TMP", env.HELM_REGISTRY_TMP ?: "albandrieu:6532/harbor").toLowerCase().trim()
  String HELM_REGISTRY_TMP_REPO_URL = vars.get("HELM_REGISTRY_TMP_REPO_URL", env.HELM_REGISTRY_TMP_REPO_URL ?: "https://${HELM_REGISTRY_TMP}/chartrepo/${HELM_PROJECT}").trim()
  String HELM_REGISTRY_TMP_URL = vars.get("HELM_REGISTRY_TMP_URL", env.HELM_REGISTRY_TMP_URL ?: "${HELM_REGISTRY_TMP_REPO_URL}/charts").trim()

  //String DRAFT_BRANCH = vars.get("DRAFT_BRANCH", params.DRAFT_BRANCH ?: "develop").trim()
  //String DRAFT_VERSION = vars.get("DRAFT_VERSION", env.DRAFT_VERSION ?: "0.16.0").trim()

  vars.helmChart = vars.get("helmChart", "charts").trim()
  vars.helmRelease = vars.get("helmRelease", "test").trim()
  vars.buildDir = vars.get("buildDir", "${pwd()}").trim()

  vars.skipStableRepo = vars.get("skipStableRepo", true).toBoolean()
  vars.stableRepoName = vars.get("stableRepoName", "stable").trim()
  vars.skipCustomRepo = vars.get("skipCustomRepo", false).toBoolean()
  vars.customRepoName = vars.get("customRepoName", "custom").trim()
  vars.isHelm2 = vars.get("isHelm2", true).toBoolean()
  vars.isDelete = vars.get("isDelete", false).toBoolean()
  vars.isHarbor = vars.get("isHarbor", true).toBoolean()
  vars.isProvenance = vars.get("isProvenance", false).toBoolean()

  vars.helmDeleteOutputFile = vars.get("helmDeleteOutputFile", "helm-delete.log").trim()
  vars.skipFailure = vars.get("skipFailure", true).toBoolean()

  try {
    echo "Using ${HELM_REGISTRY_TMP_URL}"

    dir("${vars.buildDir}") {

      //withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: HELM_REGISTRY_CREDENTIAL, usernameVariable: 'HELM_USERNAME', passwordVariable: 'HELM_PASSWORD']]) {

        if (body) { body() }

        String helmDeleteCmd = ""

        // Install chart from repo
        // https://github.com/goharbor/harbor-helm
        if (vars.isHarbor.toBoolean()) {
          if (vars.isDelete.toBoolean()) {
            if (vars.isHelm2.toBoolean()) {
              helmDeleteCmd = "helm delete --purge ${vars.helmRelease} || true"
            } else {
              helmDeleteCmd = "helm uninstall ${vars.helmRelease}"
            }
          } // isDelete
        } // isHarbor

        // TODO Remove it when tee will be back
        helmDeleteCmd += " 2>&1 > ${vars.helmDeleteOutputFile} "

        // https://github.com/helm/chartmuseum
        helm = sh (script: helmDeleteCmd, returnStatus: true)
        echo "HELM RETURN CODE : ${helm}"
        if (helm == 0) {
          echo "HELM SUCCESS"
        } else {
	      echo "WARNING : Helm delete failed, check output at \'${vars.helmDeleteOutputFile}\' "
          if (!vars.skipFailure) {
            echo "HELM FAILURE"
            //currentBuild.result = 'UNSTABLE'
            currentBuild.result = 'FAILURE'
            error 'There are errors in helm'
          } else {
            echo "HELM FAILURE skipped"
            //error 'There are errors in helm'
          }
        }

        sh "helm list || true"

      //} // withCredentials

    } // dir
  } catch (exc) {
    echo "Warn: There was a problem with pushing helm to \'${HELM_REGISTRY_TMP_URL}\' " + exc.toString()
  }

}
