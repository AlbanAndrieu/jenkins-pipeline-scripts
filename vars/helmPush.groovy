#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmPush.groovy"

  vars = vars ?: [:]

  String HELM_REGISTRY_CREDENTIAL = vars.get("HELM_REGISTRY_CREDENTIAL", env.HELM_REGISTRY_CREDENTIAL ?: "nabla").trim()

  String HELM_PROJECT = vars.get("HELM_PROJECT", env.HELM_PROJECT ?: "nabla").trim()

  vars.JENKINS_HELM_HOME = vars.get("JENKINS_HELM_HOME", env.JENKINS_HELM_HOME ?: "/home/jenkins/.cache/helm").trim()
  String HELM_REGISTRY_STABLE_URL = vars.get("HELM_REGISTRY_STABLE_URL", env.HELM_REGISTRY_STABLE_URL ?: "https://charts.helm.sh/stable").toLowerCase().trim()

  String HELM_REGISTRY_TMP = vars.get("HELM_REGISTRY_TMP", env.HELM_REGISTRY_TMP ?: "albandrieu:6532/harbor").toLowerCase().trim()
  String HELM_REGISTRY_REPO_URL = vars.get("HELM_REGISTRY_REPO_URL", env.HELM_REGISTRY_REPO_URL ?: "https://${HELM_REGISTRY_TMP}/chartrepo/${HELM_PROJECT}").trim()

  String HELM_REGISTRY_API_REPO_URL = vars.get("HELM_REGISTRY_API_REPO_URL", env.HELM_REGISTRY_API_REPO_URL ?: "https://${HELM_REGISTRY_TMP}/api/chartrepo/${HELM_PROJECT}").trim()
  String HELM_REGISTRY_API_URL = vars.get("HELM_REGISTRY_API_URL", env.HELM_REGISTRY_API_URL ?: "${HELM_REGISTRY_API_REPO_URL}/charts").trim()

  //String DRAFT_BRANCH = vars.get("DRAFT_BRANCH", params.DRAFT_BRANCH ?: "develop").trim()
  //String DRAFT_VERSION = vars.get("DRAFT_VERSION", env.DRAFT_VERSION ?: "0.16.0").trim()

  vars.helmDir = vars.get("helmDir", "./charts").trim()
  vars.helmChartName = vars.get("helmChartName", vars.draftPack ?: "charts").trim()

  vars.pomFile = vars.get("pomFile", "../pom.xml").trim()
  RELEASE_VERSION = helmTag(vars) ?: "0.0.1"

  vars.helmChartVersion = vars.get("helmChartVersion", RELEASE_VERSION).trim().replaceAll(' ','-')
  vars.helmChartArchive = vars.get("helmChartArchive", "${vars.helmChartName}-${vars.helmChartVersion}.tgz").trim().replaceAll(' ','-')

  vars.buildDir = vars.get("buildDir", "${pwd()}").trim()

  vars.skipStableRepo = vars.get("skipStableRepo", true).toBoolean()
  vars.stableRepoName = vars.get("stableRepoName", "stable").trim()
  vars.skipUpdateRepo = vars.get("skipUpdateRepo", false).toBoolean()
  vars.skipCustomRepo = vars.get("skipCustomRepo", false).toBoolean()
  vars.customRepoName = vars.get("customRepoName", "custom").trim()
  vars.isHelm2 = vars.get("isHelm2", false).toBoolean()
  vars.isDelete = vars.get("isDelete", false).toBoolean()
  vars.isInstall = vars.get("isInstall", false).toBoolean()
  vars.isHarbor = vars.get("isHarbor", true).toBoolean()
  vars.isProvenance = vars.get("isProvenance", false).toBoolean()
  vars.isSign = vars.get("isSign", false).toBoolean()
  vars.skipInsecure = vars.get("skipInsecure", true).toBoolean()
  vars.isHelmCurl = vars.get("isHelmCurl", true).toBoolean()
  vars.isHelmPush = vars.get("isHelmPush", false).toBoolean()
  vars.isHelmDeploy = vars.get("isHelmDeploy", true).toBoolean()
  vars.isTemplate = vars.get("isTemplate", true).toBoolean()
  vars.isDependencyUpdate = vars.get("isDependencyUpdate", true).toBoolean()
  vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()

  //vars.helmCaFile = vars.get("helmCaFile", "/usr/local/share/ca-certificates/UK1VSWCERT01-CA-5.crt").trim()
  //vars.helmCaFile = vars.get("helmCaFile", "/root/pki/ca-old.pem").trim()
  vars.HELM_REPO_CA_FILE = vars.get("HELM_REPO_CA_FILE", "").trim()
  vars.HELM_REPO_CONTEXT_PATH = vars.get("HELM_REPO_CONTEXT_PATH", "").trim()  // empty or chartmuseum or charts

  vars.helmPackageOutputFile = vars.get("helmPackageOutputFile", "helm-package-${vars.helmFileId}.log").trim()
  vars.helmPushOutputFile = vars.get("helmPushOutputFile", "helm-push-${vars.helmFileId}.log").trim()
  vars.helmTemplateOutputFile = vars.get("helmTemplateOutputFile", "helm-template-${vars.helmFileId}.yml").trim()
  vars.helmRepoAddOutputFile = vars.get("helmRepoAddOutputFile", "helm-repo-add-${vars.helmFileId}.log").trim()
  vars.skipHelmPushFailure = vars.get("skipHelmPushFailure", false).toBoolean()

  try {
    echo "Using api : ${HELM_REGISTRY_API_URL} from : ${vars.buildDir}"
    echo "Using repo : ${HELM_REGISTRY_REPO_URL}/index.yaml"

    dir("${vars.buildDir}") {

      // See https://scm-git-eur.misys.global.ad/projects/MD/repos/jenkins-pipeline-library-kondor/browse/vars/kondorBuildServices.groovy#193

      //def helmHome = sh(returnStdout: true, script: 'rm -rf .helm/ && mkdir .helm && cd .helm && pwd').trim()
      //String helmHome = sh(returnStdout: true, script: 'mkdir .helm && cd .helm && pwd').trim()
      String helmHome = vars.JENKINS_HELM_HOME.trim()
      withEnv(["HELM_HOME=${helmHome}"]) {

        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: HELM_REGISTRY_CREDENTIAL, usernameVariable: 'HELM_REPO_USERNAME', passwordVariable: 'HELM_REPO_PASSWORD']]) {

          sh """#!/bin/bash -l
          echo "HELM_HOME: ${HELM_HOME}"
          helm version || true
          helm repo --help
          mkdir -p /home/jenkins/.helm
          mkdir -p /home/jenkins/.config/helm
          ls -lrta /home/jenkins/.cache/helm/repository/ || true"""

          sh "ls -lrta /home/jenkins/.helm/ /home/jenkins/.helm/repository/ || true"
          sh "ls -ltra /home/jenkins/.config/helm/ || true"

       		// Remove https://kubernetes-charts.storage.googleapis.com/ if any
          if (!vars.skipStableRepo) {
            sh "helm repo add \"${vars.stableRepoName}\" \"${HELM_REGISTRY_STABLE_URL}\" --force-update"
          } else {
        		sh "helm repo remove ${vars.stableRepoName} || true"
       		}


          sh "ls -lrta /home/jenkins/.cache/helm /home/jenkins/.cache/helm/repository/ || true"

          if (!vars.skipCustomRepo) {
            //sh "echo TODO > /home/jenkins/.config/helm/repositories.yaml"
            sh "helm repo remove ${vars.customRepoName} || true"
            String helmRepoAddCmd = "helm --debug"
            //--username ${HELM_REPO_USERNAME} --password ${HELM_REPO_PASSWORD}
            if (!vars.skipInsecure) {
                helmRepoAddCmd +=" --insecure-skip-tls-verify "
            }
            if (vars.HELM_REPO_CA_FILE?.trim()) {
                helmRepoAddCmd +=" --ca-file=${vars.HELM_REPO_CA_FILE} "
            }
            if (vars.KUBECONFIG?.trim()) {
                helmRepoAddCmd +=" --kubeconfig ${vars.KUBECONFIG} "
            }
            helmRepoAddCmd +=" repo add ${vars.customRepoName} ${HELM_REGISTRY_REPO_URL} "
            echo "${helmRepoAddCmd} 2>&1 > ${vars.helmRepoAddOutputFile}"
            sh """#!/bin/bash -l
            ${helmRepoAddCmd} || true
            helm search repo ${vars.helmChartName} || true"""
          } else {
            sh "helm repo remove ${vars.customRepoName} || true"
          }
          if (!vars.skipStableRepo && !vars.skipCustomRepo) {
              echo "No repo at all!"
              // Should not be empty
              sh "ls -lrta ${HELM_HOME} ${HELM_HOME}/repository/ || true"
          } else {
              if (!vars.skipUpdateRepo ) {
                sh "helm --debug repo update || true"
              }
          }

          sh """#!/bin/bash -l
          helm repo list --output table || true
          helm repo list --output yaml > helm-repositories.yaml || true
          ls -lrta ~/.config/helm/repositories.yaml """

		      helmLint(vars, body)

		      //NO KUBECTL on Docker
		      //sh "kubectl get configmaps --namespace=kube-system || true"
		      //The connection to the server localhost:8080 was refused - did you specify the right host or port?

		      if (vars.isTemplate.toBoolean()) {
		        sh "helm template ${vars.helmDir}/${vars.helmChartName} 2>&1 > ${vars.helmTemplateOutputFile}"
		      }

		      //helm create ${vars.helmChartName}
		      // helm upgrade

					// if requirements.yml
					if (vars.isDependencyUpdate.toBoolean()) {
						sh "helm dependency update ${vars.helmDir}/${vars.helmChartName} || true"
					} else {
						sh "helm dependency build ${vars.helmDir}/${vars.helmChartName} || true"
					}
					sh """#!/bin/bash -l
					cat requirements.lock || true
					helm dependency list ${vars.helmDir}/${vars.helmChartName} || true"""

		      //sh "helm dep build ${vars.helmDir}/${vars.helmChartName} || true"
		      //
		      String helmPackageCmd = "helm package ${vars.helmDir}/${vars.helmChartName} --version ${vars.helmChartVersion} --app-version ${vars.helmChartVersion}"
					if (vars.isDependencyUpdate.toBoolean()) {
						helmPackageCmd += " --dependency-update "
					}
		      if (vars.isSign.toBoolean()) {
		        helmPackageCmd += " --sign "
		      }

		      // TODO Remove it when tee will be back
		      helmPackageCmd += " 2>&1 > ${vars.helmPackageOutputFile} "

		      echo "helmPackageCmd : ${helmPackageCmd}"

		      // https://github.com/helm/chartmuseum
		      helmPackageResult = sh (script: helmPackageCmd, returnStatus: true)
		      echo "HELM RETURN CODE : ${helmPackageResult}"
		      if (helmPackageResult == 0) {
		        echo "HELM PACKAGE SUCCESS"
		      } else {
		        echo "WARNING : Helm package failed, check output at \'${vars.helmPackageOutputFile}\' "
		        if (!vars.skipHelmPushFailure) {
		          echo "HELM PACKAGE FAILURE"
		          //currentBuild.result = 'UNSTABLE'
		          currentBuild.result = 'FAILURE'
		          error 'There are errors in helm package'
		        } else {
		          echo "HELM PACKAGE FAILURE skipped"
		          //error 'There are errors in helm package'
		        }
		      }

					sh """#!/bin/bash -l
          ls -lrta ${HELM_HOME}/repository/ || true
					helm verify ${vars.helmDir}/${vars.helmChartName} || true
					helm repo index . || true
					cat index.yaml || true"""

		      String chart="-F chart=@${vars.helmChartArchive}".trim()
		      String provenance=""
		      // https://github.com/helm/helm-www/blob/master/content/en/docs/topics/provenance.md
		      if (vars.isProvenance) {
		        provenance = "-F prov=@${vars.helmChartArchive}.prov".trim()
		      }

          String helmPushCmd = "echo NoDeploy"

          if (vars.isHelmCurl) {
            helmPushCmd = "curl --insecure -u ${HELM_REPO_USERNAME}:${HELM_REPO_PASSWORD} ${chart} ${provenance} ${HELM_REGISTRY_API_URL}"
				  }

        // https://github.com/chartmuseum/helm-push
        if (vars.isHelmPush) {
          //  --version=\"$(git log -1 --pretty=format:%h)\"
            helmPushCmd = "helm push ${vars.helmChartName} ${vars.customRepoName} --username ${HELM_REPO_USERNAME} --password ${HELM_REPO_PASSWORD} --home ${HELM_HOME} "
            if (!vars.skipInsecure) {
                helmPushCmd +=" --insecure "
            }
            if (vars.HELM_REPO_CA_FILE?.trim()) {
                helmPushCmd +=" --ca-file=${vars.HELM_REPO_CA_FILE} "
            }
            if (vars.HELM_REPO_CONTEXT_PATH?.trim()) {
                helmPushCmd +=" --context-path /${vars.HELM_REPO_CONTEXT_PATH}"
            }
        }

        // TODO Remove it when tee will be back
        helmPushCmd += " 2>&1 > ${vars.helmPushOutputFile} "

        echo "helmPushCmd : ${helmPushCmd}"

          if (isReleaseBranch() && vars.isHelmDeploy) {
        // https://github.com/helm/chartmuseum
        helmPushResult = sh (script: helmPushCmd, returnStatus: true)
        echo "HELM PUSH RETURN CODE : ${helmPushResult}"
        if (helmPushResult == 0) {
          echo "HELM PUSH SUCCESS"
              sh "echo \"Chart : ${chart} pushed to ${HELM_REGISTRY_API_URL}\"  2>&1 > ${vars.helmPushOutputFile} || true"
        } else {
          echo "WARNING : Helm push failed, check output at \'${vars.helmPushOutputFile}\' "
          if (!vars.skipHelmPushFailure) {
            echo "HELM PUSH FAILURE"
            //currentBuild.result = 'UNSTABLE'
            currentBuild.result = 'FAILURE'
            error 'There are errors in helm push'
          } else {
            echo "HELM PUSH FAILURE skipped"
            //error 'There are errors in helm push'
          }
        }
          } else {
            echo "No push on none release branches : ${helmPushCmd} 2>&1 > ${vars.helmPushOutputFile}"
          }

          if (!vars.skipCustomRepo) {
        sh "helm search repo ${vars.helmChartName} || true"
          }

        helmInstall(vars)
        helmDelete(vars)

      } // withCredentials

      } // HELM_HOME
    } // dir
  } catch (exc) {
    echo "Warn: There was a problem with pushing helm to \'${HELM_REGISTRY_API_URL}\' " + exc.toString()
  } finally {
    archiveArtifacts artifacts: "repositories.yml, **/helm-*.log, ${vars.helmTemplateOutputFile}, ${vars.helmRepoAddOutputFile}, ${vars.helmPushOutputFile}, ${vars.helmPackageOutputFile}, ${vars.helmChartArchive}", onlyIfSuccessful: false, allowEmptyArchive: true
  }
}
