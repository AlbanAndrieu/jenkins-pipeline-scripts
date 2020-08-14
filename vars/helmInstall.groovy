#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmInstall.groovy"

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

  vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get("helmRelease", "test").trim()
  vars.buildDir = vars.get("buildDir", "${pwd()}").trim()

  vars.skipStableRepo = vars.get("skipStableRepo", true).toBoolean()
  vars.stableRepoName = vars.get("stableRepoName", "stable").trim()
  vars.skipCustomRepo = vars.get("skipCustomRepo", false).toBoolean()
  vars.customRepoName = vars.get("customRepoName", "custom").trim()
  vars.isHelm2 = vars.get("isHelm2", false).toBoolean()
  vars.isInstall = vars.get("isInstall", false).toBoolean()
  vars.isHarbor = vars.get("isHarbor", true).toBoolean()
  vars.isSign = vars.get("isSign", false).toBoolean()
  vars.isDryRun = vars.get("isDryRun", false).toBoolean()
  vars.helmSetOverride = vars.get("helmSetOverride", "").trim() // Allow --set tags.api=false

  vars.helmInstallOutputFile = vars.get("helmInstallOutputFile", "helm-install.log").trim()
  vars.helmDryRunOutputFile = vars.get("helmDryRunOutputFile", "helm-install-debug.log").trim()
  vars.skipFailure = vars.get("skipFailure", true).toBoolean()

  try {
    echo "Using ${HELM_REGISTRY_TMP_URL}"

    dir("${vars.buildDir}") {

      //withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: HELM_REGISTRY_CREDENTIAL, usernameVariable: 'HELM_USERNAME', passwordVariable: 'HELM_PASSWORD']]) {

        if (body) { body() }

        if (vars.isDryRun.toBoolean()) {
          sh """#!/bin/bash -l
          helm install ${vars.helmChart} --dry-run --debug 2>&1 > ${vars.helmDryRunOutputFile}"""
          //sh "helm install $${vars.customRepoName}/${vars.helmRelease} --dry-run --debug 2>&1 > ${vars.helmDryRunOutputFile}"
        }

        String helmInstallCmd = ""

        // Install chart from repo
        // https://github.com/goharbor/harbor-helm
        if (vars.isHarbor.toBoolean()) {
          if (vars.isInstall.toBoolean()) {
            if (vars.isHelm2.toBoolean()) {
              helmInstallCmd = "helm install --name ${vars.helmChart} ${vars.customRepoName}/${vars.helmRelease}"
            } else {
              if (vars.isSign) {
                helmInstallCmd = "helm install ${vars.helmChart} ${vars.customRepoName}/${vars.helmRelease} --verify"
              } else {
                helmInstallCmd = "helm install ${vars.helmChart} ${vars.customRepoName}/${vars.helmRelease}"
              }
            }
          } // isInstall
        } // isHarbor

		helmInstallCmd += vars.helmSetOverride

        // TODO Remove it when tee will be back
        helmInstallCmd += " 2>&1 > ${vars.helmInstallOutputFile} "

        // https://github.com/helm/chartmuseum
        helm = sh (script: helmInstallCmd, returnStatus: true)
        echo "HELM RETURN CODE : ${helm}"
        if (helm == 0) {
          echo "HELM SUCCESS"
        } else {
          echo "WARNING : Helm install failed, check output at \'${vars.helmInstallOutputFile}\' "
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
