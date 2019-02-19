#!/usr/bin/groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    script {

        def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

        if (DEBUG_RUN) {
            sh 'env'

            echo "1 - RELEASE=${env.RELEASE}"
            echo env.RELEASE

            echo "1 - RELEASE_BASE=${env.RELEASE_BASE}"
            echo env.RELEASE_BASE

        }

        // TODO issue to get environement variable
        def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
        def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
        def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

        if (DEBUG_RUN) {
            echo "2 - RELEASE=${RELEASE}"
            echo "2 - RELEASE_BASE=${RELEASE_BASE}"
        }

        if (RELEASE && RELEASE_BASE) {
            sh "git fetch --tags; git checkout ${RELEASE_BASE}"
        }

        env.GIT_COMMIT = getCommitId()
        if (DEBUG_RUN) {
            echo "GIT_COMMIT: ${GIT_COMMIT} - ${GIT_COMMIT}"
        }

        env.GIT_REVISION = getRevision()
        if (DEBUG_RUN) {
            echo "GIT_REVISION: ${GIT_REVISION} - ${GIT_REVISION}"
        }

        if (body) { body() }

    } // script

    //return env.GIT_REVISION
}
