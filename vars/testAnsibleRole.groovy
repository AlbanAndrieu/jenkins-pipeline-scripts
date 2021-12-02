#!/usr/bin/groovy

import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/testAnsibleRole.groovy'

  vars = vars ?: [:]

  vars.roleName = vars.get('roleName', 'todo').trim()
  vars.testAnsibleCmd = vars.get('testAnsibleCmd', './scripts/test-with-ara.sh' + vars.roleName).trim()
  vars.testAnsibleFileId = vars.get('testAnsibleFileId', vars.roleName ?: '0').trim()

  vars.skipTestAnsibleFailure = vars.get('skipTestAnsibleFailure', false).toBoolean()
  vars.skipTestAnsible = vars.get('skipTestAnsible', false).toBoolean()
  vars.testAnsibleOutputFile = vars.get('testAnsibleOutputFile', "molecule-${vars.testAnsibleFileId}.log").trim()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipTestAnsible = true
  }

  if (!vars.skipTestAnsible) {
    try {
      //tee("${vars.testAnsibleOutputFile}") {

      if (body) { body() }

      if (vars.roleName != null && vars.roleName.trim() != '' ) {
        writeFile(file: "${pwd()}/test-with-ara.sh", text: libraryResource('test-with-ara.sh'))
        sh("ls -lrta ${pwd()}/")
        sh("chmod a+x ${pwd()}/test-with-ara.sh")

        // TODO Remove it when tee will be back
        vars.testAnsibleCmd += " 2>&1 > ${vars.testAnsibleOutputFile} "

        helm = sh (script: vars.testAnsibleCmd, returnStatus: true)
        echo "MOLECULE RETURN CODE : ${helm}"
        if (helm == 0) {
          echo 'MOLECULE SUCCESS'

          // TOREDO ClaimTestDataPublisher
          //junit testResults: "**/ara-" + vars.roleName + ".xml", healthScaleFactor: 2.0, allowEmptyResults: true, keepLongStdio: true, testDataPublishers: [[$class: 'ClaimTestDataPublisher']]
          junit testResults: '**/ara-' + vars.roleName + '.xml', healthScaleFactor: 2.0, allowEmptyResults: true, keepLongStdio: true

        //publishHTML([
        //  allowMissing: true,
        //  alwaysLinkToLastBuild: false,
        //  keepAll: true,
        //  reportDir: "./ara-" + vars.roleName + "/",
        //  reportFiles: 'index.html',
        //  includes: '**/*',
        //  reportName: "ARA " + vars.roleName + " Report",
        //  reportTitles: "ARA " + vars.roleName + " Report Index"
        //])
        } else {
          echo "WARNING : Molecule failed, check output at \'${env.BUILD_URL}artifact/${vars.testAnsibleOutputFile}\' "
          if (!vars.skipTestAnsibleFailure) {
            echo 'MOLECULE FAILURE'
            //currentBuild.result = 'UNSTABLE'
            currentBuild.result = 'FAILURE'
            error 'There are errors in molecule'
          } else {
            echo 'MOLECULE FAILURE skipped'
          //error 'There are errors in helm'
          }
        }

      //} // tee
      } // if
    } catch (exc) {
      echo 'Warn: There was a problem testing ansible ' + exc
    } finally {
      archiveArtifacts artifacts: "${vars.testAnsibleOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
      echo "Check : ${env.BUILD_URL}artifact/${vars.testAnsibleOutputFile}"
    }
  } else {
    echo 'Test ansible skipped'
  }
}
