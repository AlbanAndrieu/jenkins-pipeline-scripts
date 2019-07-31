#!/usr/bin/groovy

def call(def container="test_sample_1", def status="healthy") {
    def health = sh(returnStdout: true, script: "docker inspect ${container} --format='{{ .State.Health.Status }}'").trim()
    if (health.equalsIgnoreCase(status.trim())) {
        echo "CHECK SUCESSFULL : ${status}"
        return 0
    } else {
        echo "CHECK WRONG"
        tee("${container}-NOK-${status}.log") {
          sh "docker logs ${container}"
        }
        return 1
    }
}
