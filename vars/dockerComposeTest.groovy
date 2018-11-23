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
    //def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def DOCKER_TEST_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: buildDockerTag("${env.BRANCH_NAME}", "${env.GIT_COMMIT}").toLowerCase())
    def DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${DOCKER_TEST_TAG}_frarc_1")
    def DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "-d --force-recreate test")
    def DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${env.DOCKER_TEST_TAG}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")

    script {

        try {

            if (CLEAN_RUN) {
                sh "${dockerFilePath}docker-compose-down.sh"
            }

            if (!DRY_RUN) {
                def up = sh script: "${dockerFilePath}docker-compose-up.sh", returnStatus: true
                echo "UP RETURN CODE : ${up}"
                if (up == 0) {
                    echo "TEST SUCCESS"
                    //dockerCheckHealth("${DOCKER_TEST_CONTAINER}","healthy")

                    currentBuild.result = 'SUCCESS'
                } else if (up == 1) {
                    echo "TEST FAILURE"
                    currentBuild.result = 'FAILURE'
                } else {
                    echo "TEST UNSTABLE"
                    currentBuild.result = 'UNSTABLE'
                }
            }

			if (body) { body() }

        } catch(exc) {
            echo 'Error: There were errors in tests. '+exc.toString()
            error 'There are errors in tests'
            currentBuild.result = 'FAILURE'
        } finally {
            try {
                sh "docker-compose -f ${dockerFilePath}docker-compose.yml ${DOCKER_COMPOSE_OPTIONS} ps -q"
            }
            catch(exc) {
                echo 'Warn: There was a problem taking down the docker-compose network. '+exc.toString()
                //currentBuild.result = 'ABORTED'
            } finally {
                sh "${dockerFilePath}docker-compose-down.sh"
            }
        }

    } // script

}
