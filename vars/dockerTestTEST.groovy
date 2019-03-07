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

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry-tmp.misys.global.ad")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "mgr.jenkins")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "fusion-risk")

    def DOCKER_ALMTEST_RUNTIME_TAG = vars.get("DOCKER_ALMTEST_RUNTIME_TAG", env.DOCKER_ALMTEST_RUNTIME_TAG ?: "latest")
    def DOCKER_ALMTEST_RUNTIME_NAME = vars.get("DOCKER_ALMTEST_RUNTIME_NAME", env.DOCKER_ALMTEST_RUNTIME_NAME ?: "arc-almtest-centos7")
    def DOCKER_ALMTEST_RUNTIME_IMG = vars.get("DOCKER_ALMTEST_RUNTIME_IMG", env.DOCKER_ALMTEST_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_ALMTEST_RUNTIME_NAME}:${DOCKER_ALMTEST_RUNTIME_TAG}")

    def DOCKER_TEST_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: buildDockerTag("${env.BRANCH_NAME}", "${env.GIT_COMMIT}").toLowerCase())
    //def DOCKER_TEST_LINK = vars.get("DOCKER_TEST_LINK", env.DOCKER_TEST_LINK ?: "${DOCKER_TEST_TAG}_frarc_1:frarc")
    def DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "--exit-code-from frarcalmtest frarcalmtest")
    def DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${env.DOCKER_TEST_TAG}")
    def ADDITIONAL_ALMTEST_OPTS = vars.get("ADDITIONAL_ALMTEST_OPTS", env.ADDITIONAL_ALMTEST_OPTS ?: "-Config ./ALMTestConfig.plist -EnvironmentId ${DOCKER_TEST_TAG} -ResultJUnitPublish YES")
    def ALMTEST_RESULTS_PATH = vars.get("ALMTEST_RESULTS_PATH", env.ALMTEST_RESULTS_PATH ?: "./almtest-${env.GIT_COMMIT}-${env.BUILD_NUMBER}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")

    script {

        if (!DRY_RUN && !RELEASE && BRANCH_NAME ==~ /develop|PR-.*|feature\/.*|bugfix\/.*/ ) {

            try {

                lock(resource: "lock_ALMTEST_${env.NODE_NAME}", inversePrecedence: true) {

                    timeout(180) {

                        if (CLEAN_RUN) {
                            sh "rm -Rf result"
                            // Almonde/result/latestResult
                        }

                        dockerComposeTest(DOCKER_REGISTRY: DOCKER_REGISTRY,
                            DOCKER_TEST_TAG: DOCKER_TEST_TAG,
                            DOCKER_TEST_CONTAINER: "${DOCKER_TEST_TAG}_frarc_1",
                            DOCKER_COMPOSE_UP_OPTIONS: DOCKER_COMPOSE_UP_OPTIONS,
                            dockerFilePath: dockerFilePath) {

                            sh "docker cp frarcalmtest:${ALMTEST_RESULTS_PATH} result || true"

                            runHtmlPublishers(["ALMTestPublisher": [reportDir: "result/latestResult/"]])

                            junit testResults: 'result/latestResult/junit.xml', healthScaleFactor: 2.0, allowEmptyResults: true, keepLongStdio: true, testDataPublishers: [[$class: 'ClaimTestDataPublisher']]

                            if (body) { body() }

                        } // dockerComposeTest

                    } // timeout

                } // lock

            } catch(exc) {
                error 'There are errors in dockerTestALMTEST'
            } finally {
                archiveArtifacts artifacts: "${ALMTEST_RESULTS_PATH}/**/*.log, *.log, Build/logs/*.log", excludes: null, fingerprint: false, onlyIfSuccessful: false, allowEmptyArchive: true
            } // finally

        }

    } // script

}
