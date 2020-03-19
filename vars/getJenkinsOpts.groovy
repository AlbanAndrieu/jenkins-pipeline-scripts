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
  vars.SONAR_SCANNER = vars.get("SONAR_SCANNER", env.SONAR_SCANNER ?: "Sonar-Scanner-4.2").trim()
  vars.SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g").trim()
  //vars.SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: "$WORKSPACE").trim()

  vars.STASH_CREDENTIALS = vars.get("STASH_CREDENTIALS", env.STASH_CREDENTIALS ?: "jenkins").trim()
  vars.SONAR_CREDENTIALS = vars.get("SONAR_CREDENTIALS", env.SONAR_CREDENTIALS ?: "jenkins").trim()

  vars.JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", env.JENKINS_CREDENTIALS ?: "jenkins").trim()

  vars.JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins/").trim()

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

  //if (JENKINS_URL ==~ /.*almonde-jenkins.*|.*risk-jenkins.*|.*test-jenkins.*|.*localhost.*/ ) {
  if ( JENKINS_URL ==~ /http:\/\/albandri.*\/jenkins\/|http:\/\/localhost.*\/jenkins\// ) {
    echo "JPL is supported"
  } else {
    echo "JPL is NOT supported"
    vars.isLogParserPublisher = false
    vars.SONAR_INSTANCE = "sonar".trim()
    vars.SONAR_CREDENTIALS = "jenkins".trim()
  }

  if (vars.SONAR_INSTANCE == "sonartest") {
    vars.SONAR_CREDENTIALS = "sonartest".trim()
  }

  if (vars.DEBUG_RUN) {
    try {
      echo "JENKINS_URL : ${JENKINS_URL}"

      echo "NODE_NAME : ${NODE_NAME}"
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
