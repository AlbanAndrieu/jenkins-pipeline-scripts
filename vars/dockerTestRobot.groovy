#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/dockerTestRobot.groovy`"

    vars = vars ?: [:]

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.mobi").toLowerCase().trim()
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins").trim()
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla").trim()

    def DOCKER_ROBOT_RUNTIME_TAG = vars.get("DOCKER_ROBOT_RUNTIME_TAG", env.DOCKER_ROBOT_RUNTIME_TAG ?: "develop").trim()
    def DOCKER_ROBOT_RUNTIME_NAME = vars.get("DOCKER_ROBOT_RUNTIME_NAME", env.DOCKER_ROBOT_RUNTIME_NAME ?: "robot").trim()
    def DOCKER_ROBOT_RUNTIME_IMG = vars.get("DOCKER_ROBOT_RUNTIME_IMG", env.DOCKER_ROBOT_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_ROBOT_RUNTIME_NAME}:${DOCKER_ROBOT_RUNTIME_TAG}")

    vars.DOCKER_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: "temp").trim()
    vars.DOCKER_TEST_TAG = dockerTag(vars.DOCKER_TAG).trim()
    vars.DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${vars.DOCKER_TEST_TAG}_robot_1").trim()
    vars.DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "--force-recreate --exit-code-from robot robot").trim()
    vars.DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${vars.DOCKER_TEST_TAG}").trim()

    vars.ADDITIONAL_ROBOT_OPTS = vars.get("ADDITIONAL_ROBOT_OPTS", env.ADDITIONAL_ROBOT_OPTS ?: "-s PipelineTests.TEST -e disabled").trim()
    vars.containerName = vars.get("containerName", "robot").trim()
    vars.dockerResultPath = vars.get("dockerResultPath", "./${vars.containerName}-${env.GIT_COMMIT}-${env.BUILD_NUMBER}").trim()
    //vars.ROBOT_RESULTS_PATH = vars.get("ROBOT_RESULTS_PATH", env.ROBOT_RESULTS_PATH ?: "./robot-${env.GIT_COMMIT}-${env.BUILD_NUMBER}").trim()

    vars.dockerFilePath = vars.get("dockerFilePath", "./docker/centos7/run/").trim()
    vars.allowEmptyResults = vars.get("allowEmptyResults", false).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()

    //vars.isSuccessReturnCode = vars.get("isSuccessReturnCode", 0)
    vars.isFailReturnCode = vars.get("isFailReturnCode", 255)
    vars.isUnstableReturnCode = vars.get("isUnstableReturnCode", 250)

    script {

        if (!DRY_RUN && !RELEASE && BRANCH_NAME ==~ /develop|PR-.*|feature\/.*|bugfix\/.*/ ) {

            try {

                lock(resource: "lock_ROBOT_${env.NODE_NAME}", inversePrecedence: true) {

                    //timeout(180) {

                        if (CLEAN_RUN) {
                            sh "rm -Rf result"
                        }

                        dockerComposeTest(vars) {

                            try {

                                getContainerResults(vars)

							    if (!vars.allowEmptyResults) {
                                    runHtmlPublishers(["RobotPublisher": [outputPath: "result"]])
                                }

                                //junit testResults: 'result/results/output.xml', healthScaleFactor: 2.0, allowEmptyResults: vars.allowEmptyResults, keepLongStdio: true

                            } catch(exc) {
                                if (!vars.allowEmptyResults) {
                                    error 'There are retreiving results from docker container'
                                }
                            }

                            if (body) { body() }

                        } // dockerComposeTest

                    //} // timeout

                } // lock

            } catch(e) {
                echo 'There are errors in dockerTestRobot'
            } finally {
                archiveArtifacts artifacts: "${vars.dockerResultPath}/**/*.log, *.log, result/**/*", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
            } // finally

        }  // DRY_RUN

    } // script

}
