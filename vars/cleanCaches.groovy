#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/cleanCaches.groovy`"

    vars.isCleaningCachesEnabled = vars.get("isCleaningCachesEnabled", false).toBoolean()

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()

    if (isUnix()){
        if (vars.isCleaningStashEnabled == true || CLEAN_RUN == true) {
            sh "sudo /sbin/sysctl -w vm.drop_caches=3 || true"
        }
    }

    if (body) { body() }

}
