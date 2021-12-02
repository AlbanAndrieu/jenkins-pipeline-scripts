#!/usr/bin/groovy

def call() {
  try {
    println(env.GIT_COMMIT_REV)
    if (!env.GIT_COMMIT_REV?.trim()) {
      try {
        sh 'git rev-list --count HEAD > .git/current-revision'
        env.GIT_COMMIT_REV = readFile('.git/current-revision').trim()
      }
            catch (exc) {
        echo 'Error: There were errors in getCommitRevision. ' + exc
        sh 'git --version'
            }
    }
  }
    catch (exc) {
    echo 'Error: There were errors in getCommitRevision. ' + exc
    env.GIT_COMMIT_REV = 'TODO'
    }
  environment()
  return env.GIT_COMMIT_REV
}
