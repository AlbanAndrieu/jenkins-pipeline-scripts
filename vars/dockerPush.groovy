#!/usr/bin/groovy
import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

//def call(Closure body=null) {
//    this.vars = [:]
//    call(vars, body)
//}

// To be used with draft

def call(Map containers, Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/dockerPush.groovy`'

  vars = vars ?: [:]

  vars.DOCKER_ORGANISATION = vars.get('DOCKER_ORGANISATION', env.DOCKER_ORGANISATION ?: 'nabla').trim()

  vars.DOCKER_REGISTRY = vars.get('DOCKER_REGISTRY', env.DOCKER_REGISTRY ?: 'registry.hub.docker.com').toLowerCase().trim()  // 'https://index.docker.io/v1/'
  vars.DOCKER_REGISTRY_HUB_URL = vars.get('DOCKER_REGISTRY_HUB_URL', env.DOCKER_REGISTRY_HUB_URL ?: "https://${vars.DOCKER_REGISTRY_HUB}").trim()
  vars.DOCKER_REGISTRY_CREDENTIAL = vars.get('DOCKER_REGISTRY_CREDENTIAL', env.DOCKER_REGISTRY_CREDENTIAL ?: 'hub-docker-nabla').trim()

  vars.dockerTag = vars.get('dockerTag', env.DOCKER_TAG ?: 'temp').trim()
  vars.isPushEnabled = vars.get('isPushEnabled', true).toBoolean()

  if (body) { body() }

  if (vars.isPushEnabled ) {
    if (containers.size() == 0) {
      echo 'WARNING: No images found to deploy'
    } else {
      for (image in mapToList(containers)) {
        echo "Pushing : ${image[0]} - ${image[1]}"

        step([$class: 'DockerBuilderPublisher',
          cleanImages: true,
          cleanupWithJenkinsJobDelete: true,
          cloud: '',
          dockerFileDirectory: '',
          fromRegistry: [credentialsId: vars.DOCKER_REGISTRY_CREDENTIAL, url: vars.DOCKER_REGISTRY_HUB_URL],
          noCache: true,
          pull: false,
          pushCredentialsId: vars.DOCKER_REGISTRY_CREDENTIAL,
          pushOnSuccess: true,
          tagsString: image[0]])

      //pushDockerImage(image[1], "${vars.DOCKER_REGISTRY}/${vars.DOCKER_ORGANISATION}/${image[0]}", "${env.DOCKER_TAG}")
      //pushDockerImage is used for compatibliy purpose but it only doing container.push() for develop and is adding a latest tag
      }
    }
  } // isPushEnabled
}

def call(Map containers, Closure body=null) {
  echo '[JPL] Executing `vars/dockerPush.groovy`'

  vars = [:]

  call(containers, vars, body)
}

@NonCPS List<List<?>> mapToList(Map map) {
  return map.collect { it ->
    [it.key, it.value]
  }
}
