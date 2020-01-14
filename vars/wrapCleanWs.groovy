#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/wrapCleanWs.groovy`"

    vars.isEmailEnabled = vars.get("isEmailEnabled", true).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", true).toBoolean()

	script {
		//def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
		def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
		def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

		if (body) { body() }

		cleanStash(vars)
		cleanCaches(vars)

		if (!DEBUG_RUN && vars.isCleaningEnabled) {
			cleanWs(disableDeferredWipeout: true, deleteDirs: true)
		}
		
		if (!DRY_RUN && !DEBUG_RUN && vars.isEmailEnabled) {
			standardNotify { }
		}
	} // script

}
