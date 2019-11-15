#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/withCSTWrapper.groovy`"

  vars = vars ?: [:]

  def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
  def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
  def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
  def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

  def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.misys.global.ad").trim()
  def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
  def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "mgr.jenkins").trim()

  def CST_VERSION = vars.get("CST_VERSION", env.CST_VERSION ?: '1.0.5').trim()

  vars.configFile = vars.get("configFile", env.configFile ?: 'config.yaml').trim()
  vars.scanner_image = vars.get("scanner_image", "gcr.io/gcp-runtimes/container-structure-test:${CST_VERSION}").trim()
  vars.imageName = vars.get("imageName", "todo").trim()
  vars.buildCmdParameters = vars.get("buildCmdParameters", "docker pull ${vars.scanner_image}").trim()
  vars.buildCmd = vars.get("buildCmd", "").trim()
  vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
  vars.shellOutputFile = vars.get("shellOutputFile", "cst.log").trim()

  try {
    //tee("${vars.shellOutputFile}") {

    //docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {

      try {

        vars.buildCmdParameters+=" && docker run"
        vars.buildCmdParameters+=" --rm"
        vars.buildCmdParameters+=" --volume '${pwd()}:/data'"
        vars.buildCmdParameters+=" --volume /var/run/docker.sock:/var/run/docker.sock"
        vars.buildCmdParameters+=" ${vars.scanner_image}"
        vars.buildCmdParameters+=        " test "
        vars.buildCmdParameters+=        " --image ${vars.imageName} "
        vars.buildCmdParameters+=        " --config /data/${vars.configFile}"

        vars.buildCmdParameters += " && docker run --rm --volume ${pwd()}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ubuntu chown -R \$(id -u):\$(id -g) ."

        if (vars.buildCmdParameters?.trim()) {
            vars.buildCmd += " ${vars.buildCmdParameters}"
        }

        // TODO Remove it when tee will be back
        vars.buildCmd += " 2>&1 > ${vars.shellOutputFile} "
        //vars.buildCmd +=        " > /dev/null"

        // Run the cst build
        build = sh (
                script: "${vars.buildCmd}",
                returnStatus: true
                )
        echo "CST RETURN CODE : ${build}"
        if (build == 0) {
            echo "CST SUCCESS"
        } else {
            echo "CST FAILURE"
            if (!vars.skipFailure) {
                currentBuild.result = 'UNSTABLE'
                //error 'There are errors in cst'
            }
        }
        if (body) { body() }

      } catch (exc) {
        echo "Warn: There was a problem with cst scan image \'${vars.imageName}\' \'${vars.configFile}\' " + exc.toString()
      }

    //}  // withRegistry


    //} // tee
  } finally {
    archiveArtifacts artifacts: "*.log", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
  }

}
