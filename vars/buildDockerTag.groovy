#!/usr/bin/groovy

def call(def branch, def commit = "", def dbms = "") {
    // create safe tag for docker image from given parameters
    def branchSafeName = branch.replaceAll("/", "-")

    def DOCKER_TAG = "${branchSafeName}"

    if (commit != null && commit.trim() != "" ) {
    def commitShortSHA1 = commit.take(7)
        if (dbms == null || dbms.trim() == "" ) {
            DOCKER_TAG += "-${commitShortSHA1}"
    } else {
            DOCKER_TAG += "-${commitShortSHA1}-${dbms}"
    }
    }

    return DOCKER_TAG.toLowerCase()

}
