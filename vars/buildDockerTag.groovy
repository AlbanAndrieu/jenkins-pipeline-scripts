#!/usr/bin/groovy

def call(def branch, def commit, def dbms="sqlserver") {
    // create safe tag for docker image from given parameters
    def branchSafeName = branch.replaceAll("/", "-")
    def commitShortSHA1 = commit.take(7)
    if (dbms == null) {
        return "${branchSafeName}-${commitShortSHA1}"
    } else {
        return "${branchSafeName}-${commitShortSHA1}-${dbms}"
    }
}
