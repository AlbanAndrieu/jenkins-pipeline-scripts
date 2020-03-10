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

    def arch = vars.get("arch", "TEST").trim()
    def script = vars.get("script", "build.sh").trim()
    def artifacts = vars.get("artifacts", ['*_VERSION.TXT',
                   '*.md5',
                   '*.tar.gz',
                   '*.tgz',
                   ].join(', '))
    def excludes = vars.get("excludes", ['Latest*.tar.gz'
                   ].join(', '))

    vars.isScmEnabled = vars.get("isScmEnabled", true).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", true).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
    vars.isStashSconEnabled = vars.get("isStashSconEnabled", true).toBoolean()
    vars.isStashMavenEnabled = vars.get("isStashMavenEnabled", false).toBoolean()
    vars.shellOutputFile = vars.get("shellOutputFile", "${arch}-scons.log").trim()

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def SCONS_OPTS = vars.get("SCONS_OPTS", env.SCONS_OPTS ?: "").trim()

	try {

		tee("${vars.shellOutputFile}") {

		    lock(resource: "lock_CPP_${env.NODE_NAME}", inversePrecedence: true) {

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
			    	sh "rm -f *_VERSION.TXT"
			    }

			    if (DEBUG_RUN) {
			    	echo "Scons OPTS have been specified: ${SCONS_OPTS}"
			    }

			    if (body) { body() }

			    build = sh (
			      //script: "${script} 2>&1 > ${vars.shellOutputFile}",
                              script: "${script},
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

			} // lock

		} // tee

	} finally {
		//runHtmlPublishers(["WarningsPublisher"])

		artifacts += ", bw-outputs/build-wrapper.log, *.log"
		echo "archiveArtifacts: ${artifacts} - ${excludes}"
		archiveArtifacts artifacts: "${artifacts}", excludes: "${excludes}", fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true

		if (vars.isStashSconEnabled) {
		   stash includes: "${artifacts}", name: 'scons-artifacts-' + arch
		   stash allowEmpty: true, includes: "../bw-outputs/build-wrapper-dump.json, bw-outputs/build-wrapper-dump.json", name: 'bwoutputs-' + arch
		}

	}

}
