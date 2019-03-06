#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body) {

    vars = vars ?: [:]

    if (!body) {
        error 'no body specified, mandatory'
    }

    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()

    if (DEBUG_RUN && !body) {
        error 'no body specified, mandatory'
    }

    vars.isScmEnabled = vars.get("isScmEnabled", false).toBoolean()
    vars.isMavenEnabled = vars.get("isMavenEnabled", false).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", true).toBoolean()

    if (vars.isScmEnabled) {
        if (DEBUG_RUN) {
            echo "scm is enabled, using git!!! (SLOWER and UNSTABLE)"
        }
        // This is a bad because of the timeout which cannot be extended in jenkins

        gitCheckoutTEST(isCleaningEnabled: vars.isCleaningEnabled)

        dir ("test") {
            body()
        } // dir

    } else {
        if (DEBUG_RUN) {
            echo "scm is disabled, using unstash"
        }
        // This is a workaround because of got clone is too slow with lfs

        if (vars.isMavenEnabled) {
            unstash 'sources'
            unstash 'sources-tools'
        }

        dir ("test") {

            body()

        } // dir
    }

}
