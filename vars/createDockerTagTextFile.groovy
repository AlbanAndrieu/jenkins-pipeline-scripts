#!/usr/bin/groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo '[JPL] Executing `vars/createDockerTagTextFile.groovy`'

  vars = vars ?: [:]

  vars.pomFile = vars.get('pomFile', 'pom.xml').trim()
  vars.releaseVersion = getReleasedVersion(vars) ?: releaseVersion.trim()
  vars.dockerTag = vars.get('dockerTag', env.DOCKER_TAG ?: 'temp').trim()
  vars.dockerImage = vars.get('dockerImage',  env.DOCKER_IMAGE ?: 'TEST').trim()
  vars.dockerImageName = vars.get('dockerImageName',  vars.dockerTag + ':' + vars.dockerImage).trim()

  vars.skipDumpTag = vars.get('skipDumpTag', false).toBoolean()

  if (!vars.skipDumpTag) {
    call(vars.dockerImageName, vars.pomFile, vars.releaseVersion)
    if (body) { body() }
  } // skipDumpTag
}

/**
 * <h1>Create an VERSION_DOCKER.txt file that will contain information about repository and generated artifacts, images, charts.</h1>
 * <p>
 * Use instead @see createManifest
 * This function is aimed to be used to store information about the build.
 * How and what was produced.
 * </p>
 *
 * <b>Note:</b> Those files are used by the installer and used by the release process to gather all poducts informations.
 * Thoses information will be used to generate the release email.
 *
 * @param projectName The name of the project. If not set, the name of the repo will be used. Please as much as possible leave it empty, so that it will be generated.
 */
@Deprecated
def call(def dockerImageName, def pomFile = 'pom.xml', def releaseVersion = '') {
  echo '[JPL] Executing `vars/createDockerTagTextFile.groovy`'

  this.vars = [:]

  try {
    vars.pomFile = pomFile.trim()
    vars.isRegistryFormat = vars.get('isRegistryFormat', true).toBoolean()
    vars.isCsv = vars.get('isCsv', true).toBoolean()
    vars.releaseVersion = getReleasedVersion(vars) ?: releaseVersion.trim()
    vars.projectName = vars.get('projectName', getGitRepoName(vars).toUpperCase() ?: 'TEST').trim()
    vars.fileName = vars.get('fileName', "${vars.projectName}_DOCKER_VERSION").trim()
    if (vars.isRegistryFormat.toBoolean()) {
      vars.fileName += '.TXT'
    } else if (vars.isCsv.toBoolean()) {
      vars.fileName += '.CSV'
    } else {
      vars.fileName += '.TXT'
    }
    // See https://risk-jenkins.misys.global.ad/jenkins/pipeline-syntax/globals#env
    vars.jobUrl = vars.get('JOB_URL', env.JOB_URL ?: 'TODO').trim()

    //vars.dockerTag = vars.get("dockerTag", dockerTag ?: (env.DOCKER_TAG ?: "temp")).trim()
    //vars.dockerImage = vars.get("dockerImage", dockerImage ?: (env.DOCKER_IMAGE ?: "TEST")).trim()
    vars.dockerImageName = vars.get('dockerImageName', dockerImageName ?: 'TODO').trim()
    vars.fileContents = ''

    echo "Dumping : ${vars.dockerImageName} to ${vars.fileName}"

    if ( BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
      if (vars.isRegistryFormat.toBoolean()) {
        if (vars.dockerImageName?.trim()) {
          vars.fileContents += "${vars.dockerImageName}"
        }
      } else if (vars.isCsv.toBoolean()) {
        if (vars.dockerImageName?.trim()) {
          vars.fileContents += "${vars.dockerImageName};"
        }
        if (vars.jobUrl?.trim()) {
          vars.fileContents += "${vars.jobUrl};"
        }

      //vars.fileContents += "${vars.projectName};${vars.releaseVersion};"
      } else {
        vars.fileContents = "PROJECT_NAME:${vars.projectName} RELEASE_VERSION:${vars.releaseVersion}"

        if (vars.dockerImageName?.trim()) {
          vars.fileContents += " DOCKER_IMAGE_NAME:${vars.dockerImageName}"
        }
        if (vars.jobUrl?.trim()) {
          vars.fileContents += " JOB_NAME:${vars.jobUrl}"
        }
            } // format

      sh """#!/bin/bash -l
            echo ${vars.fileContents} >> "./${vars.fileName}"
      """

      echo "${vars.fileName} created"
    } else {
      echo 'Do no dump for PR or feature branches'
    } // BRANCH_NAME
  } catch (exception) {
    echo 'Warn: There was a problem Dumping docker image.'
    echo "Exception: ${exception}"
    echo "Issue: ${exception.getMessage()}"
    //echo "Cause: ${exception.getCause()}"
    if (isDebugRun(vars)) {
      echo "Trace: ${exception.getStackTrace()}"
    }
  }

  archiveArtifacts artifacts: "${vars.fileName}", onlyIfSuccessful: false, allowEmptyArchive: true

  return vars.fileContents
}
