#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getMavenGoalsDocker.groovy`"

    vars = vars ?: [:]

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    def JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", 'stash-jenkins').trim()

    vars.skipDocker = vars.get("skipDocker", true).toBoolean()
    vars.mavenGoals = vars.get("mavenGoals", "")

    if (!vars.skipDocker) {
        if (!DRY_RUN && !RELEASE) {
            echo "docker added"
            withCredentials([
              usernamePassword(credentialsId: JENKINS_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')
            ]) {
                vars.mavenGoals += " -Ddocker.username=${USER} -Ddocker.password=${PASSWORD}"
            }
        } // if DRY_RUN
    }

    if (body) { body() }

    return vars.mavenGoals
}
