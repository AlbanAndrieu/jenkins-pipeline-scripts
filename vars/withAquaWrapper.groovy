#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/withAquaWrapper.groovy`"

  vars = vars ?: [:]

  getJenkinsOpts(vars)


  vars.DOCKER_RUNTIME_TAG = vars.get("DOCKER_RUNTIME_TAG", env.DOCKER_RUNTIME_TAG ?: "latest").trim()
  vars.DOCKER_NAME_RUNTIME = vars.get("DOCKER_NAME_RUNTIME", env.DOCKER_NAME_RUNTIME ?: "ansible-jenkins-slave").trim()

  vars.AQUA_URL = vars.get("AQUA_URL", env.AQUA_URL ?: "http://aqua:8080/").trim()

  vars.imageName = vars.get("imageName", "${vars.DOCKER_NAME_RUNTIME}").trim()
  vars.imageTag = vars.get("imageTag", "${vars.DOCKER_RUNTIME_TAG}").trim()

  vars.registry = vars.get("registry", "${vars.DOCKER_REGISTRY_TMP}").trim()

  vars.hostedImage = vars.get("hostedImage", "${vars.DOCKER_ORGANISATION}/${vars.imageName}:${vars.imageTag}").trim()
  vars.localImage = vars.get("localImage", "${vars.DOCKER_ORGANISATION}/${vars.imageName}:${vars.imageTag}").trim()

  vars.locationType = vars.get("locationType", "hosted").trim() // hosted or local
  vars.register = vars.get("register", true).toBoolean()

  vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
  vars.aquaFileId = vars.get("aquaFileId", vars.draftPack ?: "0").trim()
  vars.aquaOutputFile = vars.get("aquaOutputFile", "aqua-${vars.aquaFileId}.log").trim()
  vars.skipAquaFailure = vars.get("skipAquaFailure", false).toBoolean()
  vars.skipAqua = vars.get("skipAqua", true).toBoolean()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
	  vars.skipAqua = true
  }

  if (!vars.skipAqua) {
    try {
      //tee("${vars.aquaOutputFile}") {

      if (vars.locationType.trim() == "local") {
         vars.register = false // needed when is empty registry
         vars.hostedImage = ""
      }

      //withRegistryWrapper(dockerRegistry: vars.DOCKER_REGISTRY_ACR, dockerRegistryCredentials: vars.DOCKER_REGISTRY_ACR_CREDENTIAL) {
		    aqua customFlags: '', hideBase: true, hostedImage: vars.hostedImage, localImage: vars.localImage, locationType: vars.locationType, notCompliesCmd: '',
		      onDisallowed: 'ignore', policies: '',
		      register: vars.register,
		      registry: vars.registry,
		      showNegligible: true
      //} // withRegistryWrapper

      if (body) { body() }

      //} // tee
    } catch (exc) {
      if (!vars.skipAquaFailure) {
          echo "AQUA UNSTABLE"
          currentBuild.result = 'UNSTABLE'
      } else {
          echo "AQUA FAILURE skipped"
          //error 'There are errors in aqua' // not needed
      }
      echo "WARNING : Scan failed, check output at \'${env.BUILD_URL}artifact/${vars.aquaOutputFile}\' "
      echo "WARNING : There was a problem with aqua scan : " + exc.toString()
      echo "Check on : ${vars.AQUA_URL}"
    } finally {
      archiveArtifacts artifacts: "${vars.aquaOutputFile}, scanout*.html, styles.css", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo "Aqua scan skipped"
  }

}
