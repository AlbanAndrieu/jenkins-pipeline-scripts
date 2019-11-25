#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/cleanCaches.groovy`"

    vars.isCleaningCachesEnabled = vars.get("isCleaningCachesEnabled", false).toBoolean()

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

    if (isUnix()){
        if (vars.isCleaningStashEnabled == true || CLEAN_RUN == true) {
            sh "sudo /sbin/sysctl -w vm.drop_caches=3 || true"
        }
    }

    if (body) { body() }

}
