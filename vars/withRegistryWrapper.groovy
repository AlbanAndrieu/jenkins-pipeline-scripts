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

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "mgr.jenkins")
    def DOCKER_CONFIG_DEFAULT = vars.get("DOCKER_CONFIG_DEFAULT", env.DOCKER_CONFIG_DEFAULT ?: "/home/jenkins/.docker/")
    
    sh 'echo DOCKER_CONFIG_DEFAULT : ${DOCKER_CONFIG_DEFAULT}'

//sh '''
//echo "DOCKER_CONFIG BEFORE : $DOCKER_CONFIG"
//'''

    withEnv(["DOCKER_CONFIG=${DOCKER_CONFIG_DEFAULT}",
             "DOCKER_REGISTRY_URL=${DOCKER_REGISTRY_URL}",
            ]) {

//sh '''
//echo "DOCKER_CONFIG AFTER : $DOCKER_CONFIG"
//'''

        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: DOCKER_REGISTRY_CREDENTIAL, usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
            usr = DOCKER_USERNAME
            pswd = DOCKER_PASSWORD
            
            sh 'ls -lrta ${DOCKER_CONFIG} || true'

            // true added, because Client.Timeout exceeded while awaiting headers
            //sh 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY_URL} || true'
            login = sh (
              script: 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY_URL}',
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
                docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {
			    
                    if (body) {
                        body()
                    }
			    
                } // withRegistry                
            }            

        } // withCredentials
    } // withEnv

}
