#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/withCSTWrapper.groovy`"

  vars = vars ?: [:]

  def CST_VERSION = vars.get("CST_VERSION", env.CST_VERSION ?: 'latest').trim()

  vars.configFile = vars.get("configFile", env.configFile ?: 'config.yaml').trim()
  vars.scanner_image = vars.get("scanner_image", "gcr.io/gcp-runtimes/container-structure-test:${CST_VERSION}").trim()
  vars.imageName = vars.get("imageName", "todo").trim()
  vars.buildCmdParameters = vars.get("buildCmdParameters", "docker pull ${vars.scanner_image}").trim()
  vars.buildCmd = vars.get("buildCmd", "").trim()
  vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
  vars.shellOutputFile = vars.get("shellOutputFile", "cst.log").trim()
  vars.skipFailure = vars.get("skipFailure", false).toBoolean()

  try {
    //tee("${vars.shellOutputFile}") {

      try {
      
        vars.buildCmdParameters+=" && docker pull ${DOCKER_RUNTIME_IMG}"

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
            if (!vars.skipFailure) {
                echo "CST UNSTABLE"
                currentBuild.result = 'UNSTABLE'
            } else {
                echo "CST FAILURE skipped"
                //error 'There are errors in cst'
            }
        }
        if (body) { body() }

      } catch (exc) {
        //currentBuild.result = 'FAILURE'
        echo "WARNING : There was a problem with cst scan image \'${vars.imageName}\' \'${vars.configFile}\' " + exc.toString()
      }

    //} // tee
  } finally {
    archiveArtifacts artifacts: "${vars.shellOutputFile}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
  }

}
