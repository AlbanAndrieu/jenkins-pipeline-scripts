#!/usr/bin/groovy
//import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonardev")

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: true)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
    def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)

    def propertiesPath = vars.get("propertiesPath", "sonar-project.properties")
    def bwoutputs = vars.get("bwoutputs", "")
    def coverage = vars.get("coverage", "")
    def skipMaven = vars.get("skipMaven", false).toBoolean()

    script {
        if (!DRY_RUN && !RELEASE) {

            if (!skipMaven) {
               if (coverage?.trim()) {
                 unstash coverage
               }
               unstash 'maven-artifacts'
               unstash 'classes'
            }

            if (bwoutputs?.trim()) {
                unstash bwoutputs
            }

            if (DEBUG_RUN) {
                echo "SONAR_INSTANCE: ${SONAR_INSTANCE}"
            }

            def scannerHome = tool name: 'Sonar-Scanner-3.2', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
            withSonarQubeEnv("${SONAR_INSTANCE}") {

                if (body) { body() }

                if (env.BRANCH_NAME ==~ /develop/) {
                  sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=develop -Dproject.settings=" + propertiesPath
                } else {
                  sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=develop -Dproject.settings=" + propertiesPath
                }
            }

        } // if DRY_RUN
    } // script

}
