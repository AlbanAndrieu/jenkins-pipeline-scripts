#!/usr/bin/groovy
//import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def pattern = vars.get("pattern", "*.sh")

	shellcheckExitCode = sh(
		script: "shellcheck -f checkstyle ${pattern} > checkstyle.xml",
		returnStdout: true,
		returnStatus: true
	)
	//sh "echo ${shellcheckExitCode}"

	checkstyle canComputeNew: false, defaultEncoding: '', healthy: '50', pattern: '**/checkstyle.xml', shouldDetectModules: true, thresholdLimit: 'normal', unHealthy: '100'

}
