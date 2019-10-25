#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withShellCheckWrapper.groovy`"

    vars = vars ?: [:]

    vars.pattern = vars.get("pattern", "*.sh")
    vars.shellcheckCmdParameters = vars.get("shellcheckCmdParameters", "")
    vars.shellOutputFile = vars.get("shellOutputFile", "shellcheck.log").trim()

    tee("${vars.shellOutputFile}") {

        shellcheckExitCode = sh(
            script: "shellcheck ${vars.shellcheckCmdParameters} -f checkstyle ${vars.pattern} 2>&1 > checkstyle.xml",
            returnStdout: true,
            returnStatus: true
        )

        echo "SHELL CHECK RETURN CODE : ${shellcheckExitCode}"
        if (vars.shellcheckExitCode == 0) {
            echo "SHELL CHECK SUCCESS"
        //} else {
        //    echo "SHELL CHECK UNSTABLE"
        //    currentBuild.result = 'UNSTABLE'
        }

    } // tee

    checkstyle canComputeNew: false, defaultEncoding: '', healthy: '50', pattern: '**/checkstyle.xml', shouldDetectModules: true, thresholdLimit: 'normal', unHealthy: '100'

    return shellcheckExitCode
}
