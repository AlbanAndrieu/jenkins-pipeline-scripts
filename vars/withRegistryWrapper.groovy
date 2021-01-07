#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

// withRegistry is buggy, because it is not working inside docker image, bellow is workaround
def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withRegistryWrapper.groovy`"

    vars = vars ?: [:]

    String DOCKER_REGISTRY = vars.get("DOCKER_REGISTRY", env.DOCKER_REGISTRY ?: "registry.hub.docker.com").toLowerCase().trim()
    String DOCKER_REGISTRY_URL = vars.get("DOCKER_REGISTRY_URL", env.DOCKER_REGISTRY_URL ?: "https://${DOCKER_REGISTRY}").trim()
    String DOCKER_REGISTRY_CREDENTIAL = vars.get("DOCKER_REGISTRY_CREDENTIAL", env.DOCKER_REGISTRY_CREDENTIAL ?: "hub-docker-nabla").trim()

    String JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins").trim()

    String DOCKER_CONFIG_DEFAULT = vars.get("DOCKER_CONFIG_DEFAULT", env.DOCKER_CONFIG_DEFAULT ?: "${JENKINS_USER_HOME}/.docker").trim()

    String DOCKER_CLIENT_TIMEOUT = vars.get("DOCKER_CLIENT_TIMEOUT", env.DOCKER_CLIENT_TIMEOUT ?: "600").trim()

    vars.skipLogout = vars.get("skipLogout", true).toBoolean()
    vars.skipShellLogin = vars.get("skipShellLogin", false).toBoolean()
    vars.dockerRegistry = vars.get("dockerRegistry", vars.get("DOCKER_REGISTRY", "localhost")).trim()
    vars.dockerRegistryUrl = vars.get("dockerRegistryUrl", "https://${vars.dockerRegistry}").trim()
    vars.dockerRegistryCredentials = vars.get("dockerRegistryCredentials", vars.get("DOCKER_REGISTRY_CREDENTIAL", "jenkins")).trim()

    withEnv(["DOCKER_CONFIG=${DOCKER_CONFIG_DEFAULT}",
             "DOCKER_CLIENT_TIMEOUT=${DOCKER_CLIENT_TIMEOUT}",
            ]) {

        cleanDockerConfig(vars)

        if (!vars.skipLogout) {
            sh  "docker logout || true"
        }

        //docker.withRegistry needed for container.push("develop") from pushDockerImage.groovy
        docker.withRegistry(vars.dockerRegistryUrl, vars.dockerRegistryCredentials) {

            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: vars.dockerRegistryCredentials, usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
                usr = DOCKER_USERNAME
                pswd = DOCKER_PASSWORD

                if (!vars.skipShellLogin) {
                    timeout(time: 2, unit: 'MINUTES') {
                        retry(3) {
                            sh "docker login ${vars.dockerRegistryUrl} --username ${DOCKER_USERNAME} --password ${DOCKER_PASSWORD}"
                        }
                    } // timeout

                }

                if (body) {
                    body()
                }
            } // withCredentials

        } // withRegistry

    } // withEnv
}
