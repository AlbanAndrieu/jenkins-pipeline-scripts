#!/usr/bin/groovy

def call() {
    sh "git rev-list --first-parent --count HEAD > .git/current-revision"
    return readFile(".git/current-revision").trim()
}
