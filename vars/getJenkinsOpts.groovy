#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getJenkinsOpts.groovy`"

  vars = vars ?: [:]

  vars.CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  vars.DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  vars.DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
  vars.RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
  vars.RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)

  vars.SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonar").trim() // sonardev
  vars.SONAR_HOST = vars.get("SONAR_HOST", env.SONAR_HOST ?: "sonar").trim() // sonardev
  vars.SONAR_URL = vars.get("SONAR_URL", env.SONAR_URL ?: "https://${vars.SONAR_HOST}").trim()
  vars.SONAR_SCANNER = vars.get("SONAR_SCANNER", env.SONAR_SCANNER ?: "Sonar-Scanner-4.2").trim()
  vars.SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g").trim()
  vars.SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: ".sonar").trim()
  vars.SONAR_CREDENTIALS = vars.get("SONAR_CREDENTIALS", env.SONAR_CREDENTIALS ?: "sonarcloud-nabla").trim()

  vars.STASH_CREDENTIALS = vars.get("STASH_CREDENTIALS", env.STASH_CREDENTIALS ?: "stash-jenkins").trim()

  vars.JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", env.JENKINS_CREDENTIALS ?: "jenkins-ssh").trim()
  vars.JENKINS_SSH_CREDENTIALS = vars.get("JENKINS_SSH_CREDENTIALS", env.JENKINS_SSH_CREDENTIALS ?: "jenkins-ssh").trim()

  vars.JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "mgr.jenkins.checkmarx").trim()

  vars.DOCKER_REGISTRY_TMP = vars.get("DOCKER_REGISTRY_TMP", env.DOCKER_REGISTRY_TMP ?: "index.docker.io/v1").toLowerCase().trim()
  vars.DOCKER_REGISTRY_TMP_URL = vars.get("DOCKER_REGISTRY_TMP_URL", env.DOCKER_REGISTRY_TMP_URL ?: "https://${vars.DOCKER_REGISTRY_TMP}").trim()
  vars.DOCKER_REGISTRY_TMP_CREDENTIAL = vars.get("DOCKER_REGISTRY_TMP_CREDENTIAL", env.DOCKER_REGISTRY_TMP_CREDENTIAL ?: "jenkins").trim()

  vars.DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "index.docker.io/v1").toLowerCase().trim()
  vars.DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${vars.DOCKER_REGISTRY}").trim()
  vars.DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins").trim()

  vars.DOCKER_REGISTRY_HUB = vars.get("DOCKER_REGISTRY_HUB", env.DOCKER_REGISTRY_HUB ?: "index.docker.io/v1").toLowerCase().trim()
  vars.DOCKER_REGISTRY_HUB_URL = vars.get("DOCKER_REGISTRY_HUB_URL", env.DOCKER_REGISTRY_HUB_URL ?: "https://${vars.DOCKER_REGISTRY_HUB}").trim()
  vars.DOCKER_REGISTRY_HUB_CREDENTIAL = vars.get("DOCKER_REGISTRY_HUB_CREDENTIAL", env.DOCKER_REGISTRY_HUB_CREDENTIAL ?: "hub-docker-nabla").trim()

  vars.DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla").trim()

  vars.COMPOSE_HTTP_TIMEOUT = vars.get("COMPOSE_HTTP_TIMEOUT", env.COMPOSE_HTTP_TIMEOUT ?: "200").trim()

  vars.HELM_PROJECT = vars.get("HELM_PROJECT", env.HELM_PROJECT ?: "nabla").trim()
  vars.HELM_REGISTRY = vars.get("HELM_REGISTRY", env.HELM_REGISTRY ?: "index.docker.io/v1").toLowerCase().trim()
  vars.HELM_REGISTRY_URL = vars.get("HELM_REGISTRY_URL", env.HELM_REGISTRY_URL ?: "https://${vars.HELM_REGISTRY}/api/chartrepo/${vars.HELM_PROJECT}/charts").trim()
  vars.HELM_REGISTRY_TMP = vars.get("HELM_REGISTRY_TMP", env.HELM_REGISTRY_TMP ?: "index.docker.io/v1").toLowerCase().trim()

  vars.HELM_REGISTRY_TMP_URL = vars.get("HELM_REGISTRY_TMP_URL", env.HELM_REGISTRY_TMP_URL ?: "https://${vars.HELM_REGISTRY_TMP}/api/chartrepo/${vars.HELM_PROJECT}/charts").trim()
  vars.HELM_REGISTRY_CREDENTIAL = vars.get("HELM_REGISTRY_CREDENTIAL", env.HELM_REGISTRY_CREDENTIAL ?: "jenkins").trim()

  // See https://opensource.triology.de/jenkins/pipeline-syntax/globals

  //def JENKINS_URL = vars.get("JENKINS_URL", env.JENKINS_URL ?: "TODO").trim()
  //def JOB_NAME = vars.get("JOB_NAME", env.JOB_NAME ?: "TODO").trim()
  //def JOB_BASE_NAME = vars.get("JOB_BASE_NAME", env.JOB_BASE_NAME ?: "TODO").trim()

  // createGlobalEnvironmentVariables

  vars.isVirtualHost = vars.get("isVirtualHost", false).toBoolean()
  vars.isRoot = vars.get("isRoot", false).toBoolean()
  //vars.isCredentialsMapping = vars.get("isCredentialsMapping", true).toBoolean()
  vars.isNetworkMapping = vars.get("isNetworkMapping", false).toBoolean()
  vars.isPidMapping = vars.get("isPidMapping", false).toBoolean()
  vars.isDnsSearchMapping = vars.get("isDnsSearchMapping", true).toBoolean()

  vars.isNpmConfigPrefix = vars.get("isNpmConfigPrefix", false).toBoolean()
  vars.isHomeWorkspace = vars.get("isHomeWorkspace", false).toBoolean()
  vars.isInit = vars.get("isInit", false).toBoolean()

  vars.isLocalJenkinsUser = vars.get("isLocalJenkinsUser", false).toBoolean()
  vars.isLocalMavenRepository = vars.get("isLocalMavenRepository", false).toBoolean()
  vars.isDockerCompose = vars.get("isDockerCompose", false).toBoolean() // In order to get access to /var/run/docker.sock
  vars.isJenkinsTools = vars.get("isJenkinsTools", false).toBoolean()

  vars.jenkinsToolsDirectory = vars.get("jenkinsToolsDirectory", "/workspace/slave/tools/").trim()
  vars.mavenHome = vars.get("mavenHome", "${vars.JENKINS_USER_HOME}/.m2/").trim()
  vars.isEntrypoint = vars.get("isEntrypoint", true).toBoolean()

  vars.isLogParserPublisher = vars.get("isLogParserPublisher", true).toBoolean()

  if (vars.DEBUG_RUN) {
    try {
      echo "JENKINS_URL : ${JENKINS_URL}"
    } catch(exc) {
      echo 'Warn: There was a problem. '+exc.toString()
    }
  }
  //if (JENKINS_URL ==~ /.*almonde-jenkins.*|.*risk-jenkins.*|.*test-jenkins.*|.*localhost.*/ ) {
  if ( JENKINS_URL ==~ /http:\/\/albandri.*\/jenkins\/|http:\/\/localhost.*\/jenkins\// || JENKINS_URL ==~ /https:\/\/albandri.*\/jenkins\/|http:\/\/localhost.*\/jenkins\// ) {
    echo "JPL is supported"
  } else {
    echo "JPL is NOT supported"
    vars.isLogParserPublisher = false
    //vars.SONAR_INSTANCE = "sonar".trim()
    //vars.SONAR_CREDENTIALS = "jenkins".trim()
  }

  if (vars.SONAR_INSTANCE == "sonartest") {
    vars.SONAR_CREDENTIALS = "sonartest".trim()
  }

  if (vars.DEBUG_RUN) {
    try {
      echo "JENKINS_URL : ${JENKINS_URL}"

      //echo "NODE_NAME : ${NODE_NAME}"
      echo "NODE_LABELS : ${NODE_LABELS}"

      echo "JOB_NAME : ${JOB_NAME}"
      echo "JOB_BASE_NAME : ${JOB_BASE_NAME}"

      echo "SONAR_INSTANCE : ${vars.SONAR_INSTANCE}"
      echo "SONAR_CREDENTIALS : ${vars.SONAR_CREDENTIALS}"
    } catch(exc) {
      echo 'Warn: There was a problem. '+exc.toString()
    }
  }

  vars.isProperties = vars.get("isProperties", "TODO").trim()
  echo "JPL testing ${vars.isProperties}"

  if (vars.isProperties ==~ /LogParserPublisher/ ) {
    return vars.isLogParserPublisher
  }

  return true

}
