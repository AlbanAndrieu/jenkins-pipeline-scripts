#!/usr/bin/env groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withArchive.groovy`'

  vars = vars ?: [:]

  //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
  def DRY_RUN = vars.get('DRY_RUN', env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

  vars.skipMaven = vars.get('skipMaven', true).toBoolean()
  vars.isFingerprintEnabled = vars.get('isFingerprintEnabled', true).toBoolean()
  vars.isOnlySuccessful = vars.get('isOnlySuccessful', true).toBoolean()

  if (!DRY_RUN) {
    if (!vars.skipMaven) {
      unstash 'maven-artifacts'
    }

    if (body) { body() }

    def artifacts = vars.get('artifacts', ['*_VERSION.TXT', '**/target/*.jar'].join(', '))

    echo "artifacts : ${artifacts}"

    archiveArtifacts artifacts: "${artifacts}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: vars.isOnlySuccessful
    } // if
}
