#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmDeploy.groovy`'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops').trim()

  vars.isHelm2 = vars.get('isHelm2', false).toBoolean()
  vars.skipHelmDeploy = vars.get('skipHelmDeploy', false).toBoolean()
  vars.isAtomic = vars.get('isAtomic', false).toBoolean()
  vars.isWait = vars.get('isWait', false).toBoolean()

  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()
  vars.helmListAllOutputFile = vars.get('helmListAllOutputFile', "helm-list-all-${vars.helmFileId}.log").trim()
  vars.helmListDeployedOutputFile = vars.get('helmListDeployedOutputFile', "helm-list-depoyed-${vars.helmFileId}.log").trim()
  vars.helmListFailedOutputFile = vars.get('helmListFailedOutputFile', "helm-list-failed-${vars.helmFileId}.log").trim()

  if (vars.isAtomic.toBoolean()) {
    echo 'Override due to atomic'
    vars.isWait = true
    vars.isDelete = false
  } // isAtomic

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipHelmDeploy = true
  }

  if (!vars.skipHelmDeploy) {
    try {
      //String helmHome = vars.JENKINS_HELM_HOME.trim()
      //withEnv(["HELM_HOME=${helmHome}"]) {
      sh "helm list --all-namespaces || true 2>&1 > ${vars.helmListAllOutputFile}"

      lock(resource: "lock_K8S_${vars.kubeNamespace}", inversePrecedence: true) {
        //helmRepo(vars)

        if (vars.isHelm2.toBoolean()) {
          // See https://hub.docker.com/r/alpine/helm
          // run container as command
          //alias helm="docker run -ti --rm -v $(pwd):/apps -v ~/.kube:/root/.kube -v ~/.helm:/root/.helm alpine/helm"
          //helm --help
          docker.image('alpine/helm:2.14.0').withRun("-e KUBECONFIG=\"/root/.kube/config:${ vars.KUBECONFIG}\"").inside(' -v ~/.kube:/root/.kube -v ~/.helm:/root/.helm ') { c ->
            helmDelete(vars)
            helmInstall(vars)

          // sh "docker logs ${c.id}"
          } // docker
        } else {
          helmDelete(vars)
          helmInstall(vars)
        }

        sh "helm list --namespace ${vars.kubeNamespace} --deployed || true 2>&1 > ${vars.helmListDeployedOutputFile}"
        sh "helm list --namespace ${vars.kubeNamespace} --failed || true 2>&1 > ${vars.helmListFailedOutputFile}"
      } // lock

      //} // HELM_HOME
      if (body) { body() }
    } catch (exc) {
      echo 'Warn: There was a problem with deploying helm ' + exc
    } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: "${vars.helmListAllOutputFile}, ${vars.helmListDeployedOutputFile}, ${vars.helmListFailedOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'Helm deploy skipped'
  }
}
