#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/draftPack.groovy`"

  vars = vars ?: [:]

  String DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.hub.docker.com").toLowerCase().trim()
  String DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
  String DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "hub-docker-nabla").trim()

  String DRAFT_BRANCH = vars.get("DRAFT_BRANCH", params.DRAFT_BRANCH ?: "develop").trim()
  String DRAFT_VERSION = vars.get("DRAFT_VERSION", env.DRAFT_VERSION ?: "0.0.1").trim()

  vars.draftPack = vars.get("draftPack", "nabla").trim()
  vars.helmChartName = vars.get("helmChartName", vars.imageName ?: env.JOB_BASE_NAME).trim()

  vars.buildDir = vars.get("buildDir", "${pwd()}").trim()

  String DRAFT_IMAGE = "${DOCKER_REGISTRY}/nabla/draft:${DRAFT_VERSION}"
  String ALPINE_IMAGE = "${DOCKER_REGISTRY}/alpine:latest"

  try {
    //sh "draft pack-repo remove github.com/Azure/draft || true"

    //sh "draft pack-repo update || true"
    sh "draft pack list || true"

    configFileProvider([configFile(fileId: 'jenkins', targetLocation: 'id_rsa')]) {

      withRegistryWrapper(dockerRegistry: DOCKER_REGISTRY, dockerRegistryCredentials: DOCKER_REGISTRY_CREDENTIAL) {

         sh "docker pull ${DRAFT_IMAGE} || true"

		    if (body) { body() }

		    sh """#!/bin/bash -l
		    chmod 0777 ${pwd()}/id_rsa
		    chmod 0777 ${vars.buildDir}
		    chmod -R 0777 ${pwd()}
		    docker run --env DRAFT_BRANCH=${DRAFT_BRANCH} --rm --volume ${pwd()}/id_rsa:/home/nabla/id_rsa --volume ${vars.buildDir}:/home/finastra/docker --workdir /home/nabla/docker ${DRAFT_IMAGE} create -p ${vars.draftPack} -a ${vars.helmChartName}
		    docker run --rm --volume ${vars.buildDir}:/home/nabla/docker --volume ${vars.buildDir}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ${ALPINE_IMAGE} chown -R \$(id -u):\$(id -g) .
		    rm -f ${pwd()}/id_rsa"""

      } // withRegistryWrapper

    } // configFileProvider
  } catch (exc) {
    echo "Warn: There was a problem with install of draft pack \'${vars.draftPack}\' from \'${DRAFT_BRANCH}\' " + exc.toString()
  }

}
