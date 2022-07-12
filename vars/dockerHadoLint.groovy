#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

// Warning dockerLint.groovy may override https://github.com/jenkinsci/analysis-model/blob/master/src/main/java/edu/hm/hafner/analysis/parser/DockerLintParser.java
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/dockerHadoLint.groovy'

  vars = vars ?: [:]

  vars.dockerFilePath = vars.get('dockerFilePath', './docker/ubuntu18').trim()
  vars.dockerFileName = vars.get('dockerFileName', 'Dockerfile').trim()
  vars.dockerFileId = vars.get('dockerFileId', vars.draftPack ?: '0').trim()
  //vars.dockerTargetPath = vars.get("dockerTargetPath", vars.get("dockerFilePath", "./docker/ubuntu18")).trim()

  // Docker linter : hadolint, dockerfilelint, dive
  // See https://medium.com/@renatomefi/writing-dockerfile-like-a-software-developer-linting-9fd8c620174
  vars.dockerLintCmd = vars.get('dockerLintCmd', "docker run --rm -i -v ${pwd()}:/project:ro --workdir=/project hadolint/hadolint hadolint --format json - < \"${vars.dockerFilePath}/${vars.dockerFileName}\"").trim()
  //hadolint "${vars.dockerFilePath}/${vars.dockerFileName}"
  //vars.dockerLintCmd = vars.get("dockerLintCmd", "dockerfile_lint --json --verbose --dockerfile \"${vars.dockerFilePath}/${vars.dockerFileName}\"").trim()
  // hadolint Dockerfile -f checkstyle > checkstyle-hadolint.xml

  vars.skipDockerLintFailure = vars.get('skipDockerLintFailure', true).toBoolean()
  vars.skipDockerLint = vars.get('skipDockerLint', false).toBoolean()
  vars.dockerLintOutputFile = vars.get('dockerLintOutputFile', "docker-hadolint-${vars.dockerFileId}.json").trim()

  if (!vars.skipDockerLint) {
    if (fileExists("${vars.dockerFilePath}/${vars.dockerFileName}")) {
      try {
        if (body) { body() }

                //docker history --no-trunc nabla/ansible-jenkins-slave:latest > docker-history.log
                //dive nabla/ansible-jenkins-slave:latest

        // TODO Remove it when tee will be back
        vars.dockerLintCmd += " 2>&1 > ${vars.dockerLintOutputFile} "

        docker = sh (script: vars.dockerLintCmd, returnStatus: true)
        echo "DOCKER HANABLA RETURN CODE : ${docker}"
        if (docker == 0) {
          echo 'DOCKER HANABLA SUCCESS'
                //sh "hadolint Dockerfile -f checkstyle > target/checkstyle-hadolint.xml \"${vars.dockerFilePath}/${vars.dockerFileName}\""
                } else {
          echo "WARNING : Docker HadoLint failed, check output at \'${env.BUILD_URL}artifact/${vars.dockerLintOutputFile}\' "
          if (!vars.skipDockerLintFailure) {
            echo 'DOCKER HANABLA UNSTABLE'
            currentBuild.result = 'UNSTABLE'
            error 'There are errors in docker HadoLint'
                  } else {
            echo 'DOCKER HANABLA UNSTABLE skipped'
          //error 'There are errors in docker'
          }
        }
            } catch (exc) {
        echo 'Warn: There was a problem with docker HadoLint ' + exc
            } finally {
        cleanEmptyFile(vars)
        archiveArtifacts artifacts: "${vars.dockerLintOutputFile}, target/checkstyle-hadolint.xml", onlyIfSuccessful: false, allowEmptyArchive: true
        echo "Check : ${env.BUILD_URL}artifact/${vars.dockerLintOutputFile}"
        //recordIssues enabledForFailure: true, tool: checkStyle(pattern: 'target/checkstyle-hadolint.xml', id: "checkstyle-hadolint-${vars.dockerFileId}")
        recordIssues enabledForFailure: true, tool: hadoLint(pattern: "${vars.dockerLintOutputFile}")
      }
    } else {
      echo "No fileExists(${vars.dockerFileBuildPath}/${vars.dockerFileName})"
    }
  } else {
    echo 'Docker Lint skipped'
  }
}
