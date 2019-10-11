#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/dockerComposeTest.groovy`"
    
    vars = vars ?: [:]

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    vars.DOCKER_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: "temp")
    vars.DOCKER_TEST_TAG = dockerTag(vars.DOCKER_TAG)
    vars.DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${vars.DOCKER_TEST_TAG}_test_1")
    vars.DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "-d --force-recreate test")
    vars.DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${vars.DOCKER_TEST_TAG}")

    vars.dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./docker/centos7/run/")
    vars.dockerDownFile = vars.get("dockerDownFile", env.dockerDownFile ?: "${vars.dockerFilePath}docker-compose-down.sh")
    vars.dockerUpFile = vars.get("dockerUpFile", env.dockerUpFile ?: "${vars.dockerFilePath}docker-compose-up.sh 2>&1 > docker-compose-up.log")

    vars.isFailReturnCode = vars.get("isFailReturnCode", env.isFailReturnCode ?: 1)
    vars.isUnstableReturnCode = vars.get("isUnstableReturnCode", env.isUnstableReturnCode ?: 250)

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

                    if (DEBUG_RUN) {
                        sh "docker images"
                        sh "docker volume ls"
                        sh "docker ps -a"
                    }

                    if (!DRY_RUN) {
                        def up = sh script: "${vars.dockerUpFile}", returnStatus: true
                        echo "UP RETURN CODE : ${up}"
                        if (up == 0) {
                            echo "TEST SUCCESS"
                        } else if (up == vars.isFailReturnCode) {
                            echo "TEST FAILURE"
                            currentBuild.result = 'FAILURE'
                            error 'There are errors staring containers'
                        } else if (up <= vars.isUnstableReturnCode) {
                            echo "TEST UNSTABLE"
                            currentBuild.result = 'UNSTABLE'
                        } else {
                            echo "TEST FAILURE"
                            //currentBuild.result = 'FAILURE'
                            error 'There are other errors'
                        }
                    }

                    if (body) { body() }

                }  // withRegistryWrapper

            } catch(exc) {
                dockerCheckHealth("test","healthy")
                def containerId = getContainerId(vars)
                //dockerCheckHealth("${vars.DOCKER_TEST_CONTAINER}","healthy")
                currentBuild.result = 'FAILURE'
                up = "FAIL" // make sure other exceptions are recorded as failure too
                echo 'Error: There were errors in compose tests. '+exc.toString()
            } finally {
                try {
                    sh "docker-compose -f ${vars.dockerFilePath}docker-compose.yml ${vars.DOCKER_COMPOSE_OPTIONS} ps"
                    dockerComposeLogs(vars)
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
