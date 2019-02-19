#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withSonarQubeWrapper.groovy`"

    vars = vars ?: [:]

    def SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonardev")

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    def propertiesPath = vars.get("propertiesPath", "sonar-project.properties")
    def bwoutputs = vars.get("bwoutputs", "")
    def coverage = vars.get("coverage", "")
    def verbose = vars.get("verbose", false).toBoolean()
    def buildCmdParameters = vars.get("buildCmdParameters", "")
    def project = vars.get("project", "NABLA")
    def repository = vars.get("repository", "")
    def skipMaven = vars.get("skipMaven", true).toBoolean()
    def targetBranch = vars.get("targetBranch", "develop")
    def scannerHome = tool name: 'Sonar-Scanner-3.2', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
    def sonarExecutable = vars.get("sonarExecutable", "${scannerHome}/bin/sonar-scanner")

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
               buildCmdParameters += " -Dsonar.verbose=true "
            }

            buildCmdParameters += getSonarInclusions(vars)

            withSonarQubeEnv("${SONAR_INSTANCE}") {
                if (body) {
                    body()
                }
                if ( BRANCH_NAME ==~ /develop|master|master_.+/ ) {
                    build = sh (
                        script: "${sonarExecutable} -Dsonar.branch.name=${env.BRANCH_NAME} " + buildCmdParameters + " 2>&1 > sonar.log",
                        returnStatus: true
                    )
                } else if ( BRANCH_NAME ==~ /release\/.+/ ) {
                    build = sh (
                        script: "${sonarExecutable} -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=master " + buildCmdParameters + " 2>&1 > sonar.log",
                        returnStatus: true
                    )
                } else {
                    build = sh (
                        script: "${sonarExecutable} -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=${targetBranch} " + buildCmdParameters + " 2>&1 > sonar.log",
                        returnStatus: true
                    )
                }

                echo "BUILD RETURN CODE : ${build}"
                if (build == 0) {
                    echo "TEST SUCCESS"
                } else {
                    echo "TEST UNSTABLE"
                    currentBuild.result = 'UNSTABLE'
                }

            } // withSonarQubeEnv

            archiveArtifacts artifacts: "sonar.log", onlyIfSuccessful: false

        } // if DRY_RUN
    } // script

}
