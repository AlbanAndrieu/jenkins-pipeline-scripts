#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

// Warning dockerLint.groovy may override https://github.com/jenkinsci/analysis-model/blob/master/src/main/java/edu/hm/hafner/analysis/parser/DockerLintParser.java
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/dockerLint.groovy'

  vars = vars ?: [:]

  vars.dockerFilePath = vars.get('dockerFilePath', './docker/ubuntu18').trim()
  vars.dockerFileName = vars.get('dockerFileName', 'Dockerfile').trim()
  vars.dockerFileId = vars.get('dockerFileId', vars.draftPack ?: '0').trim()

  // Docker linter : hadolint, dockerfilelint, dive
  vars.dockerLintCmd = vars.get('dockerLintCmd', "docker run --rm -i -v ${pwd()}:/root/:ro projectatomic/dockerfile-lint dockerfile_lint -j -f \"/root/${vars.dockerFilePath}/${vars.dockerFileName}\"").trim()

  vars.skipDockerLintFailure = vars.get('skipDockerLintFailure', true).toBoolean()
  vars.skipDockerLint = vars.get('skipDockerLint', false).toBoolean()
  vars.dockerLintOutputFile = vars.get('dockerLintOutputFile', "docker-lint-${vars.dockerFileId}.json").trim()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipDockerLint = true
  }

  if (!vars.skipDockerLint) {
    if (fileExists("${vars.dockerFilePath}/${vars.dockerFileName}")) {
      try {
        if (body) { body() }

        //docker history --no-trunc fusion-risk/ansible-jenkins-slave:latest > docker-history.log
        //dive fusion-risk/ansible-jenkins-slave:latest

        sh "find ${vars.dockerFilePath} -name \"Dockerfile*\" || true"
        sh 'pwd'

        // TODO Remove it when tee will be back
        vars.dockerLintCmd += " 2>&1 > ${vars.dockerLintOutputFile} "

        docker = sh (script: vars.dockerLintCmd, returnStatus: true)
        echo "DOCKER LINT RETURN CODE : ${docker}"
        if (docker == 0) {
          echo 'DOCKER LINT SUCCESS'
        } else {
          echo "WARNING : Docker lint failed, check output at \'${env.BUILD_URL}artifact/${vars.dockerLintOutputFile}\' "
          if (!vars.skipDockerLintFailure) {
            echo 'DOCKER LINT UNSTABLE'
            currentBuild.result = 'UNSTABLE'
            error 'There are errors in docker lint'
          } else {
            echo 'DOCKER LINT UNSTABLE skipped'
          //error 'There are errors in docker'
          }
        }
      } catch (exc) {
        echo 'Warn: There was a problem with docker lint ' + exc
      } finally {
        cleanEmptyFile(vars)
        archiveArtifacts artifacts: "${vars.dockerLintOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
        echo "Check : ${env.BUILD_URL}artifact/${vars.dockerLintOutputFile}"
        recordIssues enabledForFailure: true, tool: dockerLint(pattern: "${vars.dockerLintOutputFile}")
      }
    } else {
      echo "No fileExists(${vars.dockerFileBuildPath}/${vars.dockerFileName})"
    }
  } else {
    echo 'Docker Lint skipped'
  }
}
