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

  String HELM_REGISTRY_STABLE_URL = vars.get("HELM_REGISTRY_STABLE_URL", env.HELM_REGISTRY_STABLE_URL ?: "https://kubernetes-charts.storage.googleapis.com/").toLowerCase().trim()

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
  vars.skipCustomRepo = vars.get("skipCustomRepo", false).toBoolean()
  vars.customRepoName = vars.get("customRepoName", "custom").trim()
  vars.isHelm2 = vars.get("isHelm2", true).toBoolean()
  vars.isDelete = vars.get("isDelete", false).toBoolean()
  vars.isInstall = vars.get("isInstall", false).toBoolean()
  vars.isHarbor = vars.get("isHarbor", true).toBoolean()
  vars.isProvenance = vars.get("isProvenance", false).toBoolean()
  vars.isSign = vars.get("isSign", false).toBoolean()
  vars.isHelmPush = vars.get("isHelmPush", false).toBoolean()
  vars.isTemplate = vars.get("isTemplate", true).toBoolean()
  vars.isDependencyUpdate = vars.get("isDependencyUpdate", true).toBoolean()
  vars.isDependencyBuild = vars.get("isDependencyBuild", false).toBoolean()

  //vars.helmCaFile = vars.get("helmCaFile", "/usr/local/share/ca-certificates/UK1VSWCERT01-CA-5.crt").trim()
  //vars.helmCaFile = vars.get("helmCaFile", "/root/pki/ca-old.pem").trim()
  vars.helmCaFile = vars.get("helmCaFile", "/etc/ssl/certs/ca.pem").trim()

  vars.helmPackageOutputFile = vars.get("helmPackageOutputFile", "helm-package.log").trim()
  vars.helmPushOutputFile = vars.get("helmPushOutputFile", "helm-push.log").trim()
  vars.helmTemplateOutputFile = vars.get("helmTemplateOutputFile", "helm-template.log").trim()
  vars.skipFailure = vars.get("skipFailure", true).toBoolean()

  try {
    echo "Using api : ${HELM_REGISTRY_API_URL} from : ${vars.buildDir}"
    echo "Using repo : ${HELM_REGISTRY_REPO_URL}/index.yaml"

    dir("${vars.buildDir}") {

      // See https://scm-git-eur.misys.global.ad/projects/MD/repos/jenkins-pipeline-library-kondor/browse/vars/kondorBuildServices.groovy#193

      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: HELM_REGISTRY_CREDENTIAL, usernameVariable: 'HELM_USERNAME', passwordVariable: 'HELM_PASSWORD']]) {


        if (vars.isHelm2.toBoolean()) {
          // TODO remove once in the docker image
          sh "helm init --client-only || true"
		}

        sh "helm version || true"
        sh "helm repo list || true"

        if (!vars.isHelm2.toBoolean()) {
		// For local http://127.0.0.1:8879/charts
		sh "helm serve || true &"
		sh "curl 127.0.0.1:8879 || true"
		sh "ls -lrta /home/jenkins/.helm/ /home/jenkins/.helm/repository/ || true"
        }
        // Remove https://kubernetes-charts.storage.googleapis.com/ if any
        sh "helm repo remove ${vars.stableRepoName} || true"
        if (!vars.skipStableRepo) {
          sh "helm repo add ${vars.stableRepoName} ${HELM_REGISTRY_STABLE_URL} || true"
        }

        sh "helm repo remove ${vars.customRepoName} || true"
        if (!vars.skipCustomRepo) {
          // --insecure-skip-tls-verify
          sh "helm repo add ${vars.customRepoName} --username ${HELM_USERNAME} --password ${HELM_PASSWORD} ${HELM_REGISTRY_REPO_URL} --ca-file=${vars.helmCaFile} || true"

          sh "helm repo list || true"
          //sh "helm search repo ${vars.customRepoName}/${vars.helmChartName} || true"
          sh "helm search repo ${vars.helmChartName} || true"
        }

        helmLint(vars, body)

        //NO KUBECTL on Docker
        //sh "kubectl get configmaps --namespace=kube-system || true"
        //The connection to the server localhost:8080 was refused - did you specify the right host or port?

        if (vars.isTemplate.toBoolean()) {
          sh "helm template ${vars.helmDir}/${vars.helmChartName} 2>&1 > ${vars.helmTemplateOutputFile}"
        }
        //helm push

        //helm create ${vars.helmChartName}
        // helm upgrade

		// if requirements.yml
		if (vars.isDependencyUpdate.toBoolean()) {
		  sh "helm dependency update ${vars.helmDir}/${vars.helmChartName} || true"
		} else {
		  sh "helm dependency build ${vars.helmDir}/${vars.helmChartName} || true"
		}
		sh "cat requirements.lock || true"
		sh "helm dependency list ${vars.helmDir}/${vars.helmChartName} || true"

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
          if (!vars.skipFailure) {
            echo "HELM PACKAGE FAILURE"
            //currentBuild.result = 'UNSTABLE'
            currentBuild.result = 'FAILURE'
            error 'There are errors in helm package'
          } else {
            echo "HELM PACKAGE FAILURE skipped"
            //error 'There are errors in helm package'
          }
        }

		sh "ls ~/.helm/repository/local/ || true"

		sh "helm verify ${vars.helmDir}/${vars.helmChartName} || true"
		sh "helm repo index . || true"
		sh "cat index.yaml || true"

        String chart="-F chart=@${vars.helmChartArchive}".trim()
        String provenance=""
        // https://github.com/helm/helm-www/blob/master/content/en/docs/topics/provenance.md
        if (vars.isProvenance) {
          provenance = "-F prov=@${vars.helmChartArchive}.prov".trim()
        }

        String helmPushCmd = "curl --insecure -u ${HELM_USERNAME}:${HELM_PASSWORD} ${chart} ${provenance} ${HELM_REGISTRY_API_URL}"

        // https://github.com/chartmuseum/helm-push
        if (vars.isHelmPush) {
          //  --version=\"$(git log -1 --pretty=format:%h)\"
          helmPushCmd = "helm push ${vars.helmChartName} ${vars.customRepoName} --username ${HELM_USERNAME} --password ${HELM_PASSWORD}"
        }

        // TODO Remove it when tee will be back
        helmPushCmd += " 2>&1 > ${vars.helmPushOutputFile} "

        echo "helmPushCmd : ${helmPushCmd}"

        // https://github.com/helm/chartmuseum
        helmPushResult = sh (script: helmPushCmd, returnStatus: true)
        echo "HELM PUSH RETURN CODE : ${helmPushResult}"
        if (helmPushResult == 0) {
          echo "HELM PUSH SUCCESS"
        } else {
          echo "WARNING : Helm push failed, check output at \'${vars.helmPushOutputFile}\' "
          if (!vars.skipFailure) {
            echo "HELM PUSH FAILURE"
            //currentBuild.result = 'UNSTABLE'
            currentBuild.result = 'FAILURE'
            error 'There are errors in helm push'
          } else {
            echo "HELM PUSH FAILURE skipped"
            //error 'There are errors in helm push'
          }
        }

        sh "helm search repo ${vars.helmChartName} || true"

        helmInstall(vars)
        helmDelete(vars)

      } // withCredentials

    } // dir
  } catch (exc) {
    echo "Warn: There was a problem with pushing helm to \'${HELM_REGISTRY_API_URL}\' " + exc.toString()
  } finally {
    archiveArtifacts artifacts: "**/helm-*.log", onlyIfSuccessful: false, allowEmptyArchive: true
  }
}
