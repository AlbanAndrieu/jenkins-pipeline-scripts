#!/usr/bin/env groovy

def call() {
	try {
		if (!env.GIT_REVISION) {
			env.GIT_REVISION = getCommitRevision()
		} else {
			println(" GIT_REVISION : " + env.GIT_REVISION)
		}
	}
	catch(exc) {
		echo 'Error: There were errors in getRevision. '+exc.toString()
		env.GIT_REVISION = "TODO"
	}
	environment()
	return env.GIT_REVISION
}
