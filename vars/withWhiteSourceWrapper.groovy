#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/withWhiteSourceWrapper.groovy`"

  vars = vars ?: [:]

  def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
  def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)

  String WHITESOURCE_TOKEN = vars.get("WHITESOURCE_TOKEN", "TODO").trim()

  vars.productVersion = vars.get("productVersion", "").trim()
  vars.product = vars.get("product", "FusionRisk").trim()
  vars.projectToken = vars.get("projectToken", "").trim()

  if (vars.projectToken?.trim()) {
      vars.requesterEmail = vars.get("requesterEmail", 'alban.andrieu@free.fr').trim()

      vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
      vars.whithSourceOutputFile = vars.get("whithSourceOutputFile", "whithsource.log").trim()
      //vars.skipFailure = vars.get("skipFailure", false).toBoolean()

      try {
        //tee("${vars.whithSourceOutputFile}") {

        if (!RELEASE_VERSION) {
            echo 'No RELEASE_VERSION specified'
            RELEASE_VERSION = getSemVerReleasedVersion(vars) ?: "LATEST"
            vars.productVersion = "${RELEASE_VERSION}"
        }

        whitesource jobApiToken: WHITESOURCE_TOKEN, jobCheckPolicies: 'global', jobForceUpdate: 'global', jobUserKey: '', libExcludes: '', libIncludes: '', product: vars.product, productVersion: vars.projectToken, projectToken: vars.projectToken, requesterEmail: vars.requesterEmail

        if (body) { body() }

        //} // tee
      } catch (exc) {
        //currentBuild.result = 'UNSTABLE'
        echo "WARNING : There was a problem with aqua scan" + exc.toString()
      } finally {
        archiveArtifacts artifacts: "${vars.whithSourceOutputFile}, aqua.html", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
      }

  } else {
      echo "WARNING : There was a problem with whitesrouce scan, projectToken cannot be empty"
  }

}
