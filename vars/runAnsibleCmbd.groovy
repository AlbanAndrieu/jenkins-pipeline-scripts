#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(Map vars, Closure body=null) {

	echo "[JPL] Executing `vars/runAnsiblCmbd.groovy`"

	vars = vars ?: [:]

	vars.shell = vars.get("shell", "./scripts/run-ansible-cmbd.sh")

	call(vars.shell )

	if (body) {
		body()
	}
}

def call(String shell) {

	if (shell != null && shell.trim() != "" ) {

		try {
			tee("ansible-cmdb.log") {

				configFileProvider([
					configFile(fileId: 'vault.passwd',  targetLocation: 'vault.passwd', variable: '_')
				]) {

					build = sh (
					script: shell,
					returnStatus: true
					)

					echo "BUILD RETURN CODE : ${build}"
					if (build == 0) {
						echo "CMBD SUCCESS"
					} else {
						echo "CMBD UNSTABLE"
						currentBuild.result = 'UNSTABLE'
						error 'There are errors in ansible'
						//sh "exit 1" // this fails the stage
					}


					publishHTML([
						allowMissing: false,
						alwaysLinkToLastBuild: false,
						keepAll: true,
						reportDir: "./",
						reportFiles: 'overview.html',
						//includes: '**/target/*',
						includes: 'overview.html',
						reportName: 'Ansible CMDB Report',
						reportTitles: "Ansible CMDB Report Index"
					])

					junit testResults: 'ansible-lint.xml', healthScaleFactor: 2.0, allowEmptyResults: true, keepLongStdio: true, testDataPublishers: [
						[$class: 'ClaimTestDataPublisher']
					]
				} // configFileProvider

			} // tee
		} catch (e) {			
			currentBuild.result = 'FAILURE'
			build = "FAIL" // make sure other exceptions are recorded as failure too
			throw e			
		} finally {
		    archiveArtifacts artifacts: "overview.html, ansible-lint.*, ansible-cmdb.log", onlyIfSuccessful: false, allowEmptyArchive: true
		}
		
	} // if
}
