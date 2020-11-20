#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    this.containers = [:]
    call(vars, containers, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/draftStage.groovy`"

  vars = vars ?: [:]

  containers = containers ?: [:]

  call(vars, containers, body)

}

def call(Map vars, Map containers, Closure body=null) {

  echo "[JPL] Executing `vars/draftStage.groovy`"

  vars = vars ?: [:]

  String DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla").trim()

  String DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.hub.docker.com").toLowerCase().trim()
  String DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
  String DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "hub-docker-nabla").trim()

  String DRAFT_BRANCH = vars.get("DRAFT_BRANCH", params.DRAFT_BRANCH ?: "develop").trim()

  vars.draftPack = vars.get("draftPack", "nabla").trim()
  vars.imageName = vars.get("imageName", env.JOB_BASE_NAME).replaceAll("/", "-").replaceAll("%2F", "-").toLowerCase().trim()   // Target imageName should be provided by dev
  vars.dockerFileName = vars.get("dockerFileName", "Dockerfile").trim()
  vars.dockerTag = vars.get("dockerTag", env.DOCKER_TAG ?: "latest").toLowerCase().trim()
  vars.buildArgs = vars.get("buildArgs", "").trim()
  vars.buildArgs += [getDockerProxyOpts()].join(" ")

  vars.image = vars.get("image", "${DOCKER_ORGANISATION}/${vars.imageName}:${vars.dockerTag}").trim()

  vars.configFile = vars.get("configFile", "config.yaml").trim()
  vars.skipFailure = vars.get("skipFailure", true).toBoolean()
  vars.skipContainers = vars.get("skipContainers", false).toBoolean()
  vars.skipAqua = vars.get("skipAqua",false).toBoolean()
  vars.skipCST = vars.get("skipCST",false).toBoolean()

  vars.pomFile = vars.get("pomFile", "pom.xml").trim()
  vars.pomFile += "../" + vars.pomFile

  containers = containers ?: [:]

  dir("${vars.imageName}") {

    vars.buildDir = vars.get("buildDir", "${pwd()}").trim()
    vars.dockerFileBuildPath = vars.get("buildDir", "./").trim()  // dockerTargetPath

    draftPack(vars, body)

    dockerHadoLint(vars)

    if (!vars.skipContainers) {
      containers.put("${vars.imageName}", docker.build("${vars.image}", "${vars.buildArgs} -f ${vars.dockerFileBuildPath}/${vars.dockerFileName} ${vars.buildDir}"))
    }

    vars.localImage = vars.get("image", "${DOCKER_ORGANISATION}/${vars.imageName}:${vars.dockerTag}").trim()
    vars.imageTag = vars.get("dockerTag", env.DOCKER_TAG ?: "latest").toLowerCase().trim()
    vars.registry = vars.get("registry", "${DOCKER_REGISTRY}").toLowerCase().trim()
    vars.locationType = vars.get("locationType", "local").toLowerCase().trim()

    withAquaWrapper(vars)

    withCSTWrapper(vars)

    helmPush(vars)

  } // dir

  return containers

}
