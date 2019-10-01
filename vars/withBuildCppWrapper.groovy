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

    echo "[JPL] Executing `vars/withBuildCppWrapper.groovy`"

    vars = vars ?: [:]

    def arch = vars.get("arch", "TEST")
    def script = vars.get("script", "build.sh")
    def artifacts = vars.get("artifacts", ['*_VERSION.TXT',
                   '*.md5',
                   '*.tar.gz',
                   '*.tgz',
                   ].join(', '))

    vars.isScmEnabled = vars.get("isScmEnabled", true).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", true).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
    vars.isStashSconEnabled = vars.get("isStashSconEnabled", true).toBoolean()
    vars.isStashMavenEnabled = vars.get("isStashMavenEnabled", false).toBoolean()
    vars.buildOutputFile = vars.get("buildOutputFile", "scons-${arch}.log").trim()

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def SCONS_OPTS = vars.get("SCONS_OPTS", env.SCONS_OPTS ?: "")

	try {

		//tee("${vars.buildOutputFile}") {

			echo "DRY_RUN : ${DRY_RUN}"
			if (!DRY_RUN && vars.isStashMavenEnabled) {
				echo "Unstash"
				unstash 'maven-artifacts'
				unstash 'app'
			}

			if (CLEAN_RUN) {
				SCONS_OPTS += "--cache-disable"
				sh "rm -Rf ./bw-outputs || true"
				sh "rm -Rf ../bw-outputs || true"
			}

			if (DEBUG_RUN) {
				echo "Scons OPTS have been specified: ${SCONS_OPTS}"
			}

			if (body) { body() }

			build = sh (
			  script: "${script} 2>&1 > ${vars.buildOutputFile}",
			  returnStatus: true
			)

			echo "BUILD RETURN CODE : ${build}"
			if (build == 0) {
				echo "BUILD SUCCESS"
			} else {
				echo "BUILD FAILURE"
				currentBuild.result = 'FAILURE'
				error 'There are errors in build'
			}

		//} // tee

	} finally {
		//runHtmlPublishers(["WarningsPublisher"])
		archiveArtifacts artifacts: "bw-outputs/build-wrapper.log, *.log, *_VERSION.TXT", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true

		if (vars.isStashSconEnabled) {
		   stash includes: "${artifacts}", name: 'scons-artifacts-' + arch
		   stash allowEmpty: true, includes: "../bw-outputs/build-wrapper-dump.json, bw-outputs/build-wrapper-dump.json", name: 'bwoutputs-' + arch
		}

	}

}
