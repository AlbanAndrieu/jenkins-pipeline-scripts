#!/usr/bin/groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getGitData.groovy`"
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

        def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
        def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: "")

        env.RELEASE_VERSION = getReleasedVersion(vars) ?: "0.0.1"

        if (DEBUG_RUN) {
            echo "2 - RELEASE=${RELEASE}"
            echo "3 - RELEASE_BASE=${RELEASE_BASE}"
            echo "4 - RELEASE_VERSION=${env.RELEASE_VERSION}"
        }

        if (RELEASE && RELEASE_BASE) {
            sh "git fetch --tags; git checkout ${RELEASE_BASE}"
        }

        env.GIT_COMMIT = getCommitId()
        if (DEBUG_RUN) {
            echo "GIT_COMMIT: ${env.GIT_COMMIT}"
        }

        env.GIT_COMMIT_SHORT = getCommitShortSHA1()
        if (DEBUG_RUN) {
            echo "GIT_COMMIT_SHORT: ${env.GIT_COMMIT_SHORT}"
        }

        env.BUILD_TIMESTAMP = getTimestamp()
        if (DEBUG_RUN) {
            echo "BUILD_TIMESTAMP: ${env.BUILD_TIMESTAMP}"
        }

        environment()

        if (body) { body() }

    } // script

}
