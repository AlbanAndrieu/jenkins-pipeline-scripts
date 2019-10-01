#!/usr/bin/env groovy

def call() {
    try {
      //println(env.GIT_COMMIT)
      if (!env.GIT_COMMIT?.trim()) {
          env.GIT_COMMIT = getCommitSha()
      }
      environment()
    }
    catch(exc) {
        echo 'Error: There were errors in getCommitId. '+exc.toString()
        env.GIT_COMMIT = "TODO"
    }
    return env.GIT_COMMIT
}
