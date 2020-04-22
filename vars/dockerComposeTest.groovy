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

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "").toLowerCase().trim()
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins").trim()

    vars.DOCKER_TAG = vars.get("DOCKER_TEST_TAG", env.DOCKER_TEST_TAG ?: "temp").trim()
    vars.DOCKER_TEST_TAG = dockerTag(vars.DOCKER_TAG).trim()
    vars.DOCKER_TEST_CONTAINER = vars.get("DOCKER_TEST_CONTAINER", env.DOCKER_TEST_CONTAINER ?: "${vars.DOCKER_TEST_TAG}_test_1").trim()
    vars.DOCKER_COMPOSE_UP_OPTIONS = vars.get("DOCKER_COMPOSE_UP_OPTIONS", env.DOCKER_COMPOSE_UP_OPTIONS ?: "-d --force-recreate test").trim()
    vars.DOCKER_COMPOSE_OPTIONS = vars.get("DOCKER_COMPOSE_OPTIONS", env.DOCKER_COMPOSE_OPTIONS ?: "-p ${vars.DOCKER_TEST_TAG}").trim()

    vars.dockerFilePath = vars.get("dockerFilePath", "./docker/centos7/run/").trim()
    vars.dockerDownFile = vars.get("dockerDownFile", "${vars.dockerFilePath}docker-compose-down.sh").trim()
    vars.dockerUpFile = vars.get("dockerUpFile", "${vars.dockerFilePath}docker-compose-up.sh 2>&1 > docker-compose-up.log").trim()

    vars.isSuccessReturnCode = vars.get("isSuccessReturnCode", 0)
    vars.isFailReturnCode = vars.get("isFailReturnCode", 1)
    vars.isUnstableReturnCode = vars.get("isUnstableReturnCode", 250)
    vars.skipLogDump = vars.get("skipLogDump", false).toBoolean()
    vars.shellOutputFile = vars.get("shellOutputFile", "docker-compose.log").trim()

    script {

        tee("${vars.shellOutputFile}") {

            try {

                //docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {

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
                        if (up == vars.isSuccessReturnCode) {
                            echo "TEST SUCCESS"
                        } else if (up == vars.isFailReturnCode) {
                            echo "TEST FAILURE"
                            currentBuild.result = 'FAILURE'
                            error 'There are errors starting containers'
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

                //}  // withRegistry

            } catch(exc) {
                dockerCheckHealth("test","healthy")
                def containerId = getContainerId(vars)
                currentBuild.result = 'FAILURE'
                up = "FAIL" // make sure other exceptions are recorded as failure too
                echo 'Error: There were errors in compose tests. '+exc.toString()
            } finally {
                try {
                    if (! vars.skipLogDump) {
                        dockerComposeLogs(vars)
                    }
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
