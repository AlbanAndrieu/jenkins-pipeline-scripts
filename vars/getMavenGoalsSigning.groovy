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
    
    try {

        if (!vars.skipSigning && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /release.*/))) {
            if (!DRY_RUN && !RELEASE) {
                echo "signing added"
                // string(credentialsId: 'jenkins-arc-keystore', variable: 'KEYSTORE'),
                withCredentials([
                    [$class: 'StringBinding', credentialsId: 'jenkins-arc-keystore', variable: 'KEYSTORE'],
                    [$class: 'StringBinding', credentialsId: 'jenkins-arc-storepass', variable: 'STOREPASS']
                ]) {
                    echo " KEYSTORE : ${KEYSTORE} STOREPASS : ${STOREPASS}"
                    vars.mavenGoals += " -Djarsigner.skip=false -Djarsigner.keystore=${KEYSTORE} -Djarsigner.storepass=${STOREPASS}"
                }
            } // if DRY_RUN
        } else {
            vars.mavenGoals += " -Djarsigner.skip=true "
        }

	} catch(exc) {
        echo 'Error: There were errors to retreive credentials. '+exc.toString() // but we do not fail the whole build because of that
    }
		
    if (body) { body() }

    return vars.mavenGoals
}
