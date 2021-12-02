#!/usr/bin/groovy

def call() {
  sh 'git rev-parse HEAD > .git/current-commit'
  return readFile('.git/current-commit').trim()
}
