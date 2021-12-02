#!/usr/bin/groovy

// Use  instead createManifest.groovy
@Deprecated
def call(def description = '', def fileName = '', def pomFile = 'pom.xml', def releaseVersion = '') {
  echo '[JPL] Executing `vars/createVersionTextFile.groovy`'

  this.vars = [:]

  vars.pomFile = pomFile.trim()
  vars.releaseVersion = getReleasedVersion(vars) ?: releaseVersion.trim()
  vars.projectName = vars.get('projectName', getGitRepoName(vars).toUpperCase() ?: 'TEST').trim()
  vars.fileName = vars.get('fileName', fileName ?: "${vars.projectName}_VERSION.TXT").trim()
  vars.description = vars.get('description', description ?: "${vars.projectName}").trim()

  vars.build = currentBuild.number.toString()
  vars.commitSHA1 = getCommitId()

  vars.commitRevision = getCommitRevision()

  vars.fileContents = "${vars.description}:${vars.releaseVersion} BUILD:${vars.build}"

  if (vars.commitSHA1?.trim()) {
    vars.fileContents += " SHA1:${vars.commitSHA1}"
  }

  if (vars.commitRevision?.trim()) {
    vars.fileContents += " REV:${vars.commitRevision}"
  }

  sh """#!/bin/bash -l
        echo ${vars.fileContents} > "./${vars.fileName}"
    """

  //TODO createManifest(vars)
  archiveArtifacts artifacts: "${vars.fileName}", onlyIfSuccessful: false, allowEmptyArchive: true

  return vars.fileContents
}
