#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withTag.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    //gitChangelog from: [type: 'REF', value: 'refs/remotes/origin/develop'], ignoreCommitsWithoutIssue: true, returnType: 'STRING', to: [type: 'REF', value: 'refs/tags/LATEST_SUCCESSFULL']
    TARGET_PROJECT = sh(returnStdout: true, script: "echo ${env.JOB_NAME} | cut -d'/' -f 2").trim().toUpperCase()

    setBuildName()
    createVersionTextFile("${TARGET_PROJECT} ${env.BRANCH_NAME}","${TARGET_PROJECT}_VERSION.TXT")

    if (!DRY_RUN && !RELEASE) {

        echo "Tag repo to git"

        //utils.manualPromotion()

        if (isReleaseBranch()) {
            def TARGET_TAG = getSemVerReleasedVersion() ?: "LATEST"
            gitTagLocal("${TARGET_TAG}_SUCCESSFULL")
            gitTagRemote("${TARGET_TAG}_SUCCESSFULL")
        }
    } // if DRY_RUN

    if (body) { body() }

}
