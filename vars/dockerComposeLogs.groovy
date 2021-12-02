#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/dockerComposeLogs.groovy`'

  vars = vars ?: [:]

  def CLEAN_RUN = vars.get('CLEAN_RUN', env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get('DRY_RUN', env.DRY_RUN ?: false).toBoolean()
  def DEBUG_RUN = vars.get('DEBUG_RUN', env.DEBUG_RUN ?: false).toBoolean()
  //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
  //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
  //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

  vars.dockerFilePath = vars.get('dockerFilePath', '').trim()
  vars.dockerComposeFiles = vars.get('dockerComposeFiles', "-f ${vars.dockerFilePath}docker-compose.yml").trim()
  vars.DOCKER_TAG = vars.get('DOCKER_TEST_TAG', env.DOCKER_TEST_TAG ?: 'temp').trim()
  vars.DOCKER_TEST_TAG = dockerTag(vars.DOCKER_TAG)
  vars.DOCKER_COMPOSE_OPTIONS = vars.get('DOCKER_COMPOSE_OPTIONS', env.DOCKER_COMPOSE_OPTIONS ?: '').trim() // -p ${vars.DOCKER_TEST_TAG}

  vars.shellOutputFile = vars.get('shellOutputFile', 'docker-compose-logs.log').trim()

  script {
    tee("${vars.shellOutputFile}") {
      try {
        //if (DEBUG_RUN) {
        sh """#!/bin/bash -l
        rm -f ${pwd()}/logs/*
        mkdir -p ${pwd()}/logs

        docker images 2>&1 > ${pwd()}/logs/docker-images.log
        docker volume ls 2>&1 > ${pwd()}/logs/docker-volumes.log
        docker ps -a 2>&1 > ${pwd()}/logs/docker-ps.log

            docker-compose ${vars.dockerComposeFiles} ${vars.DOCKER_COMPOSE_OPTIONS} ps

            for i in \$(docker-compose ${vars.dockerComposeFiles} ${vars.DOCKER_COMPOSE_OPTIONS} ps -q); do docker container logs --details \$i >& ${pwd()}/logs\$(docker inspect --format='{{.Name}}' \$i).docker.log; done
            for i in \$(docker-compose ${vars.dockerComposeFiles} ${vars.DOCKER_COMPOSE_OPTIONS} ps -q); do docker container inspect \$i >& ${pwd()}/logs\$(docker inspect --format='{{.Name}}' \$i).docker.inspect; done
        """
        //} // DEBUG_RUN

        if (body) { body() }
      } catch (exc) {
        echo 'Warning: There were errors retrieving compose tests logs. ' + exc
      } finally {
        cleanEmptyFile(vars)
        archiveArtifacts artifacts: "${vars.shellOutputFile}, logs/*.log", onlyIfSuccessful: false, allowEmptyArchive: true
        echo "Check : ${env.BUILD_URL}/artifact/logs/docker-ps.log"
        echo "Check : ${env.BUILD_URL}/artifact/logs/docker-images.log"
      }
      } // tee
    } // script
}
