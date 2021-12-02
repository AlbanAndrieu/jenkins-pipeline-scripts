#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/getMavenGoalsProfile.groovy`'

  vars = vars ?: [:]

  //def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
  def RELEASE = vars.get('RELEASE', env.RELEASE ?: false).toBoolean()

  vars.skipProfile = vars.get('skipProfile', false).toBoolean()
  vars.skipObfuscation = vars.get('skipObfuscation', true).toBoolean()
  vars.skipSigning = vars.get('skipSigning', false).toBoolean()
  vars.skipIntegration = vars.get('skipIntegration', true).toBoolean()
  vars.skipDocker = vars.get('skipDocker', true).toBoolean()
  vars.profile = vars.get('profile', 'sonar')
  vars.profileGoals = vars.get('profileGoals', '')

  if (!vars.skipProfile) {
    if (RELEASE) {
      echo 'release profile added'
      vars.profile += ',release'
    }
    if (!vars.skipObfuscation && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /release.*/) || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/)) ) {
      echo 'zkm obfuscation profile added'
      vars.profile += ',obfuscation'
    }
    if (!vars.skipSigning && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /release.*/)) ) {
      echo 'signing profile added'
      vars.profile += ',signing'
    }
    if (!vars.skipIntegration && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /release.*/) || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/)) ) {
      echo 'integration test added'
      vars.profile += ',run-integration-test'
    }
    if (!vars.skipDocker) {
      echo 'docker profile added'
      vars.profile += ',docker'
    }
    if (vars.profile?.trim()) {
      vars.profileGoals = " -P${vars.profile}"
    }
    } // if skipProfile

  if (body) { body() }

  return vars.profileGoals
}
