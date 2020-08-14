#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmLint.groovy"

  vars = vars ?: [:]

  vars.helmDir = vars.get("helmDir", ".").trim()
  vars.helmChartName = vars.get("helmChartName", "charts").trim()
  vars.helmLintCmd = vars.get("helmLintCmd", "helm lint ${vars.helmDir}/${vars.helmChartName}").trim()

  vars.skipHelmLintFailure = vars.get("skipHelmLintFailure", false).toBoolean()
  vars.helmLintOutputFile = vars.get("helmLintOutputFile", "helm-lint.log").trim()

  try {
    if (body) { body() }

    // TODO Remove it when tee will be back
    vars.helmLintCmd += " 2>&1 > ${vars.helmLintOutputFile} "

    helm = sh (script: vars.helmLintCmd, returnStatus: true)
    echo "HELM LINT RETURN CODE : ${helm}"
    if (helm == 0) {
      echo "HELM LINT SUCCESS"
    } else {
	  echo "WARNING : Helm lint failed, check output at \'${vars.helmLintOutputFile}\' "
      if (!vars.skipHelmLintFailure) {
        echo "HELM LINT FAILURE"
        //currentBuild.result = 'UNSTABLE'
        currentBuild.result = 'FAILURE'
        error 'There are errors in helm lint'
      } else {
        echo "HELM LINT FAILURE skipped"
        //error 'There are errors in helm'
      }
    }

  } catch (exc) {
    echo "Warn: There was a problem with helm lint " + exc.toString()
  } finally {
    archiveArtifacts artifacts: "*.log", onlyIfSuccessful: false, allowEmptyArchive: true
  }

}
