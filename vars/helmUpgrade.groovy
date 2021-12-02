#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmUpgrade.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'charts').toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get('helmRelease', vars.helmChartName ?: 'test').trim()

  vars.customRepoName = vars.get('customRepoName', 'custom').trim()

  vars.skipUpgrade = vars.get('skipUpgrade', false).toBoolean()
  vars.isSign = vars.get('isSign', false).toBoolean()
  vars.isInsecure = vars.get('isInsecure', false).toBoolean()
  vars.isWait = vars.get('isWait', true).toBoolean()
  vars.isAtomic = vars.get('isAtomic', true).toBoolean()
  vars.isDevel = vars.get('isDevel', true).toBoolean()
  vars.isForce = vars.get('isForce', true).toBoolean()

  vars.isGenerateName = vars.get('isGenerateName', false).toBoolean()
  vars.helmSetOverride = vars.get('helmSetOverride', '').trim() // Allow --set tags.api=false
  vars.helmUpgradeTimeout = vars.get('helmUpgradeTimeout', '60s').trim()
  vars.helmUpgradeVersion = vars.get('helmUpgradeVersion', '').trim()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.helmUpgradeOutputFile = vars.get('helmUpgradeOutputFile', "helm-upgrade-${vars.helmFileId}.log").trim()
  vars.isDryRun = vars.get('isDryRun', env.DRY_RUN ?: false).toBoolean()
  vars.skipUpgradeFailure = vars.get('skipUpgradeFailure', true).toBoolean()

  if (!vars.skipUpgrade) {
    try {
      if (body) { body() }

      String helmUpgradeCmd = ''

      helmUpgradeCmd = 'helm upgrade  '
      if (vars.KUBECONFIG?.trim()) {
        helmUpgradeCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      helmUpgradeCmd += ' --install '
      if (vars.isGenerateName.toBoolean()) {
        helmUpgradeCmd += ' --generate-name '
      } else {
        helmUpgradeCmd += " ${vars.helmRelease} "
      }
      helmUpgradeCmd += " ${vars.helmDir}/${vars.helmChartName} "
      //helmUpgradeCmd += " ${vars.customRepoName}/${vars.helmRelease} "
      if (vars.kubeNamespace?.trim()) {
        helmUpgradeCmd += " --namespace ${vars.kubeNamespace} "
      }
      if (vars.isSign.toBoolean()) {
        helmUpgradeCmd += ' --verify '
      }
      if (vars.isInsecure.toBoolean()) {
        helmUpgradeCmd += ' --insecure-skip-tls-verify '
      }
      if (vars.isDryRun.toBoolean()) {
        helmUpgradeCmd += ' --dry-run '
      }
      if (vars.helmUpgradeTimeout?.trim()) {
        helmUpgradeCmd += "--timeout ${vars.helmUpgradeTimeout} "
      }
      if (vars.isWait.toBoolean()) {
        helmUpgradeCmd += ' --wait '
      }
      if (vars.isAtomic.toBoolean()) {
        helmUpgradeCmd += ' --atomic '
      }
      if (vars.isDevel.toBoolean()) {
        helmUpgradeCmd += ' --devel '
      }
      if (vars.isForce.toBoolean()) {
        helmUpgradeCmd += ' --force '
      }
      if (vars.helmUpgradeVersion?.trim()) {
        helmUpgradeCmd += "--version ${vars.helmUpgradeVersion} "
      }

      if (vars.helmSetOverride?.trim()) {
        helmUpgradeCmd += vars.helmSetOverride
      }

      // TODO Remove it when tee will be back
      helmUpgradeCmd += " 2>&1 > ${vars.helmUpgradeOutputFile} "

      helm = sh (script: helmUpgradeCmd, returnStatus: true)
      echo "HELM UPGRADE RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM UPGRADE SUCCESS'
      } else {
        echo "WARNING : Helm install failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmUpgradeOutputFile}\' "
        if (!vars.skipUpgradeFailure) {
          echo 'HELM UPGRADE FAILURE'
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm upgrade'
        } else {
          echo 'HELM UPGRADE FAILURE skipped'
        //error 'There are errors in helm'
        }
      }
    } catch (exc) {
      echo 'Warn: There was a problem with upgrading helm ' + exc
    }
  } else {
    echo 'Helm upgrade skipped'
  }
}
