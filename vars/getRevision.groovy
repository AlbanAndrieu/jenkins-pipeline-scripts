#!/usr/bin/env groovy

def call() {
  try {
    if (!env.GIT_REVISION?.trim()) {
      env.GIT_REVISION = getCommitRevision()
        } else {
      println(' GIT_REVISION : ' + env.GIT_REVISION)
    }
    environment()
  }
    catch (exc) {
    echo 'Error: There were errors in getRevision. ' + exc
    env.GIT_REVISION = 'TODO'
    }
  return env.GIT_REVISION
}
