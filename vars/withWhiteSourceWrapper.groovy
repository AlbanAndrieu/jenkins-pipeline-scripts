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

  String WHITESOURCE_TOKEN = vars.get("WHITESOURCE_TOKEN", "27651afa-9e00-4c57-9f0e-7df9358e6750").trim()
  String WHITESOURCE_URL="https://saas.whitesourcesoftware.com/".trim()

  vars.projectName = vars.get("projectName", "test").trim().replaceAll(' ','-')
  vars.projectVersion = vars.get("projectVersion", "").trim()
  vars.productVersion = vars.get("productVersion", "").trim()
  vars.product = vars.get("product", "nabla").trim().replaceAll(' ','-')
  vars.libIncludes = vars.get("libIncludes", "").trim()
  vars.libExcludes = vars.get("libExcludes", "").trim()
  vars.projectToken = vars.get("projectToken", "").trim()
  //productToken 2dcf60630aca4d3fb29fe59ad731d488747f2f6b0ba04b8b8664ae3629a1c3ae
  vars.jobUserKey = vars.get("jobUserKey", "cf5b762ee7ab4f2cb9fdab9728e59c2a3ccc2d77cb9b4718986dfe90fac671bb").trim()
  vars.skipFailure = vars.get("skipFailure", true).toBoolean()

  if (vars.projectName?.trim()) {
      vars.productVersion += vars.projectName
      vars.requesterEmail = vars.get("requesterEmail", 'alban.andrieu@free.fr').trim()

      vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
      vars.whithSourceOutputFile = vars.get("whithSourceOutputFile", "whithsource.log").trim()

      try {
        tee("${vars.whithSourceOutputFile}") {

          if (!RELEASE_VERSION) {
              echo 'No RELEASE_VERSION specified'
              RELEASE_VERSION = getSemVerReleasedVersion(vars) ?: "LATEST"
              if (!vars.projectVersion?.trim()) {
                  vars.projectVersion = "${RELEASE_VERSION}"
                  //vars.productVersion += vars.projectVersion
              }
          }

          whitesource jobApiToken: WHITESOURCE_TOKEN, jobCheckPolicies: 'global', jobForceUpdate: 'global', jobUserKey: vars.jobUserKey, libExcludes: vars.libExcludes, libIncludes: vars.libIncludes, product: vars.product, productVersion: vars.productVersion, projectToken: vars.projectToken, requesterEmail: vars.requesterEmail

          if (body) { body() }

        } // tee
      } catch (exc) {
        if (!vars.skipFailure) {
            echo "WHITESOURCE UNSTABLE"
            currentBuild.result = 'UNSTABLE'
        } else {
            echo "WHITESOURCE FAILURE skipped"
            //error 'There are errors in whitesource'
        }
        echo "WARNING : Scan failed, check output at \'${vars.whithSourceOutputFile}\' "
        echo "WARNING : There was a problem with white source scan : " + exc.toString()
        echo "Check on : ${WHITESOURCE_URL}"
      } finally {
        archiveArtifacts artifacts: "${vars.whithSourceOutputFile}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
      }

  } else {
      echo "WARNING : There was a problem with whitesource scan, projectName cannot be empty"
  }

}
