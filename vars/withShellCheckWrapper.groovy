#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withShellCheckWrapper.groovy`"

    vars = vars ?: [:]

    def SHELLCHECK_OPTS = vars.get("SHELLCHECK_OPTS", env.SHELLCHECK_OPTS ?: "-e SC2154 -e SC2086").trim()

    vars.pattern = vars.get("pattern", "*.sh")
    vars.shellcheckCmdParameters = vars.get("shellcheckCmdParameters", "")
    vars.shellOutputFile = vars.get("shellOutputFile", "shellcheck.log").trim()

    vars.isSuccessReturnCode = vars.get("isSuccessReturnCode", 0)
    vars.isFailReturnCode = vars.get("isFailReturnCode", 255)
    vars.isUnstableReturnCode = vars.get("isUnstableReturnCode", 1)

    tee("${vars.shellOutputFile}") {
        if (isUnix()) {

        shellcheckExitCode = sh(
              script: """#!/bin/bash -l
              export SHELLCHECK_OPTS=\"${SHELLCHECK_OPTS}\"
              shellcheck ${vars.shellcheckCmdParameters} -f checkstyle ${vars.pattern} 2>&1 > checkstyle.xml""",
            returnStdout: true,
            returnStatus: true
        )

        echo "SHELLCHECK RETURN CODE : ${shellcheckExitCode}"
        if (shellcheckExitCode == vars.isSuccessReturnCode) {
            echo "SHELLCHECK SUCCESS"
        } else if (shellcheckExitCode == vars.isFailReturnCode) {
           echo "SHELLCHECK FAILURE"
           currentBuild.result = 'FAILURE'
           error 'There are errors in shellcheck'
		} else if (shellcheckExitCode <= vars.isUnstableReturnCode) {
			echo "SHELLCHECK UNSTABLE"
			currentBuild.result = 'UNSTABLE'
		} else {
			echo "SHELLCHECK FAILURE"
			//currentBuild.result = 'FAILURE'
			error 'There are other errors'
		}

	  } // isUnix

    } // tee

    checkstyle canComputeNew: false, defaultEncoding: '', healthy: '50', pattern: '**/checkstyle.xml', shouldDetectModules: true, thresholdLimit: 'normal', unHealthy: '100'

    return shellcheckExitCode
}
