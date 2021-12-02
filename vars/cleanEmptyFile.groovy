#!/usr/bin/env groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/cleanEmptyFile.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.DOCKER_REGISTRY = vars.get('DOCKER_REGISTRY_HUB', env.DOCKER_REGISTRY_HUB ?: '').toLowerCase().trim()

  vars.isCleaningEmptyFileEnabled = vars.get('isCleaningEmptyFileEnabled', true).toBoolean()

  vars.DOCKER_FIND_IMAGE = vars.get('DOCKER_FIND_IMAGE', "${vars.DOCKER_REGISTRY}/alpine:latest").trim()

  vars.dockerFindCmd = vars.get('dockerFindCmd', "docker run --rm -i -v ${pwd()}:/src -w /src ${vars.DOCKER_FIND_IMAGE} find . -mindepth 1 -maxdepth 1 -size  0 -exec rm -r {} +").trim()

  vars.isCleaningEmptyFileEnabled = vars.get('isCleaningEmptyFileEnabled', true).toBoolean()

  if (vars.isCleaningEmptyFileEnabled == true) {
    try {
      withRegistryWrapper(dockerRegistry: vars.DOCKER_REGISTRY_ACR, dockerRegistryCredentials: vars.DOCKER_REGISTRY_ACR_CREDENTIAL) {
        timeout(time: 3, unit: 'MINUTES') {
          sh "${vars.dockerFindCmd}"
        } // timeout
      }
    } catch (exc) {
      echo 'Warn: There was a problem with cleaning empty files ' + exc
    }
  }

  if (body) { body() }
}
