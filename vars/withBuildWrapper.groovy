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

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: false)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
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

            sh "${script}"

            if (body) { body() }

        } catch (e) {
            step([$class: 'ClaimPublisher'])
            throw e
        }

        step([
             $class: "WarningsPublisher",
             canComputeNew: false,
             canResolveRelativePaths: false,
             canRunOnFailed: true,
             consoleParsers: [
                 [
                     parserName: 'Java Compiler (javac)'
                 ],
                 [
                     parserName: 'Maven'
                 ],
                 [
                     parserName: 'GNU Make + GNU C Compiler (gcc)', pattern: 'error_and_warnings.txt'
                 ],
                 [
                     parserName: 'Clang (LLVM based)', pattern: 'error_and_warnings_clang.txt'
                 ]
             ],
             //unstableTotalAll: '10',
             //unstableTotalHigh: '0',
             //failedTotalAll: '10',
             //failedTotalHigh: '0',
             usePreviousBuildAsReference: true,
             useStableBuildAsReference: true
             ])

        stash includes: "${artifacts}", name: 'scons-artifacts-' + arch
        stash allowEmpty: true, includes: "bw-outputs/*", name: 'bwoutputs-' + arch

    } // wrapInARC

}
