#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/sshPublisherWrapper.groovy`"

    vars = vars ?: [:]

    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

    vars.excludes = vars.get("excludes", "**/*Debug*.tar.gz")
    vars.remoteDirectory = vars.get("remoteDirectory", "TEST/LatestBuildsUntested/latest")
    vars.alwaysPublishFromMaster = vars.get("alwaysPublishFromMaster", false).toBoolean()
    vars.continueOnError = vars.get("continueOnError", true).toBoolean()
    vars.cleanRemote = vars.get("cleanRemote", false).toBoolean()
    vars.flatten = vars.get("flatten", true).toBoolean()
    vars.verbose = vars.get("verbose", false).toBoolean()
    vars.sourceFiles = vars.get("sourceFiles", "**/Latest-*.tar.gz,**/TEST-*.tar.gz")

    if ( isReleaseBranch() ) {
        if (DEBUG_RUN) {
            echo 'Publish artifacts'
        }
        
        if ( BRANCH_NAME ==~ /develop/ ) {
          vars.cleanRemote = true
        }

        if (body) { body() }

        try {
            sshPublisher alwaysPublishFromMaster: vars.alwaysPublishFromMaster, continueOnError: vars.continueOnError,
                publishers: [
                    sshPublisherDesc(
                        configName: 'albandri',
                        transfers: [
                            sshTransfer(cleanRemote: vars.cleanRemote,
                                excludes: vars.excludes,
                                execCommand: '',
                                execTimeout: 120000,
                                flatten: vars.flatten,
                                makeEmptyDirs: false,
                                noDefaultExcludes: false,
                                patternSeparator: '[, ]+',
                                remoteDirectory: vars.remoteDirectory,
                                remoteDirectorySDF: false,
                                removePrefix: '',
                                sourceFiles: vars.sourceFiles)
                        ],
                    usePromotionTimestamp: false,
                    useWorkspaceInPromotion: false,
                    verbose: vars.verbose)
                ]
        }
        catch(exc) {
            echo 'Error: There were errors running sshPublisher. '+exc.toString()
        }

        return true
    } else {
        return false
    }

}
