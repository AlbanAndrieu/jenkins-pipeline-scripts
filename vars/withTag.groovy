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

    def JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins")

    setBuildName()

    if (!DRY_RUN && !RELEASE) {

        echo "Tag repo to git"

        if (isReleaseBranch()) {
            def TARGET_TAG = getSemVerReleasedVersion(vars) ?: "LATEST"

            def tagName="${TARGET_TAG}_SUCCESSFULL"
            def message="Jenkins"
            def remote="origin"

            try {
                sh """#!/bin/bash -l
                    which git;
                    git --version;
                    git push --delete ${remote} ${tagName} || echo "Could not delete remote tag: does not exist or no access rights" || true;
                    git fetch --tags --prune > /dev/null 2>&1 || true;
                    git tag -a ${tagName} -m '${message}'; # create new tag
                    git push ${remote} ${tagName} --force || echo "Could not push tag: invalid name or no access rights";
                """
            } catch(exc) {
                echo 'Warning: There were errors while tagging. ' + exc.toString()
                try {
                    sh "git config --global --list && ls -lrta ${JENKINS_USER_HOME}/.gitconfig"
                } catch(e) {
                    echo 'Warning: There were errors whith git config.'
                }
            }

        }
    } // if DRY_RUN

    if (body) { body() }

}
