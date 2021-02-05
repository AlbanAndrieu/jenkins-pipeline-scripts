#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/cleanDockerConfig.groovy`"

    vars.isCleaningDockerConfig = vars.get("isCleaningDockerConfig", false).toBoolean()

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()

    def JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins").trim()
    vars.dockerConfigPath = vars.get("dockerConfigPath", env.dockerConfigPath ?: "${JENKINS_USER_HOME}/.docker").trim()

    if (isUnix()) {
        if (vars.isCleaningDockerConfig == true || CLEAN_RUN == true) {
          sh """#!/bin/bash -l
          rm -rf ${vars.dockerConfigPath}/config.json || true
          rm -rf ${JENKINS_USER_HOME}/.dockercfg || true
          """
        }
    } // isUnix

    if (body) { body() }

}
