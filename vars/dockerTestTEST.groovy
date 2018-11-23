#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: false)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.mobi")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla")

    def DOCKER_TEST_RUNTIME_TAG = vars.get("DOCKER_TEST_RUNTIME_TAG", env.DOCKER_TEST_RUNTIME_TAG ?: "latest")
    def DOCKER_TEST_RUNTIME_NAME = vars.get("DOCKER_TEST_RUNTIME_NAME", env.DOCKER_TEST_RUNTIME_NAME ?: "test-centos7")
    def DOCKER_TEST_RUNTIME_IMG = vars.get("DOCKER_TEST_RUNTIME_IMG", env.DOCKER_TEST_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_TEST_RUNTIME_NAME}:${DOCKER_TEST_RUNTIME_TAG}")

    def DOCKER_TEST_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: buildDockerTag("${env.BRANCH_NAME}", "${env.GIT_COMMIT}").toLowerCase())
    //def DOCKER_TEST_LINK = vars.get("DOCKER_TEST_LINK", env.DOCKER_TEST_LINK ?: "${DOCKER_TEST_TAG}_test_1:test")
    def DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "--exit-code-from toto testtoto
    def DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${env.DOCKER_TEST_TAG}")
    def ADDITIONAL_TEST_OPTS = vars.get("ADDITIONAL_TEST_OPTS", env.ADDITIONAL_TEST_OPTS ?: "-Config ./Config.properties -EnvironmentId ${DOCKER_TEST_TAG} -ResultJUnitPublish YES")
    def TEST_RESULTS_PATH = vars.get("TEST_RESULTS_PATH", env.TEST_RESULTS_PATH ?: "./toto-${env.GIT_COMMIT}-${env.BUILD_NUMBER}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")

    script {

        if (!DRY_RUN && !RELEASE && BRANCH_NAME ==~ /develop|PR-.*|feature\/.*|bugfix\/.*/ ) {

            timeout(180) {

                if (CLEAN_RUN) {
                    echo "TEST_RESULTS_PATH : ${TEST_RESULTS_PATH}/latestResult/ "
                    sh "rm -Rf result"
                    sh "rm -f ${TEST_RESULTS_PATH}/latestResult/junit.xml"
                    //sh "rm -f result/latestResult/junit.xml"
                }

                dockerComposeTest(DOCKER_TEST_TAG: DOCKER_TEST_TAG,
                    DOCKER_TEST_CONTAINER: "${DOCKER_TEST_TAG}_test_1",
                    DOCKER_COMPOSE_UP_OPTIONS: DOCKER_COMPOSE_UP_OPTIONS,
                    dockerFilePath: dockerFilePath) {

                    // --memory 1024m --cpus="1.5"
                    //docker.image("${DOCKER_TEST_RUNTIME_IMG}").withRun("-e \"TEST_RESULTS_PATH=${TEST_RESULTS_PATH}\"", "${ADDITIONAL_TEST_OPTS}").inside("-v \"${TEST_RESULTS_PATH}:/tmp/result/\" -v \"arc-data:/home/fusionrisk/Data\" -v \"arc-root:/home/fusionrisk/ARC\" --link ${DOCKER_TEST_LINK}") { c ->
                    //docker.image("${DOCKER_TEST_RUNTIME_IMG}").withRun("-e \"TEST_RESULTS_PATH=${TEST_RESULTS_PATH}\"", "${ADDITIONAL_TEST_OPTS}") { c ->
                    //    sh "docker logs ${c.id}"
                    //}

                    sh "docker cp ${DOCKER_TEST_TAG}_toto_1:${TEST_RESULTS_PATH} result"

                    publishHTML([allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: "${TEST_RESULTS_PATH}/latestResult/",
                        //reportDir: "result/latestResult/",
                        //reportDir: "result/",
                        reportFiles: 'index.html',
                        reportName: 'Test Report',
                        reportTitles: 'TEST index'])

                }

            }

            archiveArtifacts artifacts: "result/latestResult/*.log, *.log, docker/run/logs/*.log", excludes: null, fingerprint: false, onlyIfSuccessful: false

            if (body) { body() }

        }

    } // script

}
