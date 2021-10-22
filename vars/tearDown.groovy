#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/tearDown.groovy`"

  vars = vars ?: [:]

  vars.skipTearDown = vars.get("skipTearDown", false).toBoolean()

  //vars.buildDir = vars.get("buildDir", "${pwd()}/").trim()
  vars.isEmailEnabled = vars.get("isEmailEnabled", false).toBoolean()
  vars.skipLogParser = vars.get("skipLogParser", true).toBoolean()

  //vars.tearDownFileId = vars.get("tearDownFileId", env.BUILD_NUMBER ?: "0").trim()
  //vars.tearDownOutputFile = vars.get("tearDownOutputFile", "cleanit-${vars.tearDownFileId}.log").trim()

  if (!vars.skipTearDown) {
    try {

      if (body) { body() }

      archiveArtifacts artifacts: ['**/target/*.jar', '*_VERSION.TXT', '*.log'].join(', '), excludes: null, fingerprint: true, onlyIfSuccessful: false

      node('molecule||docker-compose||docker32G||docker16G') {
        if (vars.skipLogParser.toBoolean()) {
          // LEGACY function
          runHtmlPublishers(["LogParserPublisher", "AnalysisPublisher"])
        } else {
          // TODO : install logParser plugin first
          withLogParser(failBuildOnError:false, unstableOnWarning: false)
        } // skipLogParser
      } // node
      if (isCleanRun(vars)) {
        //wrapCleanWs(vars)
        wrapCleanWsOnNode(vars)
      }

    } catch (exc) {
      echo "Warn: There was a problem with teardown : " + exc.toString()
    //} finally {
    //  cleanEmptyFile(vars)
    //  archiveArtifacts artifacts: "${vars.helmListAllOutputFile}, ${vars.helmListBuildedOutputFile}, ${vars.helmListFailedOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }

  } else {
    echo "TearDown skipped"
  }
}
