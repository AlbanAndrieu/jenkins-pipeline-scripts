#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Allow to define other external repositories.</h1>
 * Add other external repositories alias.
 *
 * <b>Note:</b> This is product specific.
 *
 * @param nablaRepoName Allow to override the repository alias. By default <code>@nabla-test</code>.
 * @param skipNablaRepo Skip adding the nabla repository.
 * @param skipUpdateRepo Allow to update repository.
 * @param skipHelmRepoExternal Skip the repository definition. If true and skipNablaRepo true, repository will be removed (and not added back)
 */
def call(Map vars, Closure body=null) {

  echo '[JPL] Executing `vars/helmRepoExternal.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.HELM_KUBECONTEXT = k8sCluster(vars)

  vars.HELM_PROJECT = vars.get('HELM_PROJECT', env.HELM_PROJECT ?: 'nabla').trim()

  vars.HELM_REGISTRY_TMP = vars.get('HELM_REGISTRY_TMP', env.HELM_REGISTRY_TMP ?: 'registry-tmp.albandrieu.com').toLowerCase().trim()
  vars.HELM_NABLA_PROJECT = vars.get('HELM_NABLA_PROJECT', env.HELM_NABLA_PROJECT ?: vars.HELM_PROJECT).trim()
  vars.HELM_REGISTRY_NABLA_URL = vars.get('HELM_REGISTRY_NABLA_URL', env.HELM_REGISTRY_NABLA_URL ?: "https://${vars.HELM_REGISTRY_TMP}/chartrepo/${vars.HELM_NABLA_PROJECT}").toLowerCase().trim()

  vars.HELM_REGISTRY_NABLA_EXTERNAL_URL = vars.get('HELM_REGISTRY_NABLA_EXTERNAL_URL', env.HELM_REGISTRY_NABLA_URL ?: "https://${vars.HELM_REGISTRY_TMP}/chartrepo/${vars.HELM_NABLA_PROJECT}").toLowerCase().trim()
  // HELM_REGISTRY_NABLA_OTHER_URL for other hard coded, not control charts
  vars.HELM_REGISTRY_NABLA_OTHER_URL = vars.get('HELM_REGISTRY_NABLA_OTHER_URL', env.HELM_REGISTRY_NABLA_URL ?: "https://${vars.HELM_REGISTRY_TMP}/chartrepo/${vars.HELM_NABLA_PROJECT}").toLowerCase().trim()

  vars.HELM_REPO_CA_FILE = vars.get('HELM_REPO_CA_FILE', '').trim()

  vars.skipInsecure = vars.get('skipInsecure', true).toBoolean()
  vars.skipForceUpdate = vars.get('skipForceUpdate', false).toBoolean()

  // This alias is aimed to replace @custom alias, used for test
  vars.nablaRepoName = vars.get('nablaRepoName', 'nabla').trim()
  vars.skipNablaRepo = vars.get('skipNablaRepo', false).toBoolean()

  // Like @nabla for other naming convention
  // Below external (and custom) alias will be replaced by nablaRepoName @nabla-test

  // nabla-test ${HELM_REGISTRY_REPO_URL}

  vars.skipUpdateRepo = vars.get('skipUpdateRepo', true).toBoolean()

  vars.skipHelmRepoExternal = vars.get('skipHelmRepoExternal', false).toBoolean()

  if (!vars.skipHelmRepoExternal) {
    try {
      //sh """#!/bin/bash -l
      //helm --debug --kubeconfig ${vars.KUBECONFIG} repo add bitnami https://charts.bitnami.com/bitnami --force-update"
      //helm --debug --kubeconfig ${vars.KUBECONFIG} repo add jenkins https://charts.jenkins.io --force-update"
      //"""

      String helmRepoExternalAddCmd = 'helm '

      if (isDebugRun(vars)) {
        helmRepoExternalAddCmd += ' --debug '
      } // isDebugRun
      if (vars.KUBECONFIG?.trim()) {
        helmRepoExternalAddCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.HELM_KUBECONTEXT?.trim()) {
        helmRepoExternalAddCmd += " --kube-context ${vars.HELM_KUBECONTEXT} "
      }
      if (!vars.skipForceUpdate) {
        helmRepoExternalAddCmd += ' --force-update '
      }
      if (vars.skipInsecure.toBoolean()) {
        helmRepoExternalAddCmd += ' --insecure-skip-tls-verify '
      }
      if (vars.HELM_REPO_CA_FILE?.trim()) {
        helmRepoExternalAddCmd += " --ca-file=${vars.HELM_REPO_CA_FILE} "
      }

      sh """#!/bin/bash -l
      helm repo remove null || true
      helm repo remove nabla-test || true

      ${helmRepoExternalAddCmd} repo add nabla-test ${vars.HELM_REGISTRY_NABLA_EXTERNAL_URL}

      """

      if (!vars.skipNablaRepo && vars.nablaRepoName?.trim()) {
        echo "Using repo : ${vars.HELM_REGISTRY_NABLA_URL}/index.yaml"
        sh "helm repo remove ${vars.nablaRepoName} || true"
        sh "${helmRepoExternalAddCmd} repo add \"${vars.nablaRepoName}\" \"${vars.HELM_REGISTRY_NABLA_URL}\" "
      }
      // Should not be empty
      //sh "ls -lrta ${HELM_HOME} ${HELM_HOME}/repository/ || true"

      if (!vars.skipUpdateRepo) {
        sh 'helm --debug repo update || true'
      } // skipUpdateRepo

      if (body) { body() }

      sh '''#!/bin/bash -l
      helm repo list --output table || true
      helm repo list --output yaml > helm-repositories-external.yaml || true
      ls -lrta ~/.config/helm/repositories.yaml '''
    } catch (exc) {
      echo 'Warn: There was a problem with using helm repo external ' + exc
    } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: 'helm-repositories-external.yaml', onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'Helm repo external skipped'
  }
}
