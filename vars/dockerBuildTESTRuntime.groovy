#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/dockerBuildTESTRuntime.groovy`"

    vars = vars ?: [:]

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    //def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    vars.isPushEnabled = vars.get("isPushEnabled", false).toBoolean()
    vars.skipMaven = vars.get("skipMaven", true).toBoolean()

    def DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.nabla.mobi").toLowerCase().trim()
    def DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
    def DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "jenkins").trim()
    def DOCKER_ORGANISATION = vars.get("DOCKER_ORGANISATION", env.DOCKER_ORGANISATION ?: "nabla").trim()

    def DOCKER_TAG = vars.get("DOCKER_TEST_RUNTIME_TAG", env.DOCKER_TEST_RUNTIME_TAG ?: "temp")
    def DOCKER_RUNTIME_TAG = dockerTag(DOCKER_TAG)
    def DOCKER_RUNTIME_NAME = vars.get("DOCKER_RUNTIME_NAME", env.DOCKER_RUNTIME_NAME ?: "test").trim()
    def DOCKER_RUNTIME_IMG = vars.get("DOCKER_RUNTIME_IMG", env.DOCKER_RUNTIME_IMG ?: "${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_RUNTIME_NAME}:${DOCKER_RUNTIME_TAG}")

    vars.dockerFilePath = vars.get("dockerFilePath", "./").trim()
    vars.dockerFileName = vars.get("dockerFileName", "Dockerfile").trim()
    vars.dockerTargetPath = vars.get("dockerTargetPath", vars.get("dockerFilePath", "./")).trim()

    script {

        if (!vars.skipMaven) {
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

        //docker.withRegistry(DOCKER_REGISTRY_HUB_URL, DOCKER_REGISTRY_HUB_CREDENTIAL) {

            //sh 'docker images'
            dockerHadoLint(vars)

            def container = docker.build("${DOCKER_RUNTIME_IMG}", "${DOCKER_BUILD_ARGS} -f ${vars.dockerFilePath}${vars.dockerFileName} ${vars.dockerTargetPath} ")
            //container.inside("") {
            //    sh 'java -version'
            //}

            echo 'Test container'
            def image = docker.image("${DOCKER_RUNTIME_IMG}").withRun("", "--version") {c ->
                sh(returnStdout: true, script: """#!/bin/bash -l
                docker logs ${c.id}""").trim()
                //"org.eclipse.jetty.runner.Runner: 9.4.9.v20180320" == sh(returnStdout: true, script: "docker logs ${c.id}").trim()
            }

            if (body) {
                body()
            }

            if ((!DRY_RUN && isReleaseBranch()) && vars.isPushEnabled ) {
                echo "Push the container to the custom Registry : ${DOCKER_RUNTIME_IMG}"
                //container.push("${env.BUILD_NUMBER}")
                container.push()
            }
        //} // withRegistry

       // dockerFingerprintFrom dockerfile: "${vars.dockerFilePath}${vars.dockerFileName}", image: "${DOCKER_RUNTIME_IMG}"

    } // script

    return DOCKER_RUNTIME_TAG
}
