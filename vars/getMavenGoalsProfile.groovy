#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getMavenGoalsProfile.groovy`"

    vars = vars ?: [:]

    //def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    vars.skipProfile = vars.get("skipProfile", false).toBoolean()
    vars.skipObfuscation = vars.get("skipObfuscation", false).toBoolean()
    vars.skipSigning = vars.get("skipSigning", false).toBoolean()
    vars.profile = vars.get("profile", "sonar")
    vars.mavenGoals = vars.get("mavenGoals", "")

    if (!vars.skipProfile) {
        if (RELEASE) {
            echo "release profile added"
            vars.profile += ",release"        
        }
        if (!vars.skipObfuscation && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /release.*/) || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/)) ) {
            echo "zkm obfuscation profile added"
            vars.profile += ",obfuscation"            
        }
        if (!vars.skipSigning && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /release.*/)) ) {
            echo "signing profile added"
            vars.profile += ",signing"            
        }        
		if (vars.profile?.trim()) {
			vars.mavenGoals += " -P${vars.profile}"
		}        
    } // if skipProfile

    if (body) { body() }

    return vars.mavenGoals
}
