#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withMavenDeployWrapper.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: true).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    vars.goal = vars.get("goal", "jar:jar deploy:deploy")
    vars.profile = vars.get("profile", "sonar")
    vars.skipTests = vars.get("skipTests", true).toBoolean()
    vars.skipResults = vars.get("skipResults", true).toBoolean()
    //vars.buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
    vars.skipSonar = vars.get("skipSonar", true).toBoolean()
    vars.skipPitest = vars.get("skipPitest", true).toBoolean()
    vars.buildCmdParameters = vars.get("buildCmdParameters", "")
    vars.artifacts = vars.get("artifacts", ['*_VERSION.TXT', '**/target/*.jar'].join(', '))
    vars.mavenOutputFile = vars.get("mavenOutputFile", "maven-deploy.log").trim()

    if (!DRY_RUN) {

        vars.buildCmdParameters += "-Dskip.npm -Dskip.yarn -Dskip.bower -Dskip.grunt -Dmaven.exec.skip=true -Denforcer.skip=true -Dmaven.test.skip=true"

        withMavenWrapper(vars) {

            if (body) { body() }

        }

    } // if DRY_RUN

}
