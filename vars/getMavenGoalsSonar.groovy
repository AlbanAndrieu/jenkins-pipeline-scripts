#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getMavenGoalsSonar.groovy`"

    vars = vars ?: [:]

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    vars.skipSonar = vars.get("skipSonar", true).toBoolean()
    vars.mavenGoals = vars.get("mavenGoals", "")

    if ( !vars.skipSonar ) {
        if (!DRY_RUN && !RELEASE) {
            echo "sonar added"
            if ( env.BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
                vars.mavenGoals += " -Dsonar.branch.name=${env.BRANCH_NAME}"
            } else {
                vars.mavenGoals += " -Dsonar.branch.name=${env.BRANCH_NAME}"
                vars.mavenGoals += " -Dsonar.branch.target=develop"
            }
            vars.mavenGoals += " sonar:sonar"
        }
    }

    if (body) { body() }

    return vars.mavenGoals
}
