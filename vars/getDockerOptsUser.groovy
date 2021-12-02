#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/getDockerOptsUser.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  String DOCKER_OPTS_USER = ''

  if (vars.isRoot == true) {
    DOCKER_OPTS_USER += ' -u root:root '
  } else if (vars.isJenkinsUser == true) {
    DOCKER_OPTS_USER += ' -u 2000:2000 '
  } else if (vars.isLocalUser == true) {
    DOCKER_OPTS_USER += ' -u $(id -u) '
  }

  echo "DOCKER_OPTS_USER : ${DOCKER_OPTS_USER}"

  return DOCKER_OPTS_USER
}
