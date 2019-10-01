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

    def skipPush = vars.get("skipPush", true).toBoolean()
    def skipMaven = vars.get("skipMaven", true).toBoolean()

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.mobi")
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}")
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "mgr.jenkins")
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla")

    def DOCKER_TAG = vars.get("DOCKER_TEST_RUNTIME_TAG", env.DOCKER_TEST_RUNTIME_TAG ?: "temp")
    def DOCKER_RUNTIME_TAG = dockerTag(DOCKER_TAG)
    def DOCKER_RUNTIME_NAME = vars.get("DOCKER_RUNTIME_NAME", env.DOCKER_RUNTIME_NAME ?: "test")
    def DOCKER_RUNTIME_IMG = vars.get("DOCKER_RUNTIME_IMG", env.DOCKER_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_RUNTIME_NAME}:${DOCKER_RUNTIME_TAG}")

    def dockerFilePath = vars.get("dockerFilePath", env.dockerFilePath ?: "./")
    def dockerTargetPath = vars.get("dockerTargetPath", env.dockerTargetPath ?: ".")

    script {

        if (!skipMaven) {
           unstash 'maven-artifacts'
        }

        sh(returnStdout: true, script: "echo ${DOCKER_RUNTIME_IMG} | cut -d'/' -f -1").trim()
        DOCKER_BUILD_ARGS = [""].join(" ")
        if (CLEAN_RUN) {
            DOCKER_BUILD_ARGS = ["--no-cache",
                                 "--pull",
                                 ].join(" ")
        }
        //DOCKER_BUILD_ARGS = [ "${DOCKER_BUILD_ARGS}"].join(" ")

        docker.withRegistry("${DOCKER_REGISTRY_URL}", "${DOCKER_REGISTRY_CREDENTIAL}") {
        // TODO withRegistry is buggy, because of wrong DOCKER_CONFIG
        //withRegistryWrapper(DOCKER_REGISTRY: DOCKER_REGISTRY) {

	        sh 'docker images'

            def container = docker.build("${DOCKER_RUNTIME_IMG}", "${DOCKER_BUILD_ARGS} -f ${dockerFilePath}Dockerfile ${dockerTargetPath} ")
            //container.inside("") {
            //    sh 'java -version'
            //}

            echo 'Test container'
            def image = docker.image("${DOCKER_RUNTIME_IMG}").withRun("", "--version") {c ->
                sh(returnStdout: true, script: "docker logs ${c.id}").trim()
                //"org.eclipse.jetty.runner.Runner: 9.4.9.v20180320" == sh(returnStdout: true, script: "docker logs ${c.id}").trim()
            }

            if (body) {
                body()
            }

            if (!DRY_RUN && !skipPush) {
                //image.push("${env.BUILD_NUMBER}")
                if ( BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
                    echo "Push image ${DOCKER_RUNTIME_IMG} to DTR"
                    container.push()
                } // if
            }
        } // withRegistryWrapper

        dockerFingerprintFrom dockerfile: "${dockerFilePath}Dockerfile", image: "${DOCKER_RUNTIME_IMG}"

    } // script

    return DOCKER_RUNTIME_TAG

}
