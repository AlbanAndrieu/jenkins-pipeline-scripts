#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "albandrieu:6532/harbor")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla")

    vars.DOCKER_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: "temp")
    vars.DOCKER_TEST_TAG = dockerTag(vars.DOCKER_TAG)
    vars.DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${vars.DOCKER_TEST_TAG}_test_1")
    vars.DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "--exit-code-from frarcalmtest frarcalmtest")
    vars.DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${vars.DOCKER_TEST_TAG}")

    vars.ADDITIONAL_ALMTEST_OPTS = vars.get("ADDITIONAL_ALMTEST_OPTS", env.ADDITIONAL_ALMTEST_OPTS ?: "-Config ./TestConfig.plist -EnvironmentId ${vars.DOCKER_TEST_TAG} -ResultJUnitPublish YES")
    vars.TEST_RESULTS_PATH = vars.get("TEST_RESULTS_PATH", env.TEST_RESULTS_PATH ?: "./test-${env.GIT_COMMIT}-${env.BUILD_NUMBER}")

    vars.dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")
    vars.allowEmptyResults = vars.get("allowEmptyResults", env.allowEmptyResults ?: false).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()

    script {

        //if (!DRY_RUN && !RELEASE && BRANCH_NAME ==~ /develop|master|master_.+|release\/.+|PR-.*|feature\/.*|bugfix\/.*/ ) {
        if (!DRY_RUN && !RELEASE) {

            try {

                lock(resource: "lock_TEST_${env.NODE_NAME}", inversePrecedence: true) {

                        if (CLEAN_RUN) {
                            sh "rm -Rf result"
                        }

                        dockerComposeTest(vars) {

                            def containerId = getContainerId(vars)

                            if (containerId?.trim()) {
                                sh """
                                    docker cp ${containerId}:${vars.TEST_RESULTS_PATH} result || true
                                """
                            } else {
                                sh """
                                    docker cp frarcalmtest:${vars.TEST_RESULTS_PATH} result || true # OLD way when container name is hard coded in docker-compose. To be removed after full migration
                                """
                            }

                            runHtmlPublishers(["ALMTestPublisher": [reportDir: "result/latestResult/"]])

                            junit testResults: 'result/latestResult/junit.xml', healthScaleFactor: 2.0, allowEmptyResults: vars.allowEmptyResults, keepLongStdio: true

                            if (body) { body() }

                        } // dockerComposeTest

                } // lock

            } catch(exc) {
                error 'There are errors in dockerTestTEST'
            } finally {
                archiveArtifacts artifacts: "${vars.TEST_RESULTS_PATH}/**/*.log, *.log, result/**/*, Build/logs/*.log, Output/**/TEST-*.stdout.log", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
            } // finally

        }  // DRY_RUN

    } // script

}
