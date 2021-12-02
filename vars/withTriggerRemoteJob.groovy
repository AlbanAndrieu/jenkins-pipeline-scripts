#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withTriggerRemoteJob.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.remoteJenkinsName = vars.get('remoteJenkinsName', 'bm-ci-pl').trim()
  vars.job = vars.get('job', 'Tests/aandrieu/RKTMP/develop').trim()
  vars.blockBuildUntilComplete = vars.get('blockBuildUntilComplete', true).toBoolean()
  vars.shouldNotFailBuild = vars.get('shouldNotFailBuild', false).toBoolean()

  vars.skipTriggerRemoteJob = vars.get('skipTriggerRemoteJob', false).toBoolean()
  vars.skipTriggerRemoteJobFailure = vars.get('skipTriggerRemoteJobFailure', true).toBoolean()
  vars.triggerRemoteJobFileId = vars.get('triggerRemoteJobFileId', vars.draftPack ?: '0').trim()
  vars.triggerRemoteJobOutputFile = vars.get('triggerRemoteJobOutputFile', "triggerRemoteJob-${vars.triggerRemoteJobFileId}.log").trim()

  if (!vars.skipTriggerRemoteJob && isReleaseBranch()) {
    try {
      //tee("${vars.triggerRemoteJobOutputFile}") {

      // Trigger remote job : https://www.jenkins.io/doc/pipeline/steps/Parameterized-Remote-Trigger/
      def handle = triggerRemoteJob(remoteJenkinsName: vars.remoteJenkinsName,
          //remoteJenkinsUrl: "",
          job: vars.job,
          blockBuildUntilComplete: vars.blockBuildUntilComplete,
          shouldNotFailBuild: vars.shouldNotFailBuild,
          preventRemoteBuildQueue: true,
          //abortTriggeredJob: true,
          enhancedLogging: true)

      // Get information from the handle
      def triggerRemoteJobResult = handle.getBuildStatus()
      def buildUrl = handle.getBuildUrl()
      echo buildUrl.toString() + ' finished with ' + triggerRemoteJobResult.toString()

      // List other available methods
      echo handle.help()

      echo "BUILD RETURN CODE : ${triggerRemoteJobResult}"
      if (triggerRemoteJobResult == 0) {
        echo 'TRIGGER REMOTE SUCCESS'

        if (vars.blockBuildUntilComplete) {
          // Download and parse the archived "build-results.json" (if generated and archived by remote build)
          def results = handle.readJsonFileFromBuildArchive('build-results.json')
          echo results.urlToTestResults //only example
          } // blockBuildUntilComplete
        } else {
        echo "WARNING : TriggerRemoteJob failed, check output at \'${env.BUILD_URL}/artifact/${vars.triggerRemoteJobOutputFile}\' "
        if (!vars.skipTriggerRemoteJobFailure) {
          echo 'TRIGGER REMOTE UNSTABLE'
          currentBuild.result = 'UNSTABLE'
          } else {
          echo 'TRIGGER REMOTE FAILURE skipped'
        // error 'There are errors in triggerRemoteJob' // not needed
        }
      }

      if (body) {
        body()
      }

    //} // tee
    } catch (exc) {
      echo 'TRIGGER REMOTE FAILURE'
      currentBuild.result = 'FAILURE'
      //build = "FAIL" // make sure other exceptions are recorded as failure too
      echo 'WARNING : There was a problem with TriggerRemoteJob ' + exc
    } finally {
      archiveArtifacts artifacts: "${vars.triggerRemoteJobOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'TriggerRemoteJob skipped'
  }
}
