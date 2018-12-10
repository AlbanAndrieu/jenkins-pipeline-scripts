#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    node('docker-inside') {

        script {

            //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: false)
            def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
            def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)

            if (!DEBUG_RUN) {
                cleanWs()
            } else {
                 echo "Hi from wrapCleanWs"
            }
            if (!DRY_RUN && !DEBUG_RUN)
                standardNotify { }

            if (body) { body() }
        } // script

    } // node

}
