#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/withAquaWrapper.groovy`"

  vars = vars ?: [:]

  //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  //def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

  String DOCKER_REGISTRY="registry".trim()
  String DOCKER_ORGANISATION="nabla".trim()
  
  String DOCKER_REGISTRY_URL="https://${DOCKER_REGISTRY}".trim()
  
  String DOCKER_RUNTIME_TAG="latest".trim()
  String DOCKER_NAME_RUNTIME="ansible-jenkins-slave-docker".trim()
  //String DOCKER_RUNTIME_IMG="${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_NAME_RUNTIME}:${DOCKER_RUNTIME_TAG}".trim()

  String AQUA_URL="http://fr1cslbmts0304:8080/".trim()

  vars.imageName = vars.get("imageName", "${DOCKER_NAME_RUNTIME}").trim()
  vars.imageTag = vars.get("imageTag", "${DOCKER_RUNTIME_TAG}").trim()

  vars.registry = vars.get("registry", "${DOCKER_REGISTRY}").trim()
  vars.hostedImage = vars.get("hostedImage", "${DOCKER_ORGANISATION}/${vars.imageName}:${vars.imageTag}").trim()
  vars.localImage = vars.get("localImage", "${DOCKER_ORGANISATION}/${vars.imageName}:${vars.imageTag}").trim()
  
  vars.locationType = vars.get("locationType", "hosted").trim() // hosted or local
  vars.register = vars.get("register", true).toBoolean()

  vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
  vars.aquaOutputFile = vars.get("aquaOutputFile", "aqua.log").trim()
  vars.skipFailure = vars.get("skipFailure", false).toBoolean()

  try {
    //tee("${vars.aquaOutputFile}") {
    
    if (vars.locationType.trim() == "local") {
       vars.register = false // needed when is empty registry
       vars.hostedImage = ""
    }

	aqua customFlags: '', hideBase: true, hostedImage: vars.hostedImage, localImage: vars.localImage, locationType: vars.locationType, notCompliesCmd: '', onDisallowed: 'ignore', policies: '', register: vars.register, registry: vars.registry, showNegligible: true

	if (body) { body() }

    //} // tee
  } catch (exc) {
	if (!vars.skipFailure) {
		echo "AQUA UNSTABLE"
		currentBuild.result = 'UNSTABLE'
	} else {
		echo "AQUA FAILURE skipped"
		//error 'There are errors in aqua' // not needed
	}  
	echo "WARNING : Scan failed, check output at \'${vars.aquaOutputFile}\' "
	echo "WARNING : There was a problem with aqua scan : " + exc.toString()    	
	echo "Check on : ${AQUA_URL}"
  } finally {
    archiveArtifacts artifacts: "${vars.aquaOutputFile}, aqua.html", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
  }

}
