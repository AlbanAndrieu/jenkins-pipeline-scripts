#!/usr/bin/groovy
import java.*
import hudson.*
import hudson.model.*
import jenkins.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withBuildWrapper.groovy`'

  vars = vars ?: [:]

  if (!body) {
    echo 'No body specified'
  }

  vars.artifacts = vars.get('artifacts', ['*_VERSION.TXT',
                   '**/MD5SUMS.md5',
                   'Output/**/*.tar.gz'
                   ].join(', '))

  vars.isStashMavenEnabled = true

  vars.CLEAN_RUN = vars.get('CLEAN_RUN', env.CLEAN_RUN ?: false).toBoolean()
  vars.DRY_RUN = vars.get('DRY_RUN', env.DRY_RUN ?: false).toBoolean()
  vars.DEBUG_RUN = vars.get('DEBUG_RUN', env.DEBUG_RUN ?: false).toBoolean()
  vars.SCONS_OPTS = vars.get('SCONS_OPTS', env.SCONS_OPTS ?: '').trim()
  vars.filePath = vars.get('filePath', './bm/env/scripts/jenkins/step-2-0-0-build-env.sh').trim()
  vars.pomFile         = vars.get('pomFile', 'Almonde/pom.xml').trim()
  RELEASE_VERSION      = getReleasedVersion(vars)
  env.RELEASE_VERSION  = RELEASE_VERSION

  wrapInTEST(vars) {
        withBuildCppWrapper(vars) {
      if (body) { body() }

      if (vars.DEBUG_RUN) {
        getEnvironementData(vars)
      }
        } // withBuildCppWrapper
    } // wrapInTEST
}
