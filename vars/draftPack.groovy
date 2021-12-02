#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/draftPack.groovy`'

  vars = vars ?: [:]

  vars.DOCKER_REGISTRY = vars.get('DOCKER_REGISTRY', env.DOCKER_REGISTRY ?: 'registry.hub.docker.com').toLowerCase().trim()
  vars.DOCKER_REGISTRY_URL = vars.get('DOCKER_REGISTRY_URL', env.DOCKER_REGISTRY_URL ?: "https://${vars.DOCKER_REGISTRY}").trim()
  vars.DOCKER_REGISTRY_CREDENTIAL = vars.get('DOCKER_REGISTRY_CREDENTIAL', env.DOCKER_REGISTRY_CREDENTIAL ?: 'hub-docker-nabla').trim()

  vars.DRAFT_BRANCH = vars.get('DRAFT_BRANCH', params.DRAFT_BRANCH ?: 'develop').trim()
  vars.DRAFT_REPOS = vars.get('DRAFT_REPOS', env.DRAFT_REPOS ?: '').trim()
  vars.DRAFT_VERSION = vars.get('DRAFT_VERSION', env.DRAFT_VERSION ?: '0.0.1').trim()

  vars.draftPack = vars.get('draftPack', 'nabla').trim()
  vars.helmChartName = vars.get('helmChartName', vars.imageName ?: getGitRepoName(vars)).toLowerCase().trim()

  vars.skipDraftPack = vars.get('skipDraftPack', false).toBoolean()
  vars.skipDraftInit = vars.get('skipDraftInit', false).toBoolean()
  vars.skipDraftUpdate = vars.get('skipDraftUpdate', false).toBoolean()

  vars.buildDir = vars.get('buildDir', "${pwd()}").trim()

  String DRAFT_IMAGE = "${vars.DOCKER_REGISTRY}/nabla/draft:${vars.DRAFT_VERSION}"
  String ALPINE_IMAGE = "${vars.DOCKER_REGISTRY}/alpine:latest"

  if (!vars.skipDraftPack) {
    try {
      if (!vars.skipDraftInit) {
        sh 'draft init || true'
      //sh "draft pack-repo add git@github.com:AlbanAndrieu/draft-packs.git || true"
      }

          //sh "draft pack-repo remove github.com/Azure/draft || true"

      if (!vars.skipDraftUpdate) {
        sh 'draft pack-repo update || true'
      }

      sh 'draft pack list || true'

      configFileProvider([configFile(fileId: 'jenkins', targetLocation: 'id_rsa')]) {
        withRegistryWrapper(dockerRegistry: DOCKER_REGISTRY, dockerRegistryCredentials: DOCKER_REGISTRY_CREDENTIAL) {
            try {
            docker.image("${DRAFT_IMAGE}").pull()
            } catch (exc) {
            echo "Warn: There was a problem pulling \'${DRAFT_IMAGE}\' " + exc.toString()
            }

            echo "docker run --env DRAFT_BRANCH='${vars.DRAFT_BRANCH}' --env REPOS='${vars.DRAFT_REPOS}' --rm --volume ${pwd()}/id_rsa:/home/nabla/id_rsa --volume ${vars.buildDir}:/home/nabla/docker --workdir /home/nabla/docker ${DRAFT_IMAGE} create -p ${vars.draftPack} -a ${vars.helmChartName}"

          sh """#!/bin/bash -l
                        chmod 0777 ${pwd()}/id_rsa
                        chmod 0777 ${vars.buildDir}
                        chmod -R 0777 ${pwd()}
                        docker run --env DRAFT_BRANCH='${vars.DRAFT_BRANCH}' --env REPOS='${vars.DRAFT_REPOS}' --rm --volume ${pwd()}/id_rsa:/home/nabla/id_rsa --volume ${vars.buildDir}:/home/nabla/docker --workdir /home/nabla/docker ${DRAFT_IMAGE} create -p ${vars.draftPack} -a ${vars.helmChartName}
                        docker run --rm --volume ${vars.buildDir}:/home/nabla/docker --volume ${vars.buildDir}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ${ALPINE_IMAGE} chown -R \$(id -u):\$(id -g) .
            rm -f ${pwd()}/id_rsa
            pwd
            ls -lrta .
            ls -lrta charts || true
            ls -lrta charts/${vars.helmChartName} || true
            """

            // when using draft charts are inside the directory
            // helmDir must be relatif, never ${pwd()}/charts
            echo 'Overriding default values to match draftPack structure :'
            vars.helmDir = './charts'
            vars.buildDockerDir = vars.buildDir
            vars.pomFile = vars.get('pomFile', '../pom.xml').trim()
            echo "helmDir: ${vars.helmDir} - buildDockerDir: ${vars.buildDockerDir} - pomFile: ${vars.pomFile}"

            echo "Draft installed inside ${pwd()}/charts/${vars.helmChartName}"
            } // withRegistryWrapper
          } // configFileProvider
        } catch (exc) {
      echo "Warn: There was a problem with install of draft pack \'${vars.draftPack}\' from \'${vars.DRAFT_BRANCH}\' " + exc.toString()
    }
  } else {
    echo 'Draft pack skipped'
  }
}
