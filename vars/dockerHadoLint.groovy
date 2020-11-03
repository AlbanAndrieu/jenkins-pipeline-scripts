#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

// Warning dockerLint.groovy may override https://github.com/jenkinsci/analysis-model/blob/master/src/main/java/edu/hm/hafner/analysis/parser/DockerLintParser.java
def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/dockerHadoLint.groovy"

  vars = vars ?: [:]

  vars.dockerFilePath = vars.get("dockerFilePath", "./docker/ubuntu18").trim()
  vars.dockerFileName = vars.get("dockerFileName", "Dockerfile").trim()
  vars.dockerFileId = vars.get("dockerFileId", "0").trim()
  //vars.dockerTargetPath = vars.get("dockerTargetPath", vars.get("dockerFilePath", "./docker/ubuntu18//")).trim()

  // Docker linter : hadolint, dockerfilelint, dive
  vars.dockerLintCmd = vars.get("dockerLintCmd", "docker run --rm -i hadolint/hadolint < \"${vars.dockerFilePath}/${vars.dockerFileName}\"").trim()
  //hadolint "${vars.dockerFilePath}/${vars.dockerFileName}"
  //vars.dockerLintCmd = vars.get("dockerLintCmd", "dockerfile_lint --json --verbose --dockerfile \"${vars.dockerFilePath}/${vars.dockerFileName}\"").trim()
  // hadolint Dockerfile -f checkstyle > checkstyle-hadolint.xml

  vars.skipDockerLintFailure = vars.get("skipDockerLintFailure", true).toBoolean()
  vars.dockerLintOutputFile = vars.get("dockerLintOutputFile", "docker-hadolint-${vars.dockerFileId}.log").trim()

  try {
    if (body) { body() }

    //docker history --no-trunc fusion-risk/ansible-jenkins-slave:latest > docker-history.log
    //dive fusion-risk/ansible-jenkins-slave:latest

    // TODO Remove it when tee will be back
    vars.dockerLintCmd += " 2>&1 > ${vars.dockerLintOutputFile} "

    docker = sh (script: vars.dockerLintCmd, returnStatus: true)
    echo "DOCKER LINT RETURN CODE : ${docker}"
    if (docker == 0) {
      echo "DOCKER LINT SUCCESS"
      sh "hadolint Dockerfile -f checkstyle > target/checkstyle-hadolint.xml \"${vars.dockerFilePath}/${vars.dockerFileName}\""
    } else {
      echo "WARNING : Docker lint failed, check output at \'${vars.dockerLintOutputFile}\' "
      if (!vars.skipDockerLintFailure) {
        echo "DOCKER LINT FAILURE"
        //currentBuild.result = 'UNSTABLE'
        currentBuild.result = 'FAILURE'
        error 'There are errors in docker lint'
      } else {
        echo "DOCKER LINT FAILURE skipped"
        //error 'There are errors in docker'
      }
    }

  } catch (exc) {
    echo "Warn: There was a problem with docker lint " + exc.toString()
  } finally {
    archiveArtifacts artifacts: "${vars.dockerLintOutputFile}, target/checkstyle-hadolint.xml", onlyIfSuccessful: false, allowEmptyArchive: true
    recordIssues enabledForFailure: true, tool: checkStyle(pattern: 'target/checkstyle-hadolint.xml', id: "checkstyle-hadolint-${vars.dockerFileId}")
  }

}
