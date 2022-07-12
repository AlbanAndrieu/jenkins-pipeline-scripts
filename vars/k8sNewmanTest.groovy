#!/usr/bin/groovy
import hudson.model.*

//import java.util.regex.Pattern
//import java.util.regex.Matcher

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sNewmanTest.groovy'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.DOCKER_REGISTRY_ACR = vars.get('DOCKER_REGISTRY_ACR', env.DOCKER_REGISTRY_ACR ?: 'p21d13401013001.azurecr.io').toLowerCase().trim()
  vars.DOCKER_REGISTRY_ACR_URL = vars.get('DOCKER_REGISTRY_ACR_URL', env.DOCKER_REGISTRY_ACR_URL ?: "https://${vars.DOCKER_REGISTRY_ACR}").trim()
  vars.DOCKER_REGISTRY_ACR_CREDENTIAL = vars.get('DOCKER_REGISTRY_ACR_CREDENTIAL', env.DOCKER_REGISTRY_ACR_CREDENTIAL ?: 'p21d13401013001.azurecr.io').trim()

  vars.DOCKER_REGISTRY = vars.get('DOCKER_REGISTRY', vars.DOCKER_REGISTRY_ACR).toLowerCase().trim()

  vars.DOCKER_ORGANISATION = vars.get('DOCKER_ORGANISATION', env.DOCKER_ORGANISATION ?: 'nabla').trim()

  //vars.DOCKER_NEWMAN_IMAGE = vars.get("DOCKER_NEWMAN_IMAGE", "dannydainton/htmlextra").trim()
  vars.DOCKER_NEWMAN_TAG = vars.get('DOCKER_NEWMAN_TAG', env.DOCKER_NEWMAN_TAG ?: '0.0.2').trim()
  vars.DOCKER_NEWMAN_NAME = vars.get('DOCKER_NEWMAN_NAME', env.DOCKER_NEWMAN_NAME ?: 'fr-newman').trim()
  vars.DOCKER_NEWMAN_IMAGE = vars.get('DOCKER_NEWMAN_IMAGE', "${vars.DOCKER_REGISTRY_ACR}/${vars.DOCKER_ORGANISATION}/${vars.DOCKER_NEWMAN_NAME}:${vars.DOCKER_NEWMAN_TAG}").trim()

  vars.buildDir = vars.get('buildDir', "${pwd()}/").trim()
  vars.newmanFilePath = vars.get('newmanFilePath', vars.buildDir ?: './').trim()
  vars.newmanFilePath.replaceAll(vars.buildDir, '')
  //TODO vars.newmanFilePath.replaceAll(java.util.regex.Pattern.quote("${vars.buildDir}"), "") // newmanFilePath must be relatif to pwd
  //vars.newmanFilePath = vars.newmanFilePath.replaceAll("\\./", "")
  vars.newmanCollectionFileName = vars.get('newmanCollectionFileName', 'collection.json').trim()
  vars.newmanEnvironementFilePath = vars.get('newmanEnvironementFilePath', 'env/').trim()
  vars.newmanEnvironementFileName = vars.get('newmanEnvironementFileName', 'environment.json').trim()
  vars.newmanResultFilePath = vars.get('newmanResultFilePath', vars.newmanFilePath ?: './').trim()
  vars.newmanFileId = vars.get('newmanFileId', vars.draftPack ?: '0').trim()

  vars.k8sNewmanTestCmd = vars.get('k8sNewmanTestCmd', "docker run --rm -v ${vars.buildDir}:/etc/newman -w /etc/newman ")
  vars.isLocalUser = true
  vars.k8sNewmanTestCmd += getDockerOptsUser(vars)
  vars.k8sNewmanTestCmd += "${vars.DOCKER_NEWMAN_IMAGE} run ${vars.newmanFilePath}${vars.newmanCollectionFileName}".trim()

  vars.allowEmptyResults = vars.get('allowEmptyResults', false).toBoolean()

  vars.skipDockerNewmanFailure = vars.get('skipDockerNewmanFailure', true).toBoolean()
  vars.skipDockerNewman = vars.get('skipDockerNewman', false).toBoolean()
  vars.k8sNewmanTestOutputFile = vars.get('k8sNewmanTestOutputFile', "docker-newman-${vars.newmanFileId}.log").trim()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipDockerNewman = true
  }

  if (!vars.skipDockerNewman) {
    if (fileExists("${vars.newmanFilePath}${vars.newmanCollectionFileName}")) {
      try {
        if (body) { body() }

        // TODO See https://kubernetes.io/docs/tasks/job/parallel-processing-expansion/

        sh 'kubectl delete job -l jobgroup=newman'

        if (fileExists("${vars.newmanEnvironementFilePath}${vars.newmanEnvironementFileName}")) {
          vars.k8sNewmanTestCmd += " -e ${vars.newmanEnvironementFilePath}${vars.newmanEnvironementFileName} "
        } else {
          echo "No fileExists(${vars.newmanEnvironementFilePath}${vars.newmanEnvironementFileName})"
        }

        vars.k8sNewmanTestCmd += " -k -r htmlextra,junit,cli --reporter-htmlextra-export \'${vars.newmanResultFilePath}newman.html\' --reporter-junit-export=\'${vars.newmanResultFilePath}newman-report.xml\' --reporter-htmlextra-title 'NEWMAN'"
        //--reporter-htmlextra-title 'NEWMAN'
        //--suppress-exit-code

        sh "find ${vars.newmanFilePath} -name \"*.json\" || true"
        sh 'pwd'

        // TODO Remove it when tee will be back
        vars.k8sNewmanTestCmd += " 2>&1 > ${vars.k8sNewmanTestOutputFile} "

        newman = sh (script: vars.k8sNewmanTestCmd, returnStatus: true)
        echo "DOCKER NEWMAN RETURN CODE : ${newman}"
        if (newman == 0) {
          echo 'DOCKER NEWMAN SUCCESS'
        } else {
          echo "WARNING : Docker newman failed, check output at \'${env.BUILD_URL}artifact/${vars.k8sNewmanTestOutputFile}\' "
          if (!vars.skipDockerNewmanFailure) {
            echo 'DOCKER NEWMAN UNSTABLE'
            junit testResults: "${vars.newmanResultFilePath}newman-report.xml", healthScaleFactor: 2.0, allowEmptyResults: vars.allowEmptyResults, keepLongStdio: true
            currentBuild.result = 'UNSTABLE'
            error 'There are errors in docker newman'
          } else {
            echo 'DOCKER NEWMAN UNSTABLE skipped'
          //error 'There are errors in docker'
          }
        }
      } catch (exc) {
        echo 'Warn: There was a problem with docker newman ' + exc
      } finally {
        cleanEmptyFile(vars)
        archiveArtifacts artifacts: "${vars.k8sNewmanTestOutputFile}, ${vars.newmanResultFilePath}newman.html, ${vars.newmanResultFilePath}newman-report.xml", onlyIfSuccessful: false, allowEmptyArchive: true
        echo "Check : ${env.BUILD_URL}artifact/${vars.k8sNewmanTestOutputFile}"

        sh "ls -lrta ${vars.newmanResultFilePath} || true"

        publishHTML (target: [
              allowMissing: vars.allowEmptyResults,
              alwaysLinkToLastBuild: false,
              keepAll: true,
              reportDir: "${vars.newmanResultFilePath}",
              reportFiles: 'newman.html',
              reportName: 'Newman - tests'
        ])
      }
    } else {
      echo "No fileExists(${vars.newmanFilePath}/${vars.newmanCollectionFileName})"
    }
  } else {
    echo 'Docker Newman skipped'
  }
}
