#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/sshPublisherWrapper.groovy`"

    vars = vars ?: [:]

    def excludes = vars.get("excludes", "**/*Debug*.tar.gz")
    def remoteDirectory = vars.get("remoteDirectory", "ARC/lastSuccessfulBuild/${INSTALLER_PATH}")
    def sourceFiles = vars.get("sourceFiles", "**/Latest-*.tar.gz,**/TEST-*.tar.gz")

	sshPublisher continueOnError: true,
		publishers: [
			sshPublisherDesc(
				configName: 'ssh-server-1',
				transfers: [
					sshTransfer(cleanRemote: false,
					excludes: excludes,
					execCommand: '',
					execTimeout: 120000,
					flatten: true,
					makeEmptyDirs: false,
					noDefaultExcludes: false,
					patternSeparator: '[, ]+',
					remoteDirectory: remoteDirectory,
					remoteDirectorySDF: false,
					removePrefix: '',
					sourceFiles: sourceFiles)
				],
			usePromotionTimestamp: false,
			useWorkspaceInPromotion: false,
			verbose: true)
		]

    if (body) { body() }

}
