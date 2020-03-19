#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getDockerOpts.groovy`"

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  String DOCKER_OPTS_USER_ID = [
     '-v /etc/passwd:/etc/passwd:ro',
     '-v /etc/group:/etc/group:ro',
//     '--group-add 2000',
//     '--group-add 1101',
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
          "-v ${vars.JENKINS_USER_HOME}:/home/jenkins"
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
