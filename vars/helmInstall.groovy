#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmInstall.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'packs').toLowerCase().trim()
  //vars.helmChart = vars.get("helmChart", "charts").trim() // helmChart --> dev
  vars.helmRelease = vars.get('helmRelease', vars.helmChartName ?: 'test').trim()

  vars.customRepoName = vars.get('customRepoName', 'custom').trim()

  vars.isHelm2 = vars.get('isHelm2', false).toBoolean()
  vars.skipInstall = vars.get('skipInstall', false).toBoolean()
  vars.isSign = vars.get('isSign', false).toBoolean()
  vars.isInsecure = vars.get('isInsecure', false).toBoolean()
  vars.isAtomic = vars.get('isAtomic', false).toBoolean() // See https://github.com/helm/helm/issues/7426
  vars.isWait = vars.get('isWait', false).toBoolean()
  vars.isDevel = vars.get('isDevel', true).toBoolean()
  vars.isReplace = vars.get('isReplace', true).toBoolean()
  vars.isPackageDependencyUpdate = vars.get('isPackageDependencyUpdate', true).toBoolean()

  vars.isGenerateName = vars.get('isGenerateName', false).toBoolean()
  vars.helmSetOverride = vars.get('helmSetOverride', '').trim() // Allow --set tags.api=false
  vars.helmInstallTimeout = vars.get('helmInstallTimeout', '5m0s').trim()
  vars.helmInstallVersion = vars.get('helmInstallVersion', '').trim()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.helmInstallOutputFile = vars.get('helmInstallOutputFile', "helm-install-${vars.helmFileId}.log").trim()
  vars.isDryRun = vars.get('isDryRun', env.DRY_RUN ?: false).toBoolean()
  vars.skipInstallFailure = vars.get('skipInstallFailure', true).toBoolean()

  if (!vars.skipInstall) {
    try {
      if (body) { body() }

      sh 'helm repo list || true'

      String helmInstallCmd = ''

      if (vars.isHelm2.toBoolean()) {
        helmInstallCmd = "helm install --name ${vars.helmChartName} ${vars.customRepoName}/${vars.helmRelease}"
      } else {
        helmInstallCmd = 'helm install '
        if (vars.KUBECONFIG?.trim()) {
          helmInstallCmd += " --kubeconfig ${vars.KUBECONFIG} "
        }
        if (vars.isGenerateName.toBoolean()) {
          helmInstallCmd += ' --generate-name '
        } else {
          helmInstallCmd += " ${vars.helmRelease} "
        }
        helmInstallCmd += " ${vars.helmDir}/${vars.helmChartName} "
        //helmInstallCmd += " ${vars.customRepoName}/${vars.helmRelease} "
        if (vars.kubeNamespace?.trim()) {
          helmInstallCmd += " --namespace ${vars.kubeNamespace} "
        }
        if (vars.isSign.toBoolean()) {
          helmInstallCmd += ' --verify '
        }
        if (vars.isInsecure.toBoolean()) {
          helmInstallCmd += ' --insecure-skip-tls-verify '
        }
        if (vars.isDryRun.toBoolean()) {
          helmInstallCmd += ' --dry-run '
        }
        if (vars.helmInstallTimeout?.trim()) {
          helmInstallCmd += "--timeout ${vars.helmInstallTimeout} "
        }
        if (vars.isWait.toBoolean()) {
          helmInstallCmd += ' --wait '
        }
        if (vars.isAtomic.toBoolean()) {
          helmInstallCmd += ' --atomic '
        }
        if (vars.isDevel.toBoolean()) {
          helmInstallCmd += ' --devel '
        }
        if (vars.isReplace.toBoolean()) {
          helmInstallCmd += ' --replace '
        }
        if (vars.isPackageDependencyUpdate.toBoolean()) {
          helmInstallCmd += ' --dependency-update '
        }
        if (vars.helmInstallVersion?.trim()) {
          helmInstallCmd += "--version ${vars.helmInstallVersion} "
        }
      }

      if (vars.helmSetOverride?.trim()) {
        helmInstallCmd += vars.helmSetOverride
      }

      // TODO Remove it when tee will be back
      helmInstallCmd += " 2>&1 > ${vars.helmInstallOutputFile} "

      helm = sh (script: helmInstallCmd, returnStatus: true)
      echo "HELM INSTALL RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM INSTALL SUCCESS'
        if (vars.isWait.toBoolean()) {
          helmTest(vars)
      } else {
          echo 'Not waiting for deployment so no helmTest'
        }
      } else {
        echo "WARNING : Helm install failed, check output at \'${env.BUILD_URL}artifact/${vars.helmInstallOutputFile}\' "
        if (!vars.skipInstallFailure) {
          echo 'HELM INSTALL FAILURE'
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm install'
        } else {
          echo 'HELM INSTALL FAILURE skipped'
        //error 'There are errors in helm'
        }
      }
    } catch (exc) {
      echo 'Warn: There was a problem with installing helm ' + exc
    }
  } else {
    echo 'Helm install skipped'
  }
}
