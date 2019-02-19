#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withMavenDependencyCheckWrapper.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def goal = vars.get("goal", "org.owasp:dependency-check-maven:check")
    def profile = vars.get("profile", "sonar")
    def skipTests = vars.get("skipTests", true).toBoolean()
    def skipResults = vars.get("skipResults", true).toBoolean()
    //def buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
    def skipSonar = vars.get("skipSonar", true).toBoolean()
    def skipPitest = vars.get("skipPitest", true).toBoolean()
    def buildCmdParameters = vars.get("buildCmdParameters", "")
    def artifacts = vars.get("artifacts", ['*_VERSION.TXT', '**/target/*.jar'].join(', '))

    if (!DRY_RUN) {

        buildCmdParameters += "-Dskip.npm -Dskip.yarn -Dskip.bower -Dskip.grunt -Dmaven.exec.skip=true -Denforcer.skip=true -Dmaven.test.skip=true"

        withMavenWrapper(goal: goal,
        profile: profile,
        skipTests: skipTests,
        skipResults: skipResults,
        skipSonar: skipSonar,
        skipPitest: skipPitest,
        buildCmdParameters: buildCmdParameters,
        artifacts: artifacts) {

        if (body) { body() }

        }

        dependencyCheckPublisher canComputeNew: false, defaultEncoding: '', healthy: '50', pattern: '**/dependency-check-report.xml ', shouldDetectModules: true, thresholdLimit: 'normal', unHealthy: '100'

    } // if DRY_RUN

}
