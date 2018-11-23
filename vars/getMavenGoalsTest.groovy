#!/usr/bin/groovy
//import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)

    def skipTests = vars.get("skipTests", true).toBoolean()
    def mavenGoals = vars.get("mavenGoals", "")

    // Force skipping tests
    if (DRY_RUN || RELEASE) {
        skipTests = true
    }

    mavenGoals += " -Dmaven.test.skip=${skipTests}"

    if (isReleaseBranch()) {
        echo "skip test added"
        mavenGoals += " -Dmaven.test.failure.ignore=true -Dmaven.test.failure.skip=true"
    }

    if (body) { body() }

    return mavenGoals
}
