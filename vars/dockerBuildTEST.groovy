#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: true)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    //def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "https://github.com/")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "nabla")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla")

    def DOCKER_BUILD_TAG = vars.get("DOCKER_BUILD_TAG", env.DOCKER_BUILD_TAG ?: "latest")
    //if (env.BRANCH_NAME ==~ /release|master|develop/ ) {
    //    DOCKER_BUILD_TAG="${env.BUILD_ID}" or remove after push to registry
    //}
    def DOCKER_BUILD_NAME = vars.get("DOCKER_BUILD_NAME", env.DOCKER_BUILD_NAME ?: "jenkins-slave-nabla-centos7")
    def DOCKER_BUILD_IMG = vars.get("DOCKER_BUILD_IMG", env.DOCKER_BUILD_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_BUILD_NAME}:${DOCKER_BUILD_TAG}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "")

    script {

        gitCheckoutTEST(isScmEnabled: true) {

            //sh(returnStdout: true, script: "echo ${DOCKER_BUILD_IMG} | cut -d'/' -f -1").trim()
            DOCKER_BUILD_ARGS = [""].join(" ")
            if (CLEAN_RUN) {
                DOCKER_BUILD_ARGS = ["--no-cache",
                                     "--pull",
                                     ].join(" ")
            }
            docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {
                def container = docker.build("${DOCKER_BUILD_IMG}", "${DOCKER_BUILD_ARGS} -f ${dockerFilePath}Dockerfile . ")
                container.inside {
                    sh 'echo test'
                    if (body) { body() }
                }
                    if (!DRY_RUN && isReleaseBranch() ) {
                        echo "TODO : Push the container to the custom Registry"
                        //image.push("${env.BUILD_NUMBER}")
                        //image.push('latest')
                }
            }
		    
            dockerFingerprintFrom dockerfile: "${dockerFilePath}Dockerfile", image: "${DOCKER_BUILD_IMG}"

        }  // gitCheckoutTEST

    } // script

    return DOCKER_BUILD_TAG
}
