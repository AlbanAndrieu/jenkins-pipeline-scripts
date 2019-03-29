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
    //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def DOCKER_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: "temp")
    def DOCKER_TEST_TAG = dockerTag(DOCKER_TAG)
    def DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${DOCKER_TEST_TAG}_frarc_1")
    def DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "-d --force-recreate test")
    def DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${env.DOCKER_TEST_TAG}")

    vars.dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")
    vars.dockerDownFile = vars.get("dockerDownFile", env.dockerDownFile ?: "${vars.dockerFilePath}docker-compose-down.sh")
    vars.dockerUpFile = vars.get("dockerUpFile", env.dockerUpFile ?: "${vars.dockerFilePath}docker-compose-up.sh")

    script {

        tee('docker-compose.log') {

            try {

                // TODO withRegistry is buggy, because of wrong DOCKER_CONFIG
                withRegistryWrapper() {

                    if (CLEAN_RUN) {
                        if (vars.dockerDownFile?.trim()) {
                          sh "${vars.dockerDownFile}"
                        }
                    }

                    sh "docker images"
                    sh "docker volume ls"
                    sh "docker ps -a"

                    if (!DRY_RUN) {
                        def up = sh script: "${vars.dockerUpFile}", returnStatus: true
                        echo "UP RETURN CODE : ${up}"
                        if (up == 0) {
                            echo "TEST SUCCESS"
                            //dockerCheckHealth("${DOCKER_TEST_CONTAINER}","healthy")
                        } else if (up == 1) {
                            echo "TEST FAILURE"
                            error 'There are errors in tests'
                            currentBuild.result = 'FAILURE'
                        } else {
                            echo "TEST UNSTABLE"
                            currentBuild.result = 'UNSTABLE'
                            //sh "exit 1" // this fails the stage
                        }
                    }

                    if (body) { body() }

                }  // withRegistryWrapper

            } catch(exc) {
                echo 'Error: There were errors in compose tests. '+exc.toString()
                error 'There are errors in compose tests'
                currentBuild.result = 'FAILURE'
                up = "FAIL" // make sure other exceptions are recorded as failure too
            } finally {
                try {
                    sh "docker-compose -f ${vars.dockerFilePath}docker-compose.yml ${DOCKER_COMPOSE_OPTIONS} ps -q"
                }
                catch(exc) {
                    echo 'Warn: There was a problem taking down the docker-compose. '+exc.toString()
                } finally {
                    if (vars.dockerDownFile?.trim()) {
                        sh "${vars.dockerDownFile}"
                    }
                }
            } // finally

        } // tee

    } // script

}
