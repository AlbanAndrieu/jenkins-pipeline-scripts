#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getDockerOpts.groovy`"

  vars = vars ?: [:]

  def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

  def JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins/").trim()

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
  vars.mavenHome = vars.get("mavenHome", "${JENKINS_USER_HOME}/.m2/").trim()
  vars.isEntrypoint = vars.get("isEntrypoint", true).toBoolean()

  String DOCKER_OPTS_USER_ID = [
     '-v /etc/passwd:/etc/passwd:ro',
     '-v /etc/group:/etc/group:ro',
     '--group-add 2000',
     '--group-add 1101',
  ].join(" ")

  String DOCKER_OPTS_BASIC = DOCKER_OPTS_USER_ID

  if (vars.isRoot == true) {
      DOCKER_OPTS_BASIC += ' -u root:root'
  }

  if (vars.isNetworkMapping == true) {
      DOCKER_OPTS_BASIC += ' --net=host'
  }
  if (vars.isPidMapping == true) {
      DOCKER_OPTS_BASIC += ' --pid=host'
  }
  if (vars.isDnsSearchMapping == true) {
      DOCKER_OPTS_BASIC += " --dns-search=nabla.mobi"
  }
  if (vars.isInit == true) {
      DOCKER_OPTS_BASIC += ' --init'
  }

  if (vars.isNpmConfigPrefix == true) {
      DOCKER_OPTS_BASIC += " -e NPM_CONFIG_PREFIX=${WORKSPACE}/.npm"
  }
  if (vars.isHomeWorkspace == true) {
      DOCKER_OPTS_BASIC += " -e HOME=${WORKSPACE}"
  }

  if (vars.isLocalJenkinsUser == true) {
      DOCKER_OPTS_BASIC = [
          DOCKER_OPTS_BASIC,
          //DOCKER_OPTS_USER_ID,
          "-v ${JENKINS_USER_HOME}:/home/jenkins"
      ].join(" ")
  } else if (vars.isLocalMavenRepository == true) {
      // Only mounting maven repository
      DOCKER_OPTS_BASIC += " -v ${vars.mavenHome}:/home/jenkins/.m2:ro"
  }

  // On Virtual host or bare metal hardware we can use local settings
  // On docker we will not use local configuration
  if (vars.isVirtualHost == true) {
      // workspace is needed for SonarQube access to sonar-scanner and sonar-build-wrapper
      DOCKER_OPTS_BASIC = [
          DOCKER_OPTS_BASIC,
          "-v /usr/local/sonar-build-wrapper:/usr/local/sonar-build-wrapper",
          "-v /usr/local/sonar-runner/:/usr/local/sonar-runner/"
      ].join(" ")

      if (vars.isJenkinsTools == true) {
          DOCKER_OPTS_BASIC += " -v ${vars.jenkinsToolsDirectory}:/workspace/slave/tools/"
      }
  }

  String DOCKER_OPTS_COMPOSE = [
      DOCKER_OPTS_BASIC,
  ].join(" ")

  if (vars.isDockerCompose == true) {
      DOCKER_OPTS_COMPOSE += ' -v /var/run/docker.sock:/var/run/docker.sock'
  }

  if (vars.isEntrypoint == true) {
      DOCKER_OPTS_COMPOSE += " --entrypoint=\'\'"
  }

  // if you have Failed to run image Error: docker: invalid reference format.
  // case also you have spaces between params

  echo "DOCKER_OPTS_COMPOSE : ${DOCKER_OPTS_COMPOSE}"

  return DOCKER_OPTS_COMPOSE.trim()

}
