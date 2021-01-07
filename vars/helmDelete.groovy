#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmDelete.groovy`"

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()

  vars.helmRelease = vars.get("helmRelease", vars.helmChartName ?: "test").trim()

  vars.isHelm2 = vars.get("isHelm2", false).toBoolean()
  vars.isDryRun = vars.get("isDryRun", env.DRY_RUN ?: false).toBoolean()
  vars.helmDeleteTimeout = vars.get("helmDeleteTimeout", "5m0s").trim()
  vars.isAtomic = vars.get("isAtomic", false).toBoolean()
  vars.isWait = vars.get("isWait", false).toBoolean()

  vars.skipDelete = vars.get("skipDelete", false).toBoolean()
  vars.isKeepHistory = vars.get("isKeepHistory", false).toBoolean()
  vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()
  vars.kubeNamespace = vars.get("kubeNamespace", env.KUBENAMESPACE ?: "fr-standalone-devops").trim()

  vars.helmDeleteOutputFile = vars.get("helmDeleteOutputFile", "helm-delete-${vars.helmFileId}.log").trim()
  vars.skipDeleteFailure = vars.get("skipDeleteFailure", true).toBoolean()

  if (!vars.skipDelete) {
    try {
      //echo "Using ${HELM_REGISTRY_TMP_URL}"

      if (body) { body() }

      String helmDeleteCmd = ""

      if (vars.isHelm2.toBoolean()) {
        helmDeleteCmd = "helm delete --purge ${vars.helmRelease}"
      } else {
        helmDeleteCmd = "helm uninstall "
        if (vars.KUBECONFIG?.trim()) {
          helmDeleteCmd += " --kubeconfig ${vars.KUBECONFIG} "
        }
        helmDeleteCmd += " --debug ${vars.helmRelease}"
        if (vars.kubeNamespace?.trim()) {
          helmDeleteCmd += " --namespace ${vars.kubeNamespace} "
        }
        if (vars.helmDeleteTimeout?.trim()) {
          helmDeleteCmd += "--timeout ${vars.helmDeleteTimeout} "
        }
        if (vars.isDryRun.toBoolean()) {
          helmDeleteCmd += " --dry-run "
        }
        if (vars.isKeepHistory.toBoolean()) {
          helmDeleteCmd += " --keep-history "
        }
      }

      // TODO Remove it when tee will be back
      helmDeleteCmd += " 2>&1 > ${vars.helmDeleteOutputFile} "

      // https://github.com/helm/chartmuseum
      helm = sh (script: helmDeleteCmd, returnStatus: true)
      echo "HELM DELETE RETURN CODE : ${helm}"
      if (helm == 0) {
        echo "HELM DELETE SUCCESS"
        if (!vars.isWait && !vars.isAtomic) {
          echo "Waiting..."
          sleep(time:5, unit:"SECONDS")
        } // isWait
      } else {
        echo "WARNING : Helm delete failed, check output at \'${env.BUILD_URL}artifact/${vars.helmDeleteOutputFile}\' "
        if (!vars.skipDeleteFailure) {
          echo "HELM DELETE FAILURE"
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm delete'
        } else {
          echo "HELM DELETE FAILURE skipped"
          //error 'There are errors in helm'
        }
      }

    } catch (exc) {
      echo "Warn: There was a problem with deleting helm " + exc.toString()
    }
  } else {
    echo "Helm delete skipped"
  }

}
