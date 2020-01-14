#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

// withRegistry is buggy, because of wrong DOCKER_CONFIG, so until it gets fix, bellow is workaround
def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withRegistryWrapper.groovy`"

    vars = vars ?: [:]

    //def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.mobi").toLowerCase().trim()
    def DOCKER_REGISTRY_TMP = vars.get("DOCKER_REGISTRY_TMP", env.DOCKER_REGISTRY_TMP ?: "registry-tmp.nabla.mobi").toLowerCase().trim()
    //def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
    def DOCKER_REGISTRY_TMP_URL = vars.get("DOCKER_REGISTRY_TMP_URL", env.DOCKER_REGISTRY_TMP_URL ?: "https://${DOCKER_REGISTRY_TMP}").trim()
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins").trim()
    def JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins").trim()
    def DOCKER_CONFIG_DEFAULT = vars.get("DOCKER_CONFIG_DEFAULT", env.DOCKER_CONFIG_DEFAULT ?: "${JENKINS_USER_HOME}/.docker/").trim()
    def DOCKER_CLIENT_TIMEOUT = vars.get("DOCKER_CLIENT_TIMEOUT", env.DOCKER_CLIENT_TIMEOUT ?: "600").trim()

//sh '''
//echo "DOCKER_CONFIG BEFORE : $DOCKER_CONFIG"
//'''

    withEnv(["DOCKER_CONFIG=${DOCKER_CONFIG_DEFAULT}",
             "DOCKER_REGISTRY_TMP_URL=${DOCKER_REGISTRY_TMP_URL}",
             "DOCKER_CLIENT_TIMEOUT=${DOCKER_CLIENT_TIMEOUT}",
            ]) {

        sh 'echo DOCKER_CONFIG_DEFAULT : ${DOCKER_CONFIG_DEFAULT}'

        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: DOCKER_REGISTRY_CREDENTIAL, usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
            usr = DOCKER_USERNAME
            pswd = DOCKER_PASSWORD

            sh 'ls -lrta ${DOCKER_CONFIG} || true'

            // true added, because Client.Timeout exceeded while awaiting headers
            login = sh (
              script: 'docker logout && docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY_TMP_URL}',
              returnStatus: true
            )

            echo "LOGIN RETURN CODE : ${login}"
            if (login == 0) {
                echo "LOGIN SUCCESS"

                if (body) {
                    body()
                }

            } else {
                echo "LOGIN FAILURE"
                //currentBuild.result = 'FAILURE'
                // TODO withRegistry is buggy, because of wrong DOCKER_CONFIG
                docker.withRegistry("${DOCKER_REGISTRY_TMP_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {

                    if (body) {
                        body()
                    }

                } // withRegistry
            }

        } // withCredentials
    } // withEnv

}
