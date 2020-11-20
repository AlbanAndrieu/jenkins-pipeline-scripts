#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getMavenGoalsTest.groovy`"

    vars = vars ?: [:]

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    vars.skipTests = vars.get("skipTests", false).toBoolean()
    vars.skipReleaseTests = vars.get("skipReleaseTests", true).toBoolean()
    vars.skipRetryTests = vars.get("skipRetryTests", true).toBoolean()
    vars.mavenGoals = vars.get("mavenGoals", "")

    // Force skipping tests
    if (DRY_RUN || RELEASE) {
        if (vars.skipReleaseTests.toBoolean()) {
            vars.skipTests = true
        }
    }

    if (vars.skipTests.toBoolean()) {
        vars.mavenGoals += " -Dmaven.test.skip=${vars.skipTests}"
    }

    if (isReleaseBranch()) {
        //echo "skip test added"
        //vars.mavenGoals += " -Dmaven.test.failure.ignore=true -Dmaven.test.failure.skip=true"
        if (!vars.skipRetryTests.toBoolean()) {
            vars.mavenGoals += " -Dsurefire.skipAfterFailureCount=2 -Dsurefire.rerunFailingTestsCount=3 "
	      }
    }

    if (body) { body() }

    return vars.mavenGoals
}
