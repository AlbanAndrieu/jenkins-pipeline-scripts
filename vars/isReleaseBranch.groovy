#!/usr/bin/groovy
import hudson.model.*

def call() {
  this.vars = [:]
  call(vars)
}

def call(Map vars) {
  echo '[JPL] Executing `vars/isReleaseBranch.groovy`'

  vars = vars ?: [:]

  def DEBUG_RUN = vars.get('DEBUG_RUN', env.DEBUG_RUN ?: false).toBoolean()

  try {
    if ( env.BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
      if (DEBUG_RUN) {
        echo 'Release branch detected'
      }
      return true
      } else {
      return false
    }
  }
    catch (exc) {
    echo 'Warning: There were errors in isReleaseBranch. ' + exc
    }

  return false
}
