#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmPush.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  def DEBUG_RUN = vars.get('DEBUG_RUN', env.DEBUG_RUN ?: false).toBoolean()

  vars.HELM_REGISTRY_CREDENTIAL = vars.get('HELM_REGISTRY_CREDENTIAL', env.HELM_REGISTRY_CREDENTIAL ?: 'nabla').trim()
  vars.HELM_PROJECT = vars.get('HELM_PROJECT', env.HELM_PROJECT ?: 'nabla').trim()

  vars.JENKINS_HELM_HOME = vars.get('JENKINS_HELM_HOME', env.JENKINS_HELM_HOME ?: '/home/jenkins/.cache/helm').trim()
  vars.HELM_REGISTRY_STABLE_URL = vars.get('HELM_REGISTRY_STABLE_URL', env.HELM_REGISTRY_STABLE_URL ?: 'https://charts.helm.sh/stable').toLowerCase().trim()

  vars.HELM_REGISTRY_TMP = vars.get('HELM_REGISTRY_TMP', env.HELM_REGISTRY_TMP ?: 'albandrieu:6532/harbor').toLowerCase().trim()
  vars.HELM_REGISTRY_REPO_URL = vars.get('HELM_REGISTRY_REPO_URL', env.HELM_REGISTRY_REPO_URL ?: "https://${vars.HELM_REGISTRY_TMP}/chartrepo/${vars.HELM_PROJECT}").trim()

  vars.HELM_REGISTRY_API_REPO_URL = vars.get('HELM_REGISTRY_API_REPO_URL', env.HELM_REGISTRY_API_REPO_URL ?: "https://${vars.HELM_REGISTRY_TMP}/api/chartrepo/${vars.HELM_PROJECT}").trim()
  vars.HELM_REGISTRY_API_URL = vars.get('HELM_REGISTRY_API_URL', env.HELM_REGISTRY_API_URL ?: "${vars.HELM_REGISTRY_API_REPO_URL}/charts").trim()

  //String DRAFT_BRANCH = vars.get("DRAFT_BRANCH", params.DRAFT_BRANCH ?: "develop").trim()
  //String DRAFT_VERSION = vars.get("DRAFT_VERSION", env.DRAFT_VERSION ?: "0.16.0").trim()

  // The directory that contains a chart MUST have the same name as the chart.
  // See https://helm.sh/docs/chart_best_practices/conventions/
  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'packs').toLowerCase().trim()

  vars.helmChartDefaultVersion = vars.get('helmChartDefaultVersion', '0.0.1').trim()
  //vars.pomFile = vars.get("pomFile", "../pom.xml").trim()
  RELEASE_VERSION = helmTag(vars) ?: vars.helmChartDefaultVersion

  vars.helmChartVersion = vars.get('helmChartVersion', RELEASE_VERSION).trim().replaceAll(' ', '-')
  vars.helmChartAppVersionTag = vars.get('helmChartAppVersionTag', vars.helmChartVersion).trim()
  vars.helmChartVersionTag = vars.get('helmChartVersionTag', vars.helmChartVersion).trim()
  vars.helmChartArchive = vars.get('helmChartArchive', "${vars.helmChartName}-${vars.helmChartVersion}.tgz").trim().replaceAll(' ', '-')

  vars.buildDir = vars.get('buildDir', "${pwd()}").trim()

  vars.skipStableRepo = vars.get('skipStableRepo', true).toBoolean()
  vars.stableRepoName = vars.get('stableRepoName', 'stable').trim()
  vars.skipUpdateRepo = vars.get('skipUpdateRepo', false).toBoolean()
  vars.skipCustomRepo = vars.get('skipCustomRepo', false).toBoolean()
  vars.customRepoName = vars.get('customRepoName', 'custom').trim()
  vars.isDelete = vars.get('isDelete', false).toBoolean()
  vars.isInstall = vars.get('isInstall', false).toBoolean()
  vars.isProvenance = vars.get('isProvenance', false).toBoolean()
  vars.isSign = vars.get('isSign', false).toBoolean()
  vars.skipInsecure = vars.get('skipInsecure', true).toBoolean()
  vars.isHelmCurl = vars.get('isHelmCurl', true).toBoolean()
  vars.isHelmPush = vars.get('isHelmPush', false).toBoolean()
  vars.isHelmDeploy = vars.get('isHelmDeploy', true).toBoolean()
  vars.isHelmDeployForce = vars.get('isHelmDeployForce', false).toBoolean()
  vars.isTemplate = vars.get('isTemplate', false).toBoolean()
  vars.isPackageDependencyUpdate = vars.get('isPackageDependencyUpdate', true).toBoolean()
  vars.isDependencyUpdate = vars.get('isDependencyUpdate', true).toBoolean()
  vars.isDependencyBuild = vars.get('isDependencyBuild', false).toBoolean()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()

  //vars.helmCaFile = vars.get("helmCaFile", "/usr/local/share/ca-certificates/UK1VSWCERT01-CA-5.crt").trim()
  //vars.helmCaFile = vars.get("helmCaFile", "/root/pki/ca-old.pem").trim()
  vars.HELM_REPO_CA_FILE = vars.get('HELM_REPO_CA_FILE', '').trim()
  vars.HELM_REPO_CONTEXT_PATH = vars.get('HELM_REPO_CONTEXT_PATH', '').trim()  // empty or chartmuseum or charts

  vars.helmPackageOutputFile = vars.get('helmPackageOutputFile', "helm-package-${vars.helmFileId}.log").trim()
  vars.helmPushOutputFile = vars.get('helmPushOutputFile', "helm-push-${vars.helmFileId}.log").trim()
  vars.helmTemplateOutputFile = vars.get('helmTemplateOutputFile', "helm-template-${vars.helmFileId}.yml").trim()
  vars.helmRepoAddOutputFile = vars.get('helmRepoAddOutputFile', "helm-repo-add-${vars.helmFileId}.log").trim()
  vars.skipHelmPackageFailure = vars.get('skipHelmPackageFailure', false).toBoolean()
  vars.skipHelmPushFailure = vars.get('skipHelmPushFailure', false).toBoolean()

  try {
    echo "Using : ${vars.HELM_PROJECT} - ${vars.HELM_REGISTRY_API_URL} from : ${vars.buildDir}"

    dir("${vars.buildDir}") {
      //def helmHome = sh(returnStdout: true, script: 'rm -rf .helm/ && mkdir .helm && cd .helm && pwd').trim()
      //String helmHome = sh(returnStdout: true, script: 'mkdir .helm && cd .helm && pwd').trim()
      String helmHome = vars.JENKINS_HELM_HOME.trim()
      withEnv(["HELM_HOME=${helmHome}"]) {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: vars.HELM_REGISTRY_CREDENTIAL, usernameVariable: 'HELM_REPO_USERNAME', passwordVariable: 'HELM_REPO_PASSWORD']]) {
          if (DEBUG_RUN) {
            sh """#!/bin/bash -l
            echo "HELM_HOME: ${HELM_HOME}"
            helm version || true
            """
          }

          helmRepo(vars)

          helmLint(vars)

          if (body) { body() }

          if (vars.isTemplate.toBoolean()) {
            sh "helm template ${vars.helmDir}/${vars.helmChartName} 2>&1 > ${vars.helmTemplateOutputFile}"
          }

          helmVersion(vars)

          if (!vars.isPackageDependencyUpdate.toBoolean()) {
            helmDependency(vars)
          }

          helmDeploy(vars)

          String helmPackageCmd = "helm package ${vars.helmDir}/${vars.helmChartName} --version ${vars.helmChartVersionTag} --app-version ${vars.helmChartAppVersionTag}"
          if (vars.isPackageDependencyUpdate.toBoolean()) {
            helmPackageCmd += ' --dependency-update '
          }
          if (vars.isSign.toBoolean()) {
            helmPackageCmd += ' --sign '
          }

          // TODO Remove it when tee will be back
          helmPackageCmd += " 2>&1 > ${vars.helmPackageOutputFile} "

          echo "helmPackageCmd : ${helmPackageCmd}"

          // https://github.com/helm/chartmuseum
          helmPackageResult = sh (script: helmPackageCmd, returnStatus: true)
          echo "HELM PACKAGE RETURN CODE : ${helmPackageResult}"
          if (helmPackageResult == 0) {
            echo 'HELM PACKAGE SUCCESS'
          } else {
            echo "WARNING : Helm package failed, check output at \'${env.BUILD_URL}artifact/${vars.helmPackageOutputFile}\' "
            if (!vars.skipHelmPackageFailure) {
              echo 'HELM PACKAGE FAILURE'
              //currentBuild.result = 'UNSTABLE'
              currentBuild.result = 'FAILURE'
              error 'There are errors in helm package'
            } else {
              echo 'HELM PACKAGE FAILURE skipped'
            //error 'There are errors in helm package'
            }
          }

          if (DEBUG_RUN) {
            sh """#!/bin/bash -l
            ls -lrta ${HELM_HOME}/repository/ || true
            helm verify ${vars.helmDir}/${vars.helmChartName} || true
            helm repo index . || true
            cat index.yaml || true"""
          }

          String chart = "-F chart=@${vars.helmChartArchive}".trim()
          String provenance = ''
          // https://github.com/helm/helm-www/blob/master/content/en/docs/topics/provenance.md
          if (vars.isProvenance) {
            provenance = "-F prov=@${vars.helmChartArchive}.prov".trim()
          }

          String helmPushCmd = 'echo NoDeploy'

          if (vars.isHelmCurl) {
            helmPushCmd = "curl --insecure -u ${HELM_REPO_USERNAME}:${HELM_REPO_PASSWORD} ${chart} ${provenance} ${vars.HELM_REGISTRY_API_URL}"
          }

          // https://github.com/chartmuseum/helm-push
          if (vars.isHelmPush) {
            //  --version=\"$(git log -1 --pretty=format:%h)\"
            helmPushCmd = "helm push ${vars.helmDir}/${vars.helmChartName} ${vars.customRepoName} --version ${vars.helmChartVersionTag} --username ${HELM_REPO_USERNAME} --password ${HELM_REPO_PASSWORD} --home ${HELM_HOME} "
            if (!vars.skipInsecure) {
              helmPushCmd += ' --insecure '
            }
            if (vars.HELM_REPO_CA_FILE?.trim()) {
              helmPushCmd += " --ca-file=${vars.HELM_REPO_CA_FILE} "
            }
            if (vars.HELM_REPO_CONTEXT_PATH?.trim()) {
              helmPushCmd += " --context-path /${vars.HELM_REPO_CONTEXT_PATH}"
            }
          }

          // TODO Remove it when tee will be back
          helmPushCmd += " 2>&1 > ${vars.helmPushOutputFile} "

          echo "helmPushCmd : ${helmPushCmd}"

          if ((isReleaseBranch() && vars.isHelmDeploy) || vars.isHelmDeployForce) {
            retry(3) {
              helmPushResult = sh (script: helmPushCmd, returnStatus: true)
            } // retry
            echo "HELM PUSH RETURN CODE : ${helmPushResult}"
            if (helmPushResult == 0) {
              echo 'HELM PUSH SUCCESS'
              sh "echo \"Chart : ${chart} pushed to ${vars.HELM_REGISTRY_API_URL}\"  2>&1 > ${vars.helmPushOutputFile} || true"
              println hudson.console.ModelHyperlinkNote.encodeTo(env.BUILD_URL + 'artifact/${vars.helmPushOutputFile}', "${vars.helmPushOutputFile}")
            } else {
              echo "WARNING : Helm push failed, check output at \'${env.BUILD_URL}artifact/${vars.helmPushOutputFile}\' "
              if (!vars.skipHelmPushFailure) {
                echo 'HELM PUSH FAILURE'
                //currentBuild.result = 'UNSTABLE'
                currentBuild.result = 'FAILURE'
                error 'There are errors in helm push'
              } else {
                echo 'HELM PUSH FAILURE skipped'
              //error 'There are errors in helm push'
              }
            }
          } else {
            echo "No push on none release branches - ${vars.isHelmDeploy} : ${helmPushCmd} 2>&1 > ${vars.helmPushOutputFile}"
          }

          if (!vars.skipCustomRepo) {
            sh "helm search repo ${vars.customRepoName}/${vars.helmChartName} || true"
          }
        } // withCredentials
      } // HELM_HOME
    } // dir
  } catch (exc) {
    echo "Warn: There was a problem with pushing helm to \'${vars.HELM_REGISTRY_API_URL}\' " + exc.toString()
  } finally {
    cleanEmptyFile(vars)
    archiveArtifacts artifacts: "**/helm-*.log, ${vars.helmTemplateOutputFile}, ${vars.helmPushOutputFile}, ${vars.helmPackageOutputFile}, ${vars.helmChartArchive}", onlyIfSuccessful: false, allowEmptyArchive: true
  }
}
