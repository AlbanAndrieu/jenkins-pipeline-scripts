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

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.movi")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla")

    def DOCKER_ROBOT_RUNTIME_TAG = vars.get("DOCKER_ROBOT_RUNTIME_TAG", env.DOCKER_ROBOT_RUNTIME_TAG ?: "develop")
    def DOCKER_ROBOT_RUNTIME_NAME = vars.get("DOCKER_ROBOT_RUNTIME_NAME", env.DOCKER_ROBOT_RUNTIME_NAME ?: "robot")
    def DOCKER_ROBOT_RUNTIME_IMG = vars.get("DOCKER_ROBOT_RUNTIME_IMG", env.DOCKER_ROBOT_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_ROBOT_RUNTIME_NAME}:${DOCKER_ROBOT_RUNTIME_TAG}")

    def DOCKER_TEST_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: buildDockerTag("${env.BRANCH_NAME}", "${env.GIT_COMMIT}").toLowerCase())
    //def DOCKER_TEST_LINK = vars.get("DOCKER_TEST_LINK", env.DOCKER_TEST_LINK ?: "test:test")
    def DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "-d --force-recreate robot")
    def DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${env.DOCKER_TEST_TAG}")
    def ADDITIONAL_ROBOT_OPTS = vars.get("ADDITIONAL_ROBOT_OPTS", env.ADDITIONAL_ROBOT_OPTS ?: "-s PipelineTests.TEST")
    def ROBOT_RESULTS_PATH = vars.get("ROBOT_RESULTS_PATH", env.ROBOT_RESULTS_PATH ?: "/tmp/robot-${env.GIT_COMMIT}-${env.BUILD_NUMBER}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")

    script {

        if (!DRY_RUN && !RELEASE && BRANCH_NAME ==~ /develop|PR-.*|feature\/.*|bugfix\/.*/ ) {

            dockerComposeTest(DOCKER_TEST_TAG: DOCKER_TEST_TAG,
                DOCKER_TEST_CONTAINER: "${DOCKER_TEST_TAG}_test_1",
                DOCKER_COMPOSE_UP_OPTIONS: "-d --force-recreate robot",
                dockerFilePath: dockerFilePath) {

                // --memory 1024m --cpus="1.5"
                //docker.image("${DOCKER_ROBOT_RUNTIME_IMG}").withRun("-e \"ADDITIONAL_ROBOT_OPTS=${ADDITIONAL_ROBOT_OPTS}\"").inside("--link ${DOCKER_TEST_LINK}--network ${DOCKER_TEST_TAG}_default -v ${ROBOT_RESULTS_PATH}:/tmp/:rw") {c ->
                //    sh "docker logs ${c.id}"
                //    //sh "python --version"
                //}

            }

            step(
              [
                $class               : 'RobotPublisher',
                outputPath           : "${ROBOT_RESULTS_PATH}",
                outputFileName       : "output.xml",
                reportFileName       : 'report.html',
                logFileName          : 'log.html',
                disableArchiveOutput : false,
                passThreshold        : 100.0,
                unstableThreshold    : 80.0,
                otherFiles           : "*.png,*.jpg",
              ]
            )

        }

        if (body) { body() }

    } // script

}
