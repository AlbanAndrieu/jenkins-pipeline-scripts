#!/usr/bin/groovy

def call() {
    setBuildName(env.BRANCH_NAME)
}

def call(def gitBranchName, def desc = "") {
    currentBuild.displayName = "#" + currentBuild.number.toString() + " " + gitBranchName + " " + getCommitShortSHA1()
    def PULL_REQUEST_URL = env.getProperty('PULL_REQUEST_URL')
    def PULL_REQUEST_ID = env.getProperty("PULL_REQUEST_ID")
    if (desc != null && desc.trim() != "" ) {
        currentBuild.setDescription(desc)
    } else if (null != PULL_REQUEST_URL) {
        def description = "<a href='$PULL_REQUEST_URL'>PR #$PULL_REQUEST_ID</a>"
        currentBuild.setDescription(description)
    }
}
