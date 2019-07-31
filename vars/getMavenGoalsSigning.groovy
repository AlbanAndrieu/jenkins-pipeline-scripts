#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getMavenGoalsSigning.groovy`"

    vars = vars ?: [:]

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    vars.skipSigning = vars.get("skipSigning", true).toBoolean()
    vars.mavenGoals = vars.get("mavenGoals", "")
    
    vars.mavenGoals += " -Djarsigner.skip=" + vars.skipSigning + " "
	
    if (body) { body() }

    return vars.mavenGoals
}
