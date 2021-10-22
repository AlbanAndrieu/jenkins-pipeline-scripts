#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/setUp.groovy`"

  vars = vars ?: [:]

  vars.skipSetUp = vars.get("skipSetUp", false).toBoolean()

  RELEASE_VERSION = getSemVerReleasedVersion() ?: "LATEST"

  echo "RELEASE_VERSION: ${RELEASE_VERSION} - ${env.RELEASE_VERSION}"

  vars.setUpFileId = vars.get("setUpFileId", env.BUILD_NUMBER ?: "0").trim()
  vars.description = vars.get("description", "Test project ${RELEASE_VERSION}").trim()
  vars.filename = vars.get("filename", "TEST_VERSION.TXT").trim()

  //vars.buildDir = vars.get("buildDir", "${pwd()}/").trim())

  //vars.setUpFileId = vars.get("setUpFileId", env.BUILD_NUMBER ?: "0").trim()
  //vars.setUpOutputFile = vars.get("setUpOutputFile", "cleanit-${vars.setUpFileId}.log").trim()

  if (!vars.skipSetUp) {
    try {

      if (body) { body() }

      if (isBuildNightly(vars)) {
        properties(createPropertyList(cronString: env.BRANCH_NAME == 'develop' ? 'H H(0-7) * * 1-5' : ''))

      } else {
        properties(createPropertyList(vars))
      } // BUILD_NIGHLTY

      //createVersionTextFile("Sample", "TEST_VERSION.TXT")
      createManifest(vars)

      setBuildName(vars)

      cleanIt(vars)

    } catch (exc) {
      echo "Warn: There was a problem with setup : " + exc.toString()
    //} finally {
    //  cleanEmptyFile(vars)
    //  archiveArtifacts artifacts: "${vars.helmListAllOutputFile}, ${vars.helmListBuildedOutputFile}, ${vars.helmListFailedOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }

  } else {
    echo "SetUp skipped"
  }
}
