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

    def skipSonar = vars.get("skipSonar", true).toBoolean()
    def mavenGoals = vars.get("mavenGoals", "")

    if ( !skipSonar && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/))) {
        if (!DRY_RUN && !RELEASE) {
            echo "sonar added"
            if (env.BRANCH_NAME ==~ /develop/) {
                mavenGoals += " -Dsonar.branch.name=develop"
            } else {
                mavenGoals += " -Dsonar.branch.name=${env.BRANCH_NAME}"
                mavenGoals += " -Dsonar.branch.target=develop"
            }
            mavenGoals += " sonar:sonar"
        }
    }

    if (body) { body() }

    return mavenGoals
}
