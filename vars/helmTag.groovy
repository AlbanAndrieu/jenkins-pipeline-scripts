#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmTag.groovy`'

  vars = vars ?: [:]

  vars.helmTag = vars.get('helmTag', env.HELM_TAG ?: '0.0.1').trim()
  vars.commit = vars.get('commit', env.GIT_COMMIT ?: '').trim() // getCommitId.groovy
  vars.isBuildNumber = vars.get('isBuildNumber', false).toBoolean()
  if (!vars.isBuildNumber) {
    vars.buildId = vars.get('buildId', env.CUSTOM_BUILD_ID ?: '0').trim()
  } else {
    vars.buildId = vars.get('buildId', env.BUILD_ID ?: env.BUILD_NUMBER).trim()  // FYI : BUILD_ID and BUILD_NUMBER are the same
  }
  vars.pomFile = vars.get('pomFile', './pom.xml').trim()

  vars.date = vars.get('date', getTimestamp())

  RELEASE_VERSION = getSemVerReleasedVersion(vars) ?: "${vars.helmTag}"

  try {
    //println(env.HELM_TAG)
    if (!env.HELM_TAG?.trim()) {
      env.HELM_TAG = RELEASE_VERSION + '-' + vars.date

      if (vars.commit != null && vars.commit.trim() != '' ) {
        def commitShortSHA1 = vars.commit.take(7)
        env.HELM_TAG += ".sha${commitShortSHA1}"
      }

      env.HELM_TAG += '.' + vars.buildId

      echo "NEW HELM_TAG : ${env.HELM_TAG}"
    }
    environment()
  } catch (exc) {
    echo 'Error: There were errors in helmTag. ' + exc
    env.HELM_TAG = RELEASE_VERSION
  }
  echo "HELM_TAG : ${env.HELM_TAG}"

  if (body) { body() }

  // See https://helm.sh/docs/chart_best_practices/conventions/
  // TODO .replaceAll('\\+','_')
  return env.HELM_TAG.toLowerCase()
}
