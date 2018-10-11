#!/usr/bin/groovy

def call(isDefaultBranch = false, gitDefaultBranchName = "develop") {

    def myBranches = null

    if (isDefaultBranch) {
       //echo 'Default branches managed by Jenkins'
       myBranches = scm.branches
    } else {
       myBranches = [[name: gitDefaultBranchName]]
    }

    return myBranches
}
