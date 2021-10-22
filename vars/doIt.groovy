#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/doIt.groovy`"

  vars = vars ?: [:]

  vars.skipDoIt = vars.get("skipDoIt", false).toBoolean()

  //vars.doItFileId = vars.get("doItFileId", env.BUILD_NUMBER ?: "0").trim()
  //vars.doItOutputFile = vars.get("doItOutputFile", "doit-${vars.doItFileId}.log").trim()

  if (!vars.skipDoIt) {
    //try {

    if (body) { body() }

    testPreCommit()

    withMavenWrapper(skipSonar: !params.BUILD_SONAR.toBoolean())

    withMavenSiteWrapper(shellOutputFile: "maven-site.log",
      skipSonarCheck: true)

    withSonarQubeWrapper(verbose: true,
      skipMaven: false,
      skipFailure: false,
      skipSonarCheck: true,
      reportTaskFile: ".scannerwork/report-task.txt",
      isScannerHome: false,
      sonarExecutable: "/usr/local/sonar-runner/bin/sonar-scanner")

    withSonarQubeCheck(skipFailure: !params.BUILD_SONAR.toBoolean())

    withChangelog()

    //} catch (exc) {
    //  echo "Warn: There was a problem with building : " + exc.toString()
    //} finally {
    //  cleanEmptyFile(vars)
    //  archiveArtifacts artifacts: "${vars.helmListAllOutputFile}, ${vars.helmListBuildedOutputFile}, ${vars.helmListFailedOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    //}

  } else {
    echo "DoIt skipped"
  }
}
