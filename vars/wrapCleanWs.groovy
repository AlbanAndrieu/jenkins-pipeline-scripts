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

		if (body) { body() }

		cleanStash(vars)
		cleanCaches(vars)

		if (!isDebugRun(vars) && vars.isCleaningEnabled) {
			cleanWs(disableDeferredWipeout: true, deleteDirs: true)
		}

		if (!isDryRun(vars) && !isDebugRun(vars) && vars.isEmailEnabled) {
			standardNotify { }
		}
	} // script

}
