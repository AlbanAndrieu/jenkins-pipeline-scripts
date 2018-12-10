#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonardev")

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: false)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
    def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)

    def propertiesPath = vars.get("propertiesPath", "sonar-project.properties")
    def bwoutputs = vars.get("bwoutputs", "")
    def coverage = vars.get("coverage", "")
    def verbose = vars.get("verbose", false).toBoolean()
    def buildCmdParameters = vars.get("buildCmdParameters", "")
    def skipMaven = vars.get("skipMaven", true).toBoolean()

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
                verbose = true
            }

            buildCmdParameters += " -Dproject.settings=" + propertiesPath

            if (verbose) {
               buildCmdParameters += " -Dsonar.verbose=true"

            }

            def scannerHome = tool name: 'Sonar-Scanner-3.2', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
            withSonarQubeEnv("${SONAR_INSTANCE}") {

                if (body) { body() }

                //if (isReleaseBranch()) {
                //BRANCH_NAME ==~ /develop|PR-.*|feature\/.*|bugfix\/.*/
                if ( BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
                  echo "isReleaseBranch"
                  sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME}" + buildCmdParameters
                } else {
                  sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=develop" + buildCmdParameters
                }
            }

        } // if DRY_RUN
    } // script

}
