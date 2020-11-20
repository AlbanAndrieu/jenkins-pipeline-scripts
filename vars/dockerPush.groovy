#!/usr/bin/groovy
import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

//def call(Closure body=null) {
//    this.vars = [:]
//    call(vars, body)
//}

// To be used with draft

def call(Map containers, Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/dockerPush.groovy`"

  vars = vars ?: [:]

  String DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla").trim()
  String DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.hub.docker.com").toLowerCase().trim()

  vars.dockerTag = vars.get("dockerTag", env.DOCKER_TAG ?: "temp").trim()
  vars.isPushEnabled = vars.get("isPushEnabled", true).toBoolean()

  if (body) { body() }

  if (vars.isPushEnabled ) {

    if (containers.size() == 0) {
      echo "WARNING: No images found to deploy"
    } else {
      for (image in mapToList(containers)) {
        echo "Pushing : ${image[0]} - ${image[1]}"
        pushDockerImage(image[1], "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${image[0]}", "${env.DOCKER_TAG}")
        //pushDockerImage is used for compatibliy purpose but it only doing container.push() for develop and is adding a latest tag
      }
    }

  } // isPushEnabled

}

def call(Map containers, Closure body=null) {

  echo "[JPL] Executing `vars/dockerPush.groovy`"

  vars = [:]

  call(containers, vars, body)

}

@NonCPS List<List<?>> mapToList(Map map) {
  return map.collect { it ->
    [it.key, it.value]
  }
}
