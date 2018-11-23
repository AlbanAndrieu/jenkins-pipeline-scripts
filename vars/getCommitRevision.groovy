#!/usr/bin/groovy

def call() {
    sh "git rev-list --count HEAD > .git/current-revision"
    return readFile(".git/current-revision").trim()
}
