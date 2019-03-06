#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/wrapCleanWs.groovy`"

    node('docker-inside') {

        script {

            //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
            def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
            def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

            if (!DEBUG_RUN) {
                cleanWs()
            } else {
                echo "Hi from wrapCleanWs"
            }
            if (!DRY_RUN && !DEBUG_RUN) {
                standardNotify { }
            }

            if (body) { body() }
        } // script

    } // node

}
