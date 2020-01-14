#!/usr/bin/groovy
import hudson.model.*
import static com.test.jenkins.sonar.Sonar.getSonarInclusions;

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withSonarQubeWrapper.groovy`"

    vars = vars ?: [:]

    def SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonar").trim()
    def SONAR_SCANNER = vars.get("SONAR_SCANNER", env.SONAR_SCANNER ?: "Sonar-Scanner-4.2").trim()
    def SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g").trim()
    //def SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: "$WORKSPACE").trim()
    def STASH_CREDENTIALS = vars.get("STASH_CREDENTIALS", env.STASH_CREDENTIALS ?: "jenkins").trim()

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)

    vars.propertiesPath = vars.get("propertiesPath", "sonar-project.properties")
    vars.bwoutputs = vars.get("bwoutputs", "").trim()
    vars.coverage = vars.get("coverage", "").trim()
    vars.verbose = vars.get("verbose", false).toBoolean()
    vars.buildCmdParameters = vars.get("buildCmdParameters", "").trim()
    vars.project = vars.get("project", "NABLA").trim()
    vars.projectVersion = vars.get("projectVersion", "")
    vars.repository = vars.get("repository", "").trim()
    vars.skipMaven = vars.get("skipMaven", true).toBoolean()
    vars.skipUnstable = vars.get("skipUnstable", false).toBoolean()
    vars.skipInclusion = vars.get("skipInclusion", false).toBoolean()
    vars.skipSonarCheck = vars.get("skipSonarCheck", true).toBoolean()
    vars.targetBranch = vars.get("targetBranch", "develop").trim()
    vars.isScannerHome = vars.get("isScannerHome", true).toBoolean()
    if (vars.isScannerHome == true) {
        def scannerHome = tool name: "${SONAR_SCANNER}", type: 'hudson.plugins.sonar.SonarRunnerInstallation'
        vars.sonarExecutable = vars.get("sonarExecutable", "${scannerHome}/bin/sonar-scanner")
    } else {
        // docker
        vars.sonarExecutable = vars.get("sonarExecutable", "/usr/local/sonar-runner/bin/sonar-scanner")
    }
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
    vars.shellOutputFile = vars.get("shellOutputFile", "sonar.log").trim()

    script {
        if (!DRY_RUN && !RELEASE) {

            tee("${vars.shellOutputFile}") {

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

                vars.buildCmdParameters += " -Dproject.settings=" + vars.propertiesPath

                if (!RELEASE_VERSION) {
                    echo 'No RELEASE_VERSION specified'
                    RELEASE_VERSION = getSemVerReleasedVersion(vars) ?: "LATEST"
                    vars.projectVersion = "${RELEASE_VERSION}"
                }

                if (vars.projectVersion?.trim()) {
                    vars.buildCmdParameters += " -Dsonar.projectVersion=${vars.projectVersion} "
                }

                if (vars.verbose) {
                    vars.buildCmdParameters += " -X -Dsonar.verbose=true "
                }

                if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
                    echo "[JPL] isReleaseBranch, so no check for `Sonar.getSonarInclusions`"
                } else {
                    if (!vars.skipInclusion) {
                        vars.jobName = vars.get("jobName", env.JOB_NAME)
                        vars.currentRevision = vars.get("currentRevision", env.GIT_COMMIT)

                        try {
                            // SynchronousNonBlockingStepExecution with usernamePassword not available in static groovy JPL
                            withCredentials([
                                usernamePassword(
                                credentialsId: STASH_CREDENTIALS,
                                usernameVariable: 'stashLogin',
                                passwordVariable: 'stashPass'
                                )
                            ]) {
                                vars.basicAuth = "${stashLogin}:${stashPass}".getBytes().encodeBase64().toString()
                            }

                            println("[JPL] Full CONFIG after applying the default values for getSonarInclusions is: ${vars}")
                            vars.buildCmdParameters += getSonarInclusions(vars)
                        }
                        catch(exc) {
                            echo 'Error: There were errors to retreive credentials. '+exc.toString() // but we do not fail the whole build because of that
                        }
                    }
                }

                // TODO Remove it when tee will be back
                //vars.buildCmdParameters += " 2>&1 > ${vars.shellOutputFile} "

                echo "Sonar GOALS have been specified: ${vars.buildCmdParameters}"

                withSonarQubeEnv("${SONAR_INSTANCE}") {
                    if (body) {
                        body()
                    }
                    def build = "FAIL"

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

                    echo "SONAR RETURN CODE : ${build}"
                    if (build == 0) {
                        echo "SONAR SUCCESS"
                    } else {
                        echo "SONAR UNSTABLE"
                        if (!vars.skipUnstable) {
                            currentBuild.result = 'UNSTABLE'
                            error 'There are errors in sonar'
                        }
                    }

                } // withSonarQubeEnv
                
                if (!vars.skipSonarCheck) {               
                    withSonarQubeCheck(vars)
                }

                archiveArtifacts artifacts: "${vars.shellOutputFile}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true

            } // tee

        } // if DRY_RUN
    } // script

}
