#!/usr/bin/groovy

def call() {
    try {
      println(env.GIT_REVISION)
      if (!env.GIT_REVISION) {
          env.GIT_REVISION = getCommitRevision()
      }
    }
    catch(exc) {
        echo 'Error: There were errors in getRevision. '+exc.toString()
        env.GIT_REVISION = "TODO"
    }
    environment()
    return env.GIT_REVISION
}
