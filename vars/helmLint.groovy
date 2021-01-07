#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmLint.groovy`"

  vars = vars ?: [:]

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get("helmDir", "./packs").toLowerCase().trim()
  vars.helmChartName = vars.get("helmChartName", "packs").toLowerCase().trim()
  vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()

  vars.skipHelmLintFailure = vars.get("skipHelmLintFailure", false).toBoolean()
  vars.skipHelmLint = vars.get("skipHelmLint", false).toBoolean()
  vars.helmLintOutputFile = vars.get("helmLintOutputFile", "helm-lint-${vars.helmFileId}.log").trim()

  //if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
  //    vars.skipHelmLint = true
  //}

  if (!vars.skipHelmLint) {

		try {
		  if (body) { body() }

      String helmLintCmd = "helm lint"

      helmLintCmd += " ${vars.helmDir}/${vars.helmChartName} "
		  // TODO Remove it when tee will be back
      helmLintCmd += " 2>&1 > ${vars.helmLintOutputFile} "

      helm = sh (script: helmLintCmd, returnStatus: true)
		  echo "HELM LINT RETURN CODE : ${helm}"
		  if (helm == 0) {
		    echo "HELM LINT SUCCESS"
		  } else {
        echo "WARNING : Helm lint failed, check output at \'${env.BUILD_URL}artifact/${vars.helmLintOutputFile}\' "
		    if (!vars.skipHelmLintFailure) {
          echo "HELM LINT UNSTABLE"
          currentBuild.result = 'UNSTABLE'
		      error 'There are errors in helm lint'
		    } else {
		      echo "HELM LINT UNSTABLE skipped"
		      //error 'There are errors in helm'
		    }
		  }

		} catch (exc) {
		  echo "Warn: There was a problem with helm lint " + exc.toString()
		} finally {
      cleanEmptyFile(vars)
		  archiveArtifacts artifacts: "${vars.helmLintOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
      echo "Check : ${env.BUILD_URL}artifact/${vars.helmLintOutputFile}"
		}

  } else {
      echo "Helm Lint skipped"
  }
}
