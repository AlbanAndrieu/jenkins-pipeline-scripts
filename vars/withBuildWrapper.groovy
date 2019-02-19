#!/usr/bin/groovy
import java.*
import java.lang.*
import hudson.*
import hudson.model.*
import jenkins.model.Jenkins
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

    def arch = vars.get("arch", "TEST")
    def script = vars.get("script", "build.sh")
    def artifacts = vars.get("artifacts", ['*_VERSION.TXT',
                   '**/MD5SUMS.md5',
                   '**/Output/**/*.tar.gz'
                   ].join(', '))

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false)
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def SCONS_OPTS = vars.get("SCONS_OPTS", env.SCONS_OPTS ?: "")

    wrapInTEST(isScmEnabled: true) {

        if (!DRY_RUN) {
            unstash 'maven-artifacts'
            unstash 'app'
        }

        try {

            if (CLEAN_RUN) {
                SCONS_OPTS += "--cache-disable"
            }

            echo "Scons OPTS have been specified: ${SCONS_OPTS}"

            //getEnvironementData(filePath: "./bm/step-2-0-0-build-env.sh", DEBUG_RUN: DEBUG_RUN)

            build = sh (
              script: "${script} 2>&1 > scons-${ARCH}.log",
              returnStatus: true
            )
	        
            echo "BUILD RETURN CODE : ${build}"
            if (build == 0) {
                echo "TEST SUCCESS"
                //currentBuild.result = 'SUCCESS'
            } else {
                echo "TEST FAILURE"
                currentBuild.result = 'FAILURE'
            }

            if (body) { body() }

        } catch (e) {
            step([$class: 'ClaimPublisher'])
            throw e
        }

        runHtmlPublishers(["WarningsPublisher"])

        stash includes: "${artifacts}", name: 'scons-artifacts-' + arch
        stash allowEmpty: true, includes: "bw-outputs/build-wrapper-dump.json", name: 'bwoutputs-' + arch

    } // wrapInTEST

    archiveArtifacts artifacts: "bw-outputs/build-wrapper.log", excludes: null, fingerprint: false, onlyIfSuccessful: false, allowEmptyArchive: true

}
