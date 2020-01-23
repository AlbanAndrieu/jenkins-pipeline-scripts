#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/withAquaWrapper.groovy`"

  vars = vars ?: [:]

  def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
  def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
  def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
  def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

  def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.mobi").toLowerCase().trim()
  def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
  def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins").trim()

  def AQUA_VERSION = vars.get("AQUA_VERSION", env.AQUA_VERSION ?: "latest").trim()
  //def AQUA_USER = vars.get("AQUA_USER", env.AQUA_USER ?: '--user scanner').trim()
  def ASLOGIN = vars.get("ASLOGIN", env.ASLOGIN ?: 'scanner').trim()
  //def AQUA_PASS = vars.get("AQUA_PASS", env.AQUA_PASS ?: '--password password').trim()
  def ASPASSWORD = vars.get("ASPASSWORD", env.ASPASSWORD ?: 'password').trim()

  //def AQUA_HOST = vars.get("AQUA_HOST", env.AQUA_HOST ?: '--host http://aqua:8080').trim()
  def ASURI = vars.get("ASURI", env.ASURI ?: 'http://aqua:8080').trim()

  def AQUA_REPORT = vars.get("AQUA_REPORT", env.AQUA_REPORT ?: 'aqua.html').trim()

  String AQUA_OPTS = [
    '--local',
    '--direct-cc',
    "--jsonfile /mnt/${AQUA_REPORT}.AquaSec",
    "--htmlfile /mnt/${AQUA_REPORT}"
  ].join(" ")

  // docker login registry.aquasec.com
  // docker pull registry.aquasec.com/scanner:4.2
  vars.scanner_image = vars.get("scanner_image", "${DOCKER_REGISTRY}/nabla/aquasec-scanner-cli:${AQUA_VERSION}").trim()
  vars.imageName = vars.get("imageName", "").trim()
  vars.reportName = vars.get("reportName", "").trim()
  vars.buildCmdParameters = vars.get("buildCmdParameters", "docker pull ${vars.scanner_image}").trim()
  vars.buildCmd = vars.get("buildCmd", "").trim()
  vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
  vars.shellOutputFile = vars.get("shellOutputFile", "aqua.log").trim()
  vars.skipFailure = vars.get("skipFailure", false).toBoolean()

  try {
    //tee("${vars.shellOutputFile}") {

    //docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {

      try {
        sh "rm -f ${pwd()}/${AQUA_REPORT}* || true"

        vars.buildCmdParameters+=" && docker run"
        vars.buildCmdParameters+=" --rm"
        vars.buildCmdParameters+=" --volume '${pwd()}:/mnt'"
        vars.buildCmdParameters+=" --volume /var/run/docker.sock:/var/run/docker.sock"
        vars.buildCmdParameters+=" ${vars.scanner_image}"

        vars.buildCmdParameters+=        " scan"
        //vars.buildCmdParameters+=        " import"

        vars.buildCmdParameters+=        " --user '${ASLOGIN}'"
        vars.buildCmdParameters+=        " --password '${ASPASSWORD}'"

        vars.buildCmdParameters+=        " --host '${ASURI}'"
        vars.buildCmdParameters+=        " ${AQUA_OPTS} "
        ////vars.buildCmdParameters+=        " /mnt/${reportJsonPath}"
        vars.buildCmdParameters+=        " ${vars.imageName} > /dev/null"

        //vars.buildCmdParameters+ = " && docker run --rm --volume ${pwd()}:/mnt --volume /var/run/docker.sock:/var/run/docker.sock ${vars.scanner_image} "
        //vars.buildCmdParameters+ = " scan --user ${ASLOGIN} --password ${ASPASSWORD} --host ${ASURI} ${AQUA_OPTS} ${vars.imageName} > /dev/null"

        vars.buildCmdParameters += " && docker run --rm --volume ${pwd()}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ubuntu chown -R \$(id -u):\$(id -g) ."

        if (vars.buildCmdParameters?.trim()) {
            vars.buildCmd += " ${vars.buildCmdParameters}"
        }

        // TODO Remove it when tee will be back
        vars.buildCmd += " 2>&1 > ${vars.shellOutputFile} "
        //vars.buildCmd +=        " > /dev/null"

        // Run the aqua build
        build = sh (
                script: "${vars.buildCmd}",
                returnStatus: true
                )
        echo "AQUA RETURN CODE : ${build}"
        if (build == 0) {
            echo "AQUA SUCCESS"
        } else {
            if (!vars.skipFailure) {
                echo "AQUA UNSTABLE"
                currentBuild.result = 'UNSTABLE'
            } else {
                echo "AQUA FAILURE skipped"
                //error 'There are errors in aqua'
            }
        }
        if (body) { body() }

      } catch (exc) {
        currentBuild.result = 'FAILURE'
        error "There was a problem with aqua scan image \'${vars.imageName}\' " + exc.toString()
      }

    //}  // withRegistry

    if (fileExists("${pwd()}/${AQUA_REPORT}")) {
      publishHTML([
        allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: '',
        reportFiles : "${AQUA_REPORT}", reportName: "Aqua_${vars.reportName}", reportTitles: "${vars.imageName}"
      ])
    } else {
      echo "Aqua scan output file ${AQUA_REPORT} was not generated"
    } // if

    //} // tee
  } finally {
    archiveArtifacts artifacts: "*.log, aqua.html", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
  }

}
