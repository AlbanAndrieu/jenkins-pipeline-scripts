#!/usr/bin/groovy

import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/testPreCommit.groovy"

  vars = vars ?: [:]

  vars.testPreCommitCmd = vars.get("testPreCommitCmd", "pre-commit run --all --verbose").trim()
  vars.testPreCommitFileId = vars.get("testPreCommitFileId", "0").trim()

  vars.skipTestPreCommitFailure = vars.get("skipTestPreCommitFailure", true).toBoolean()
  vars.skipTestPreCommit = vars.get("skipTestPreCommit", false).toBoolean()
  vars.testPreCommitOutputFile = vars.get("testPreCommitOutputFile", "molecule-${vars.testPreCommitFileId}.log").trim()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
      vars.skipTestPreCommit = true
  }

  if (!vars.skipTestPreCommit) {

    try {

      //tee("${vars.testPreCommitOutputFile}") {

      if (body) { body() }

      sh """#!/bin/bash -l
      ls -lrta /opt/ansible/
      . /opt/ansible/env38/bin/activate
      which python3
      python3 -V
      #source ./scripts/run-python.sh
      pip -V
      pip install pre-commit==2.9.0
      which pre-commit
      pre-commit run git-branches-check --verbose 2>&1 > pre-commit-branches-check.log || true
      #pre-commit run --all --verbose 2>&1 > pre-commit.log || true
      """

      // TODO Remove it when tee will be back
      vars.testPreCommitCmd += " 2>&1 > ${vars.testPreCommitOutputFile} "

      helm = sh (script: vars.testPreCommitCmd, returnStatus: true)
      echo "PRE-COMMIT RETURN CODE : ${helm}"
      if (helm == 0) {
        echo "PRE-COMMIT SUCCESS"
      } else {
        echo "WARNING : Molecule failed, check output at \'${env.BUILD_URL}/artifact/${vars.testPreCommitOutputFile}\' "
        if (!vars.skipTestPreCommitFailure) {
          echo "PRE-COMMIT FAILURE"
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in pre-commit'
        } else {
          echo "PRE-COMMIT FAILURE skipped"
          //error 'There are errors in helm'
        }
      }

      //} // tee

    } catch (exc) {
      echo "Warn: There was a problem testing pre-commit " + exc.toString()
    } finally {
      archiveArtifacts artifacts: "${vars.testPreCommitOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
      echo "Check : ${env.BUILD_URL}/artifact/${vars.testPreCommitOutputFile}"
    }
  } else {
    echo "Test pre-commit skipped"
  }
}
