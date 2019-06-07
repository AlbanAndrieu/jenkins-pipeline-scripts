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

    vars.DOCKER_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: "temp")
    vars.DOCKER_TEST_TAG = dockerTag(vars.DOCKER_TAG)
    vars.DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${vars.DOCKER_TEST_TAG}_test_1")
    vars.DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "--exit-code-from frarcalmtest frarcalmtest")
    vars.DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${vars.DOCKER_TEST_TAG}")

    vars.ADDITIONAL_ALMTEST_OPTS = vars.get("ADDITIONAL_ALMTEST_OPTS", env.ADDITIONAL_ALMTEST_OPTS ?: "-Config ./ALMTestConfig.plist -EnvironmentId ${vars.DOCKER_TEST_TAG} -ResultJUnitPublish YES")
    vars.ALMTEST_RESULTS_PATH = vars.get("ALMTEST_RESULTS_PATH", env.ALMTEST_RESULTS_PATH ?: "./almtest-${env.GIT_COMMIT}-${env.BUILD_NUMBER}")

    vars.dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")
    vars.allowEmptyResults = vars.get("allowEmptyResults", env.allowEmptyResults ?: false).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()

    script {

        if (!DRY_RUN && !RELEASE && BRANCH_NAME ==~ /develop|PR-.*|feature\/.*|bugfix\/.*/ ) {

            try {

                lock(resource: "lock_ALMTEST_${env.NODE_NAME}", inversePrecedence: true) {

                    timeout(180) {

                        if (CLEAN_RUN) {
                            sh "rm -Rf result"
                        }

                        dockerComposeTest(vars) {

                            sh "docker cp frarcalmtest:${vars.ALMTEST_RESULTS_PATH} result || true"

                            runHtmlPublishers(["ALMTestPublisher": [reportDir: "result/latestResult/"]])

                            junit testResults: 'result/latestResult/junit.xml', healthScaleFactor: 2.0, allowEmptyResults: vars.allowEmptyResults, keepLongStdio: true

                            if (body) { body() }

                        } // dockerComposeTest

                    } // timeout

                } // lock

            } catch(exc) {
                error 'There are errors in dockerTestALMTEST'
            } finally {
                archiveArtifacts artifacts: "${vars.ALMTEST_RESULTS_PATH}/**/*.log, *.log, Build/logs/*.log, Output/**/MGR-ARC-*.stdout.log", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
            } // finally

        }  // DRY_RUN

    } // script

}
