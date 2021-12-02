#!/usr/bin/groovy

def call() {
  return getCommitId().take(7)
}
