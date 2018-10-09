#!/usr/bin/groovy

def call() {
    println("***")
    try {
      println(env.GIT_COMMIT)
      if (!env.GIT_COMMIT) {
          env.GIT_COMMIT = getCommitSha()
      }
    }
    catch(exc) {
        echo 'Error: There were errors in getCommitId. '+exc.toString()
        env.GIT_COMMIT = "TODO"
    }
    environment()
    return env.GIT_COMMIT
}
