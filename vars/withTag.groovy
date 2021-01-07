#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withTag.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    vars.DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    vars.RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    vars.JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins")
    vars.STASH_CREDENTIALS = vars.get("STASH_CREDENTIALS", env.STASH_CREDENTIALS ?: "jenkins").trim()

    setBuildName()

    if (!vars.DRY_RUN && !vars.RELEASE) {

        if (isReleaseBranch()) {
            String TARGET_TAG = vars.get("TARGET_TAG", getSemVerReleasedVersion(vars) ?: "0.0.1").toUpperCase().trim()

            def tagName="${TARGET_TAG}_SUCCESSFULL"
            def message="Jenkins"
            def remote="origin"

            echo "Tag repo to git : git tag -a ${tagName} -m ${message}"
            sh "git config --global --list || true"

            // See https://support.cloudbees.com/hc/en-us/articles/360027646491-Pipeline-Equivalent-to-Git-Publisher
            withCredentials([usernamePassword(credentialsId: vars.STASH_CREDENTIALS, usernameVariable: 'stashLogin', passwordVariable: 'stashPass')]) {

              try {
                  sh """#!/bin/bash -l
                  which git;
                  git --version;
                  git config --local credential.helper "!f() { echo username=\\$stashLogin; echo password=\\$stashPass; }; f"
                  export GIT_SSH_COMMAND="ssh -oStrictHostKeyChecking=no"
                  git push --delete ${remote} ${tagName} || echo "Could not delete remote tag: does not exist or no access rights" || true;
                  git fetch --tags --prune > /dev/null 2>&1 || true;
                  git tag -a ${tagName} -m '${message}'; # create new tag
                  git push ${remote} ${tagName} --force || echo "Could not push tag: invalid name or no access rights";
                  """
              } catch(exc) {
                  echo 'Warning: There were errors while tagging. ' + exc.toString()
                  try {
                      sh "git config --global --list && ls -lrta ${vars.JENKINS_USER_HOME}/.gitconfig"
                  } catch(e) {
                      echo 'Warning: There were errors whith git config.'
                  }
              }
            } // withCredentials

        } // isReleaseBranch
    } // if DRY_RUN

    if (body) { body() }

}
