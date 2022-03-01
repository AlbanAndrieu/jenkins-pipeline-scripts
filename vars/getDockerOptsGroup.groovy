#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo '[JPL] Executing `vars/getDockerOptsGroup.groovy`'

  vars = vars ?: [:]

  String DOCKER_OPTS_USER_ID = [
     '-v /etc/passwd:/etc/passwd:ro',
     '-v /etc/group:/etc/group:ro',
     '--group-add 2000', // docker
     '--group-add 998', // microk8s
     '--group-add 789', // newman
  ].join(' ')

  echo "DOCKER_OPTS_USER_ID : ${DOCKER_OPTS_USER_ID}"

  return DOCKER_OPTS_USER_ID
}
