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

  vars.SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonar").trim()
  vars.SONAR_HOST = vars.get("SONAR_HOST", env.SONAR_HOST ?: "sonarcloud.io").trim()
  vars.SONAR_URL = vars.get("SONAR_URL", env.SONAR_URL ?: "https://${vars.SONAR_HOST}").trim()
  vars.SONAR_SCANNER = vars.get("SONAR_SCANNER", env.SONAR_SCANNER ?: "Sonar-Scanner-4.2").trim()
  vars.SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g").trim()
  vars.SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: ".sonar").trim()
  vars.SONAR_CREDENTIALS = vars.get("SONAR_CREDENTIALS", env.SONAR_CREDENTIALS ?: "sonarcloud-nabla").trim()

  vars.STASH_CREDENTIALS = vars.get("STASH_CREDENTIALS", env.STASH_CREDENTIALS ?: "stash-jenkins").trim()

  vars.JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", env.JENKINS_CREDENTIALS ?: "jenkins-ssh").trim()
  vars.JENKINS_SSH_CREDENTIALS = vars.get("JENKINS_SSH_CREDENTIALS", env.JENKINS_SSH_CREDENTIALS ?: "jenkins-ssh").trim()

  vars.JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins").trim()

  vars.DOCKER_REGISTRY_TMP = vars.get("DOCKER_REGISTRY_TMP", env.DOCKER_REGISTRY_TMP ?: "registry.hub.docker.com").toLowerCase().trim()
  vars.DOCKER_REGISTRY_TMP_URL = vars.get("DOCKER_REGISTRY_TMP_URL", env.DOCKER_REGISTRY_TMP_URL ?: "https://${vars.DOCKER_REGISTRY_TMP}").trim()
  vars.DOCKER_REGISTRY_TMP_CREDENTIAL = vars.get("DOCKER_REGISTRY_TMP_CREDENTIAL", env.DOCKER_REGISTRY_TMP_CREDENTIAL ?: "jenkins").trim()

  vars.DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.hub.docker.com").toLowerCase().trim()
  vars.DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${vars.DOCKER_REGISTRY}").trim()
  vars.DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "hub-docker-nabla").trim()

  vars.DOCKER_REGISTRY_HUB = vars.get("DOCKER_REGISTRY_HUB", env.DOCKER_REGISTRY_HUB ?: "").toLowerCase().trim()
  vars.DOCKER_REGISTRY_HUB_URL = vars.get("DOCKER_REGISTRY_HUB_URL", env.DOCKER_REGISTRY_HUB_URL ?: "https://${vars.DOCKER_REGISTRY_HUB}").trim()
  vars.DOCKER_REGISTRY_HUB_CREDENTIAL = vars.get("DOCKER_REGISTRY_HUB_CREDENTIAL", env.DOCKER_REGISTRY_HUB_CREDENTIAL ?: "hub-docker-nabla").trim()

  vars.DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla").trim()

  vars.COMPOSE_HTTP_TIMEOUT = vars.get("COMPOSE_HTTP_TIMEOUT", env.COMPOSE_HTTP_TIMEOUT ?: "200").trim()

  vars.JENKINS_HELM_HOME = vars.get("JENKINS_HELM_HOME", env.JENKINS_HELM_HOME ?: "/home/jenkins/.cache/helm").trim()
  vars.HELM_REGISTRY_STABLE_URL = vars.get("HELM_REGISTRY_STABLE_URL", env.HELM_REGISTRY_STABLE_URL ?: "https://charts.helm.sh/stable").toLowerCase().trim()

  vars.HELM_PROJECT = vars.get("HELM_PROJECT", env.HELM_PROJECT ?: "nabla").trim()
  vars.HELM_REGISTRY = vars.get("HELM_REGISTRY", env.HELM_REGISTRY ?: "registry.hub.docker.com").toLowerCase().trim()
  vars.HELM_REGISTRY_URL = vars.get("HELM_REGISTRY_URL", env.HELM_REGISTRY_URL ?: "https://${vars.HELM_REGISTRY}/api/chartrepo/${vars.HELM_PROJECT}/charts").trim()
  vars.HELM_REGISTRY_TMP = vars.get("HELM_REGISTRY_TMP", env.HELM_REGISTRY_TMP ?: "registry.hub.docker.com").toLowerCase().trim()

  vars.HELM_REGISTRY_TMP_URL = vars.get("HELM_REGISTRY_TMP_URL", env.HELM_REGISTRY_TMP_URL ?: "https://${vars.HELM_REGISTRY_TMP}/api/chartrepo/${vars.HELM_PROJECT}/charts").trim()
  vars.HELM_REGISTRY_CREDENTIAL = vars.get("HELM_REGISTRY_CREDENTIAL", env.HELM_REGISTRY_CREDENTIAL ?: "jenkins").trim()

  vars.KUBECONFIG = vars.get("KUBECONFIG", env.KUBECONFIG ?: "/home/jenkins/.kube/config").trim()
  vars.NPM_SETTINGS_CONFIG = vars.get("NPM_SETTINGS_CONFIG", env.NPM_SETTINGS_CONFIG ?: "nabla-npmrc-default").trim()
  vars.BOWER_SETTINGS_CONFIG = vars.get("BOWER_SETTINGS_CONFIG", env.BOWER_SETTINGS_CONFIG ?: "nabla-bowerrc-default").trim()
  vars.MAVEN_SETTINGS_CONFIG = vars.get("MAVEN_SETTINGS_CONFIG", env.MAVEN_SETTINGS_CONFIG ?: "nabla-settings-nexus").trim()
  vars.MAVEN_SETTINGS_SECURITY_CONFIG = vars.get("MAVEN_SETTINGS_SECURITY_CONFIG", env.MAVEN_SETTINGS_SECURITY_CONFIG ?: "nabla-settings-security-nexus").trim()
  vars.K8S_SETTINGS_CONFIG = vars.get("K8S_SETTINGS_CONFIG", env.K8S_SETTINGS_CONFIG ?: "nabla-k8s-default").trim()

  vars.HTTP_PROXY = vars.get("HTTP_PROXY", env.HTTP_PROXY ?: "http://192.168.1.57:3128").trim()
  vars.HTTPS_PROXY = vars.get("HTTPS_PROXY", env.HTTPS_PROXY ?: "http://192.168.1.57:3128").trim()
  vars.NO_PROXY = vars.get("NO_PROXY", env.NO_PROXY ?: "localhost,127.0.0.1,.finastra.com,.misys.global.ad,.finastra.global,.azurecr.io,verdaccio,10.199.52.11").trim()

  // See https://opensource.triology.de/jenkins/pipeline-syntax/globals

  //def JENKINS_URL = vars.get("JENKINS_URL", env.JENKINS_URL ?: "TODO").trim()
  //def JOB_NAME = vars.get("JOB_NAME", env.JOB_NAME ?: "TODO").trim()
  //def JOB_BASE_NAME = vars.get("JOB_BASE_NAME", env.JOB_BASE_NAME ?: "TODO").trim()

  // createGlobalEnvironmentVariables

  vars.isVirtualHost = vars.get("isVirtualHost", false).toBoolean()
  vars.isRoot = vars.get("isRoot", false).toBoolean()
  //vars.isCredentialsMapping = vars.get("isCredentialsMapping", true).toBoolean()
  vars.isNetworkMapping = vars.get("isNetworkMapping", false).toBoolean()
  vars.isUserNamespace = vars.get("isUserNamespace", false).toBoolean()
  vars.isPidMapping = vars.get("isPidMapping", false).toBoolean()
  vars.isDnsSearchMapping = vars.get("isDnsSearchMapping", true).toBoolean()
  vars.isProxy = vars.get("isProxy", false).toBoolean()

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
  vars.skipAqua = vars.get("skipAqua", true).toBoolean()
  vars.skipCheckmarx = vars.get("skipCheckmarx", true).toBoolean()

  if (vars.DEBUG_RUN) {
    try {
      echo "JENKINS_URL : ${JENKINS_URL}"
    } catch(exc) {
      echo 'Warn: There was a problem. '+exc.toString()
    }
  }
  if ( JENKINS_URL ==~ /http:\/\/albandri.*\/jenkins\/|http:\/\/localhost.*\/jenkins\// || JENKINS_URL ==~ /https:\/\/albandri.*\/jenkins\/|http:\/\/localhost.*\/jenkins\// ) {
    echo "JPL is supported"
  } else {
    echo "JPL is NOT supported"
    vars.isLogParserPublisher = false
    vars.skipAqua = true
    vars.skipCheckmarx = true
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

  vars.isProperties = vars.get("isProperties", "none").trim()
  echo "JPL isProperties ${vars.isProperties}"

  if (vars.isProperties ==~ /LogParserPublisher/ ) {
    return vars.isLogParserPublisher
  }

  if (vars.isProperties ==~ /Aqua|skipAqua/ ) {
    return vars.skipAqua
  }

  if (vars.isProperties ==~ /Checkmarx|skipCheckmarx/ ) {
    return vars.skipCheckmarx
  }

  return true

}
