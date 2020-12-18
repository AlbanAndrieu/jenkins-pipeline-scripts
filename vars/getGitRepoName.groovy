#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getGitRepoName.groovy`"

    vars = vars ?: [:]

    try {
      if (!env.GIT_REPO_NAME?.trim()) {
        // See https://stackoverflow.com/questions/25088034/use-git-repo-name-as-env-variable-in-jenkins-job
        if (env.GIT_URL != null && env.GIT_URL.trim() != "" ) {
          env.GIT_REPO_NAME = env.GIT_URL.replaceFirst(/^.*\/([^\/]+?).git$/, '$1')
        } else {
          // See https://stackoverflow.com/questions/15715825/how-do-you-get-the-git-repositorys-name-in-some-git-repository
          env.GIT_REPO_NAME = sh(returnStdout: true, script: 'basename `git rev-parse --show-toplevel`').trim()
          echo "BASE GIT_REPO_NAME : ${env.GIT_REPO_NAME}"
          env.GIT_REPO_NAME = env.GIT_REPO_NAME.split("_")[0]
          //if (isUnix()) {
          //  env.GIT_REPO_NAME = sh(returnStdout: true, script: 'basename `git rev-parse --show-toplevel`').trim()
          //} else {
          //  env.GIT_REPO_NAME = bat(returnStdout: true, script: 'basename `git rev-parse --show-toplevel').trim()
          //}
        }
        echo "NEW GIT_REPO_NAME : ${env.GIT_REPO_NAME}"
        environment()
      }
    } catch(exc) {
        echo 'Error: There were errors in getGitRepoName. '+exc.toString()
        String JOB_NAME = env.JOB_NAME.replaceFirst(env.JOB_BASE_NAME,"").trim()
        env.GIT_REPO_NAME = JOB_NAME.split("/")[1]
    }
    echo "GIT_REPO_NAME : ${env.GIT_REPO_NAME}"
    return env.GIT_REPO_NAME.toLowerCase()
}
