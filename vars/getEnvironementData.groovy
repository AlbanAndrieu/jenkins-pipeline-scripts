#!/usr/bin/groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getEnvironementData.groovy`"

  vars = vars ?: [:]

  if (!body) {
      echo 'No body specified'
  }

  vars.DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
  vars.filePath = vars.get("filePath", "step-2-0-0-build-env.sh").trim()

  if (vars.DEBUG_RUN) {
      sh "set -xv && ${vars.filePath}"
  } else {
      sh "${vars.filePath}"
  }

  load "./jenkins-env.groovy"
}
