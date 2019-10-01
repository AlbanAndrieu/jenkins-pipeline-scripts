#!/usr/bin/groovy
import java.*
import hudson.*
import hudson.model.*
import jenkins.model.*
import com.cloudbees.groovy.cps.NonCPS

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withBuildWrapper.groovy`"

    vars = vars ?: [:]

    if (!body) {
        echo 'No body specified'
    }

    vars.artifacts = ['*_VERSION.TXT',
                   '**/MD5SUMS.md5',
                   '**/Output/**/*.tar.gz'
                   ].join(', ')

    vars.isStashMavenEnabled = true

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def SCONS_OPTS = vars.get("SCONS_OPTS", env.SCONS_OPTS ?: "")

    wrapInTEST(vars) {

        withBuildCppWrapper(vars) {

		    if (body) { body() }

			if (DEBUG_RUN) {
				getEnvironementData(filePath: "./Build/step-2-0-0-build-env.sh", DEBUG_RUN: DEBUG_RUN)
			}

		} // withBuildCppWrapper

    } // wrapInTEST

}
