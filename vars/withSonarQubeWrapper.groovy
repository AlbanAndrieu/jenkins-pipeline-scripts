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
    def SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g")
    //def SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: "$WORKSPACE")

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    vars.propertiesPath = vars.get("propertiesPath", "sonar-project.properties")
    vars.bwoutputs = vars.get("bwoutputs", "")
    vars.coverage = vars.get("coverage", "")
    vars.verbose = vars.get("verbose", false).toBoolean()
    vars.buildCmdParameters = vars.get("buildCmdParameters", "")
    vars.project = vars.get("project", "NABLA")
    vars.repository = vars.get("repository", "")
    vars.skipMaven = vars.get("skipMaven", true).toBoolean()
    vars.skipInclusion = vars.get("skipInclusion", false).toBoolean()
    vars.targetBranch = vars.get("targetBranch", "develop")
    vars.scannerHome = tool name: 'Sonar-Scanner-3.2', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
    vars.sonarExecutable = vars.get("sonarExecutable", "${scannerHome}/bin/sonar-scanner")

    script {
        if (!DRY_RUN && !RELEASE) {
        
            tee("sonar.log") {

                if (!vars.skipMaven) {
                   if (vars.coverage?.trim()) {
                     unstash vars.coverage
                   }
                   unstash 'maven-artifacts'
                   unstash 'classes'
                }
			    
                if (vars.bwoutputs?.trim()) {
                    unstash vars.bwoutputs
                }
			    
                if (DEBUG_RUN) {
                    echo "SONAR_INSTANCE: ${SONAR_INSTANCE}"
                    vars.verbose = true
                }
			    
                bvars.uildCmdParameters += " -Dproject.settings=" + vars.propertiesPath
			    
                if (vars.verbose) {
                   vars.buildCmdParameters += " -X -Dsonar.verbose=true "
                }
			    
                if (!vars.skipInclusion) {
                  vars.buildCmdParameters += getSonarInclusions(vars)
                } 
			    
                withSonarQubeEnv("${SONAR_INSTANCE}") {
                    if (body) {
                        body()
                    }
                    if ( BRANCH_NAME ==~ /develop|master|master_.+/ ) {
                        build = sh (
                            script: "${vars.sonarExecutable} -Dsonar.branch.name=${env.BRANCH_NAME} " + vars.buildCmdParameters + " ",
                            returnStatus: true
                        )
                    } else if ( BRANCH_NAME ==~ /release\/.+/ ) {
                        build = sh (
                            script: "${vars.sonarExecutable} -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=master " + vars.buildCmdParameters + " ",
                            returnStatus: true
                        )
                    } else {
                        build = sh (
                            script: "${vars.sonarExecutable} -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=${vars.targetBranch} " + vars.buildCmdParameters + " ",
                            returnStatus: true
                        )
                    }
			    
                    echo "BUILD RETURN CODE : ${build}"
                    if (build == 0) {
                        echo "SONAR SUCCESS"
                    } else {
                        echo "SONAR UNSTABLE"
                        currentBuild.result = 'UNSTABLE'
                    }
			    
                } // withSonarQubeEnv
			    
                archiveArtifacts artifacts: "sonar.log", excludes: null, fingerprint: false, onlyIfSuccessful: false, allowEmptyArchive: true
            
            } // tee

        } // if DRY_RUN
    } // script

}
