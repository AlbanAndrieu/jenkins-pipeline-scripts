#!/usr/bin/env groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/cleanEmptyFile.groovy`"

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY_HUB", env.DOCKER_REGISTRY_HUB ?: "").toLowerCase().trim()

  vars.isCleaningEmptyFileEnabled = vars.get("isCleaningEmptyFileEnabled", true).toBoolean()

  def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

  if (vars.isCleaningEmptyFileEnabled == true) {
    sh "docker run --rm -v ${pwd()}:/src -w /src ${vars.DOCKER_REGISTRY}/alpine:latest find . -mindepth 1 -maxdepth 1 -size  0 -exec rm -r {} +"
  }

  if (body) { body() }

}
