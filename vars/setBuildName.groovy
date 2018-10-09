#!/usr/bin/groovy

def call() {
    setBuildName(env.BRANCH_NAME)
}

def call(def gitBranchName) {
    currentBuild.displayName = "#" + currentBuild.number.toString() + " " + gitBranchName + " " + getCommitShortSHA1()
    def PULL_REQUEST_URL = env.getProperty('PULL_REQUEST_URL')
    def PULL_REQUEST_ID = env.getProperty("PULL_REQUEST_ID")
    if (null != PULL_REQUEST_URL) {
        def description = "<a href='$PULL_REQUEST_URL'>PR #$PULL_REQUEST_ID</a>"
        currentBuild.setDescription(description)
    }
}
