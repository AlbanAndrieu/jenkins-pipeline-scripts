#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withMavenDependencyCheckWrapper.groovy`'

  vars = vars ?: [:]

  //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get('DRY_RUN', env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

  vars.goal = vars.get('goal', 'org.owasp:dependency-check-maven:check')
  vars.profile = vars.get('profile', 'sonar').trim()
  vars.skipTests = vars.get('skipTests', true).toBoolean()
  vars.skipResults = vars.get('skipResults', true).toBoolean()
  //vars.buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
  vars.skipSonar = vars.get('skipSonar', true).toBoolean()
  vars.skipDependencyCheck = vars.get('skipDependencyCheck', true).toBoolean()
  vars.skipPitest = vars.get('skipPitest', true).toBoolean()
  vars.skipSonarCheck = vars.get('skipSonarCheck', true).toBoolean()
  vars.skipDocker = vars.get('skipDocker', true).toBoolean()
  vars.skipArtifacts = vars.get('skipArtifacts', true).toBoolean()
  vars.buildCmdParameters = vars.get('buildCmdParameters', '').trim()
  vars.artifacts = vars.get('artifacts', ['*_VERSION.TXT', '**/target/*.jar'].join(', '))
  vars.shellOutputFile = vars.get('shellOutputFile', 'maven-depcheck.log').trim()

  if (!DRY_RUN && !vars.skipDependencyCheck) {
    vars.buildCmdParameters += ' -Dskip.npm -Dskip.yarn -Dskip.bower -Dskip.grunt -Dmaven.exec.skip=true -Denforcer.skip=true -Dmaven.test.skip=true '

    withMavenWrapper(vars) {
      stash allowEmpty: true, includes: '**/dependency-check-report.xml', name: 'depcheck'

      if (body) { body() }
    }

    try {
      dependencyCheckPublisher canComputeNew: false, defaultEncoding: '', healthy: '50', pattern: '**/dependency-check-report.xml', shouldDetectModules: true, thresholdLimit: 'normal', unHealthy: '100'
        } catch (exc) {
      echo 'Warn: There was a problem with dependencyCheckPublisher ' + exc
    }
    } // if DRY_RUN
}
