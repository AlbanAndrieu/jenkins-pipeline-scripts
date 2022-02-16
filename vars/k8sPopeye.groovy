#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sPopeye.groovy'

  vars = vars ?: [:]

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim() // TODO fixme HELM_REPOSITORY_CONFIG
  vars.kubeContext = vars.get('kubeContext', env.KUBECONTEXT ?: 'treasury-trba').trim() // TODO fixme Rename to HELM_KUBECONTEXT
  vars.kubeNamespace = vars.get('kubeNamespace', env.KUBENAMESPACE ?: 'fr-standalone-devops') // TODO fixme rename HELM_NAMESPACE

  vars.buildDir = vars.get('buildDir', "${pwd()}/").trim()
  //vars.popeyeResultFilePath = vars.get("popeyeResultFilePath", vars.buildDir ?: "./").trim()
  vars.popeyeResultFilePath = vars.get('popeyeResultFilePath', './').trim()
  vars.popeyeResultFileName = vars.get('popeyeResultFileName', 'popeye.html').trim()
  vars.popeyeSpinachtFilePath = vars.get('popeyeSpinachtFilePath', './k8s/').trim()
  vars.popeyeSpinachFileName = vars.get('popeyeSpinachFileName', 'spinach.yml').trim()

  vars.dockerFileId = vars.get('dockerFileId', vars.draftPack ?: '0').trim()

  vars.DOCKER_POPEYE_IMAGE = vars.get('DOCKER_POPEYE_IMAGE', 'quay.io/derailed/popeye').trim()

  // -v ${vars.buildDir}:/root/:ro -e POPEYE_REPORT_DIR=/tmp/popeye
  vars.k8sPopeyeCmd = vars.get('k8sPopeyeCmd', "docker run --rm -v $HOME/.kube:/root/.kube -v ${vars.buildDir}:/tmp --workdir /tmp ")
  vars.isLocalUser = true
  vars.k8sPopeyeCmd += getDockerOptsUser(vars)
  vars.k8sPopeyeCmd += " ${vars.DOCKER_POPEYE_IMAGE} --save --out html --output-file ${vars.popeyeResultFileName} "

  vars.allowEmptyResults = vars.get('allowEmptyResults', false).toBoolean()

  vars.skipDockerPopeyeFailure = vars.get('skipDockerPopeyeFailure', true).toBoolean()
  vars.skipDockerPopeye = vars.get('skipDockerPopeye', false).toBoolean()
  vars.k8sPopeyeOutputFile = vars.get('k8sPopeyeOutputFile', "docker-popeye-${vars.dockerFileId}.log").trim()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipDockerPopeye = true
  }

  if (!vars.skipDockerPopeye) {
    if (fileExists("${vars.KUBECONFIG}")) {
      try {
        if (body) { body() }

        if (!vars.kubeContext?.trim()) {
          vars.k8sPopeyeCmd += " --context ${vars.kubeContext} "
        }

        if (!vars.kubeNamespace?.trim()) {
          vars.k8sPopeyeCmd += " --namespace ${vars.kubeNamespace} "
        }

        if (fileExists("${vars.popeyeSpinachtFilePath}${vars.popeyeSpinachFileName}")) {
          vars.k8sPopeyeCmd += " -f ${vars.popeyeSpinachtFilePath}${vars.popeyeSpinachFileName} "
        } else {
          echo "No fileExists(${vars.popeyeSpinachtFilePath}${vars.popeyeSpinachFileName})"
        }

        // TODO Remove it when tee will be back
        vars.k8sPopeyeCmd += " 2>&1 > ${vars.k8sPopeyeOutputFile} "

        docker = sh (script: vars.k8sPopeyeCmd, returnStatus: true)
        echo "DOCKER POPEYE RETURN CODE : ${docker}"
        if (docker == 0) {
          echo 'DOCKER POPEYE SUCCESS'
        } else {
          echo "WARNING : Docker popeye failed, check output at \'${env.BUILD_URL}artifact/${vars.k8sPopeyeOutputFile}\' "
          if (isManager(vars)) {
            addWarningBadge("Check ${env.BUILD_URL}artifact/${vars.k8sPopeyeOutputFile}")
          }
          setBuildDescription(description: "<br/><b><font color=\"red\">Failed</font></b> k8sPopeye: <a href='${env.BUILD_URL}artifact/${vars.k8sPopeyeOutputFile}/*view*/'>${vars.popeyeResultFileName}</a>")
          if (!vars.skipDockerPopeyeFailure) {
            echo 'DOCKER POPEYE UNSTABLE'
            currentBuild.result = 'UNSTABLE'
            error 'There are errors in docker popeye'
          } else {
            echo 'DOCKER POPEYE UNSTABLE skipped'
          //error 'There are errors in docker'
          }
        }
      } catch (exc) {
        echo 'Warn: There was a problem with docker popeye : ' + exc
      } finally {
        cleanEmptyFile(vars)
        archiveArtifacts artifacts: "${vars.k8sPopeyeOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
        echo "Check : ${env.BUILD_URL}artifact/${vars.k8sPopeyeOutputFile}"
        //recordIssues enabledForFailure: true, tool: k8sPopeye(pattern: "${vars.k8sPopeyeOutputFile}")

        publishHTML (target: [
          allowMissing: vars.allowEmptyResults,
          alwaysLinkToLastBuild: false,
          keepAll: true,
          reportDir: "${vars.popeyeResultFilePath}",
          reportFiles: "${vars.popeyeResultFileName}",
          reportName: 'Popeye'
        ])
      }
    } else {
      echo "No fileExists(${vars.KUBECONFIG})"
    }
  } else {
    echo 'Docker popeye skipped'
  }
}
