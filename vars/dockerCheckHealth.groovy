#!/usr/bin/groovy

def call(def container="test_sample_1", def status="healthy") {
	timeout(10) {
		// Wait until it is ready
		waitUntil {
			sleep time: 1, unit: 'MINUTES'
			status.toString() == sh(returnStdout: true,
				script: "docker inspect ${container} --format='{{ .State.Health.Status }}'").trim()
		} // waitUntil
	} // timeout
}
