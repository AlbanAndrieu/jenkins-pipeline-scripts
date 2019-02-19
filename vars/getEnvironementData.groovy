#!/usr/bin/groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    if (!body) {
        echo 'No body specified'
    }

    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def filePath = vars.get("filePath", "step-2-0-0-build-env.sh")

    if (DEBUG_RUN) {
        sh "set -xv && ${filePath}"
    } else {
        sh "${filePath}"
    }

    load "./jenkins-env.groovy"
}
