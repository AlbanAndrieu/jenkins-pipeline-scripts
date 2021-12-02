#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withTriggerJob.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  //vars.remoteJenkinsName = vars.get("remoteJenkinsName", "bm-ci-pl").trim()
  vars.job = vars.get('job', 'bower-fr-integration-test/develop').trim()
  vars.blockBuildUntilComplete = vars.get('blockBuildUntilComplete', true).toBoolean()
  vars.shouldNotFailBuild = vars.get('shouldNotFailBuild', false).toBoolean()

  vars.withTriggerJob = vars.get('withTriggerJob', false).toBoolean()
  vars.skipTriggerRemoteJobFailure = vars.get('skipTriggerRemoteJobFailure', true).toBoolean()
  vars.triggerJobFileId = vars.get('triggerJobFileId', vars.draftPack ?: '0').trim()
  vars.triggerJobOutputFile = vars.get('triggerJobOutputFile', "triggerJob-${vars.triggerJobFileId}.log").trim()

  if (!vars.withTriggerJob) {
    try {
      //tee("${vars.triggerJobOutputFile}") {

      // See https://www.jenkins.io/doc/pipeline/steps/pipeline-build-step/
      //Trigger job
      catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
          def handle = build job: vars.job, propagate: !vars.shouldNotFailBuild, quietPeriod: 1, wait: vars.blockBuildUntilComplete, parameters: [string(name: 'HELM_TAG', value: String.valueOf(env.HELM_TAG))]
        } // catchError

      echo "Init result: ${currentBuild.result}"
      echo "Init currentResult: ${currentBuild.currentResult}"

        // See https://issues.jenkins.io/browse/JENKINS-53923
        //def triggerJobResult = handle.getResult()
        //echo "Build of ${vars.job} returned result: ${triggerJobResult}"
        //
        ////List other available methods
        ////echo handle.help()
        //
        //echo "BUILD RETURN CODE : ${triggerJobResult}"
        ////if (build.getResult() == Result.SUCCESS) {
        //if (triggerJobResult == 0) {
        //  echo "TRIGGER SUCCESS"
        //
        //  //Get information from the handle
        //  //def j1EnvVariables = handle.getBuildVariables();
        //
        //} else {
        //  echo "WARNING : TriggerJob failed, check output at \'${env.BUILD_URL}/artifact/${vars.triggerJobOutputFile}\' "
        //  if (!vars.skipTriggerRemoteJobFailure) {
        //    echo "TRIGGER UNSTABLE"
        //    currentBuild.result = 'UNSTABLE'
        //  } else {
        //    echo "TRIGGER FAILURE skipped"
        //    //error 'There are errors in triggerJob' // not needed
        //  }
        //}

      if (body) {
        body()
      }

    //} // tee
    } catch (exc) {
      echo 'TRIGGER FAILURE'
      currentBuild.result = 'FAILURE'
      //build = "FAIL" // make sure other exceptions are recorded as failure too
      echo 'WARNING : There was a problem with TriggerJob ' + exc
    } finally {
      archiveArtifacts artifacts: "${vars.triggerJobOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
      }
  } else {
    echo 'TriggerJob skipped'
    }
  }
