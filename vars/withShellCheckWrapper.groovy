#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def pattern = vars.get("pattern", "*.sh")

    shellcheckExitCode = sh(
        script: "shellcheck -x -f checkstyle ${pattern} > checkstyle.xml",
        returnStdout: true,
        returnStatus: true
    )

    echo "SHELL CHECK RETURN CODE : ${shellcheckExitCode}"
    if (shellcheckExitCode == 0) {
        echo "TEST SUCCESS"
    } else {
        echo "TEST UNSTABLE"
        //currentBuild.result = 'UNSTABLE'
    }

    checkstyle canComputeNew: false, defaultEncoding: '', healthy: '50', pattern: '**/checkstyle.xml', shouldDetectModules: true, thresholdLimit: 'normal', unHealthy: '100'

}
