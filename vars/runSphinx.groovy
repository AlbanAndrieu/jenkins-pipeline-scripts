#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(Map vars, Closure body=null) {

	echo "[JPL] Executing `vars/runSphinx.groovy`"

	vars = vars ?: [:]

	vars.shell = vars.get("shell", "./build.sh")
	vars.targetDirectory = vars.get("targetDirectory", "./todo")

	call(vars.shell, vars.targetDirectory)

	if (body) {
		body()
	}
}

def call(String shell, String targetDirectory) {

	if (shell != null && shell.trim() != "" ) {

		try {
			tee("sphinx.log") {

				dir("docs") {

					build = sh (
							script: shell,
							returnStatus: true
							)

					echo "BUILD RETURN CODE : ${build}"
					if (build == 0) {
						echo "SPHINX SUCCESS"
					} else {
						echo "SPHINX UNSTABLE"						
						currentBuild.result = 'UNSTABLE'
						error 'There are errors in sphinx'
						//sh "exit 1" // this fails the stage
					}

					publishHTML([
						allowMissing: false,
						alwaysLinkToLastBuild: false,
						keepAll: true,
						reportDir: "./_build",
						reportFiles: 'index.html',
						includes: '**/*',
						reportName: 'Sphinx Docs',
						reportTitles: "Sphinx Docs Index"
					])
					
					if (isReleaseBranch()) {
						// Initially, we will want to publish only one version,
						// i.e. the latest one from develop branch.
						dir("_build") {
							rsync([
								source: "*",
								destination: "jenkins@FR1CSLFRBM0059:/kgr/release/docs/" + targetDirectory,
								credentialsId: "jenkins_unix_slaves"
							])
						}
					}
				} // dir docs

			} // tee
						
		} catch (e) {
			currentBuild.result = 'FAILURE'
			build = "FAIL" // make sure other exceptions are recorded as failure too
			throw e
		} finally {
		    archiveArtifacts artifacts: "sphinx.log", onlyIfSuccessful: false, allowEmptyArchive: true
		}
	} // if 

}
