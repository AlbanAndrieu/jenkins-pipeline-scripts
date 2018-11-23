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

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.misys.global.ad")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "mgr.jenkins")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "fusion-risk")

    def DOCKER_TAG = vars.get("DOCKER_RUNTIME_TAG", env.DOCKER_RUNTIME_TAG ?: "temp")
    def DOCKER_RUNTIME_TAG = dockerTag(DOCKER_TAG)
    def DOCKER_RUNTIME_NAME = vars.get("DOCKER_RUNTIME_NAME", env.DOCKER_RUNTIME_NAME ?: "test")
    def DOCKER_RUNTIME_IMG = vars.get("DOCKER_RUNTIME_IMG", env.DOCKER_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_ALMTEST_RUNTIME_NAME}:${DOCKER_ALMTEST_RUNTIME_TAG}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "/")

    script {

        wrapInTEST() {

            #unstash 'maven-artifacts'

            #sh "mkdir ${dockerFilePath}target/ || true"
            #sh "cp ./test/target/AlmondeTest.jar ${dockerFilePath}target/"
            #sh "cp ./test/conf/*.plist ${dockerFilePath}target/"

            sh(returnStdout: true, script: "echo ${DOCKER_RUNTIME_IMG} | cut -d'/' -f -1").trim()
            DOCKER_BUILD_ARGS = [""].join(" ")
            if (CLEAN_RUN) {
                DOCKER_BUILD_ARGS = ["--no-cache",
                                     "--pull",
                                     ].join(" ")
            }
            //DOCKER_BUILD_ARGS = [ "${DOCKER_BUILD_ARGS}"].join(" ")
		    
            docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {
                def container = docker.build("${DOCKER_RUNTIME_IMG}", "${DOCKER_BUILD_ARGS} -f ${dockerFilePath}Dockerfile ${dockerFilePath} ")
                //container.inside("--help") {
                //    sh 'java -version'
                //}
		    
                echo 'Test container'
                def image = docker.image("${DOCKER_RUNTIME_IMG}").withRun("", "--version") {c ->
                    if (body) {
                        body()
                    } else {
                        "Test : Program used to Test Computation" == sh(returnStdout: true, script: "docker logs ${c.id}").trim()
                    }
                }
		    
                //if (!DRY_RUN && isReleaseBranch() ) {
                if (!DRY_RUN ) {
                    echo "TODO : Push the container to the custom Registry"
                    //image.push("${env.BUILD_NUMBER}")
                    container.push()
                }
            }

            dockerFingerprintFrom dockerfile: "${dockerFilePath}Dockerfile", image: "${DOCKER_RUNTIME_IMG}"

        }  // wrapInTEST

    } // script

    return DOCKER_RUNTIME_TAG

}
