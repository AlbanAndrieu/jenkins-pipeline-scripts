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
  // Target imageName should be provided by dev
  vars.imageName = vars.get("imageName",  vars.draftPack ?: getGitRepoName(vars)).replaceAll("/", "-").replaceAll("%2F", "-").toLowerCase().trim()
  vars.dockerFileName = vars.get("dockerFileName", "Dockerfile").trim()
  vars.dockerTag = vars.get("dockerTag", env.DOCKER_TAG ?: "latest").toLowerCase().trim()
  vars.buildArgs = vars.get("buildArgs", "").trim()
  vars.buildArgs += [getDockerProxyOpts()].join(" ")

  // The directory that contains a chart MUST have the same name as the chart.
  // See https://helm.sh/docs/chart_best_practices/conventions/
  // helmDir must be relatif, never ${pwd()}/charts
  // draftPack if enable wil loverride helmDir
  vars.helmDir = vars.get("helmDir", "./charts").toLowerCase().trim()
  // draftPack if enable wil loverride buildDockerDir
  vars.buildDockerDir = vars.get("buildDockerDir", vars.buildDir ?: "./packs").trim()

  vars.image = vars.get("image", "${DOCKER_ORGANISATION}/${vars.imageName}:${vars.dockerTag}").trim()

  vars.configFile = vars.get("configFile", "config.yaml").trim()

  vars.skipDraftStage = vars.get("skipDraftStage", false).toBoolean()

  vars.skipContainers = vars.get("skipContainers", false).toBoolean()
  vars.skipAqua = vars.get("skipAqua",false).toBoolean()
  vars.skipCST = vars.get("skipCST",false).toBoolean()

  vars.pomFile = vars.get("pomFile", "../pom.xml").trim() // No pom.xml on draft pack. they are in product directory, just the directory before

  containers = containers ?: [:]

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
      vars.skipDraftStage = true
  }

  if (!vars.skipDraftStage) {
    // Draft is making a copy of the packs inside a local directory ${vars.imageName}/charts
		dir("${vars.imageName}") {

      echo "[JPL] Executing `vars/draftStage.groovy` FOR ${vars.draftPack}"

		  vars.buildDir = vars.get("buildDir", "${pwd()}").trim()
		  vars.dockerFileBuildPath = vars.get("buildDir", "./").trim()  // dockerTargetPath

      if (body) { body() }

		  draftPack(vars, body)

		  dockerHadoLint(vars)

      if (fileExists("${vars.dockerFileBuildPath}/${vars.dockerFileName}")) {
				if (!vars.skipContainers) {
          // Add docker login when draftPack is disabled (skipDraftPack: true)
          withRegistryWrapper(dockerRegistry: vars.DOCKER_REGISTRY, dockerRegistryCredentials: vars.DOCKER_REGISTRY_CREDENTIAL) {
				    containers.put("${vars.imageName}", docker.build("${vars.image}", "${vars.buildArgs} -f ${vars.dockerFileBuildPath}/${vars.dockerFileName} ${vars.buildDockerDir}"))
          } // withRegistryWrapper
				}

				vars.localImage = vars.get("image", "${DOCKER_ORGANISATION}/${vars.imageName}:${vars.dockerTag}").trim()
				vars.imageTag = vars.get("dockerTag", env.DOCKER_TAG ?: "latest").toLowerCase().trim()
				vars.registry = vars.get("registry", "${DOCKER_REGISTRY_TMP}").toLowerCase().trim()
				vars.locationType = vars.get("locationType", "local").toLowerCase().trim()

        withCSTWrapper(vars)
      } else {
        echo "No fileExists(${vars.dockerFileBuildPath}/${vars.dockerFileName})"
      }
		  withAquaWrapper(vars)

      if (vars.skipDraftPack.toBoolean()) {
        // Draft is making a copy of the packs inside a local directory, we are doing the same here
        echo "Move charts from dir ../packs/${vars.imageName}/charts/* to local (${vars.imageName}/charts/${vars.imageName})"
        sh "mkdir -p charts/${vars.imageName} && cp -R ../packs/${vars.imageName}/charts/* ./charts/${vars.imageName} || true"
        //vars.helmDir = vars.get("helmDir", "./packs").toLowerCase().trim()
      }

		  helmPush(vars)

		} // dir
  } else {
    echo "Draft stage skipped"
  }

  return containers

}
