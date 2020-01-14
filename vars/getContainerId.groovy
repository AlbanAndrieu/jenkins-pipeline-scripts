#!/usr/bin/groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getContainerId.groovy`"

    vars = vars ?: [:]

    script {

        if (vars.DOCKER_TEST_CONTAINER?.trim()) {
            // docker inspect cb714085fb0d --format='{{ .State.Status }}'
            def containerId = sh(returnStdout: true, script: "docker ps -q -a -f 'name=${vars.DOCKER_TEST_CONTAINER}'").trim()
            echo "Container Id: ${containerId}"

            if (body) { body() }

            return containerId
        }

    } // script

}
