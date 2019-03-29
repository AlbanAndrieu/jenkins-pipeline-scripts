#!/usr/bin/groovy

def call() {
    try {
        sh "git rev-list --count HEAD > .git/current-revision"
        return readFile(".git/current-revision").trim()
    }
    catch(exc) {
        echo 'Error: There were errors in getCommitRevision. '+exc.toString()
        sh "git --version"
    }
}
