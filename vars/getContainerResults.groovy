#!/usr/bin/groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/getContainerResults.groovy`'

  vars = vars ?: [:]

  vars.containerName = vars.get('containerName', 'frrobot').trim()
  vars.dockerResultPath = vars.get('dockerResultPath', "./${vars.containerName}-${env.GIT_COMMIT}-${env.BUILD_NUMBER}").trim()

  script {
    def containerId = getContainerId(vars)

    if (containerId?.trim()) {
      sh """
                docker cp ${containerId}:${vars.dockerResultPath} result || true
            """
        } else {
      sh """
                docker cp ${vars.containerName}:${vars.dockerResultPath} result || true # OLD way when container name is hard coded in docker-compose. To be removed after full migration
            """
    }

    if (body) { body() }
    } // script
}
