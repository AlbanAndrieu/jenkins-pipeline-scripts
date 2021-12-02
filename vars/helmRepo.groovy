#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmRepo.groovy`'

  vars = vars ?: [:]

  def DEBUG_RUN = vars.get('DEBUG_RUN', env.DEBUG_RUN ?: false).toBoolean()

  vars.HELM_PROJECT = vars.get('HELM_PROJECT', env.HELM_PROJECT ?: 'nabla').trim()

  vars.HELM_REGISTRY_TMP = vars.get('HELM_REGISTRY_TMP', env.HELM_REGISTRY_TMP ?: 'registry.hub.docker.com').toLowerCase().trim()
  vars.HELM_REGISTRY_REPO_URL = vars.get('HELM_REGISTRY_REPO_URL', env.HELM_REGISTRY_REPO_URL ?: "https://${vars.HELM_REGISTRY_TMP}/chartrepo/${vars.HELM_PROJECT}").trim()

  vars.HELM_REGISTRY_STABLE_URL = vars.get('HELM_REGISTRY_STABLE_URL', env.HELM_REGISTRY_STABLE_URL ?: 'https://charts.helm.sh/stable').toLowerCase().trim()

  vars.HELM_REPO_CA_FILE = vars.get('HELM_REPO_CA_FILE', '').trim()

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'jenkins').trim()

  vars.skipStableRepo = vars.get('skipStableRepo', true).toBoolean()
  vars.stableRepoName = vars.get('stableRepoName', 'stable').trim()
  vars.skipUpdateRepo = vars.get('skipUpdateRepo', false).toBoolean()
  vars.skipCustomRepo = vars.get('skipCustomRepo', false).toBoolean()
  vars.customRepoName = vars.get('customRepoName', 'custom').trim()

  vars.skipHelmRepo = vars.get('skipHelmRepo', false).toBoolean()

  //if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
  //    vars.skipHelmRepo = true
  //}

  if (!vars.skipHelmRepo) {
    try {
      echo "Using repo : ${vars.HELM_REGISTRY_REPO_URL}/index.yaml"

      if (DEBUG_RUN) {
        sh '''#!/bin/bash -l
        helm help registry
        mkdir -p /home/jenkins/.helm
        mkdir -p /home/jenkins/.config/helm
        ls -lrta /home/jenkins/.cache/helm/repository/ || true
        ls -lrta /home/jenkins/.helm/ /home/jenkins/.helm/repository/ || true
        ls -ltra /home/jenkins/.config/helm/ || true
        '''
      }

      // Remove https://kubernetes-charts.storage.googleapis.com/ if any
      if (!vars.skipStableRepo) {
        sh "helm repo add \"${vars.stableRepoName}\" \"${vars.HELM_REGISTRY_STABLE_URL}\" --force-update"
      } else {
        sh "helm repo remove ${vars.stableRepoName} || true"
      }

      //sh "ls -lrta /home/jenkins/.cache/helm /home/jenkins/.cache/helm/repository/ || true"

      if (!vars.skipCustomRepo) {
        sh "helm repo remove ${vars.customRepoName} || true"
        String helmRepoAddCmd = 'helm --debug'
        if (!vars.skipInsecure) {
          helmRepoAddCmd += ' --insecure-skip-tls-verify '
        }
        if (vars.HELM_REPO_CA_FILE?.trim()) {
          helmRepoAddCmd += " --ca-file=${vars.HELM_REPO_CA_FILE} "
        }
        if (vars.KUBECONFIG?.trim()) {
          helmRepoAddCmd += " --kubeconfig ${vars.KUBECONFIG} "
        }
        helmRepoAddCmd += " repo add ${vars.customRepoName} ${vars.HELM_REGISTRY_REPO_URL} --force-update "
        echo "${helmRepoAddCmd} 2>&1 > ${vars.helmRepoAddOutputFile}"
        sh """#!/bin/bash -l
        ${helmRepoAddCmd} || true
        helm search repo ${vars.customRepoName}/${vars.helmChartName} --devel || true"""
      } else {
        sh "helm repo remove ${vars.customRepoName} || true"
      }
      if (!vars.skipStableRepo && !vars.skipCustomRepo) {
        echo 'No repo at all!'
        // Should not be empty
        sh "ls -lrta ${HELM_HOME} ${HELM_HOME}/repository/ || true"
      } else {
        if (!vars.skipUpdateRepo) {
          sh 'helm --debug repo update || true'
        }
      }

      if (DEBUG_RUN) {
        sh '''#!/bin/bash -l
        helm repo list --output table || true
        helm repo list --output yaml > helm-repositories.yaml || true
        ls -lrta ~/.config/helm/repositories.yaml '''
      }

      if (body) { body() }
    } catch (exc) {
      echo 'Warn: There was a problem with using helm repo ' + exc
  } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: "repositories.yml,${vars.helmRepoAddOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'Helm repo skipped'
  }
}
