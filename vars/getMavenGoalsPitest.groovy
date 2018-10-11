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

    def skipPitest = vars.get("skipPitest", true).toBoolean()
    def mavenGoals = vars.get("mavenGoals", "")

    if (!skipPitest && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/))) {
        if (!DRY_RUN && !RELEASE) {
            echo "pitest added"
            mavenGoals += " -DwithHistory"
            mavenGoals += " org.pitest:pitest-maven:mutationCoverage"
        }
    } // if DRY_RUN

    if (body) { body() }

    return mavenGoals
}
