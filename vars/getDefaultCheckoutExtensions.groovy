#!/usr/bin/groovy

def call(isDefaultBranch = false, relativeTargetDir = "TODO") {

    def DEFAULT_EXTENTIONS = [
            //[$class: 'GitLFSPull'],
            //[$class: 'LocalBranch', localBranch: "${env.GIT_BRANCH_NAME}"],
            [$class: 'LocalBranch', localBranch: "**"],
            //[$class: 'WipeWorkspace'],
            [$class: 'CleanCheckout'],
            [$class: 'RelativeTargetDirectory', relativeTargetDir: relativeTargetDir],
            [$class: 'MessageExclusion', excludedMessage: '.*\\\\[maven-release-plugin\\\\].*'],
            [$class: 'IgnoreNotifyCommit'],
            [$class: 'CheckoutOption', timeout: 20],
            [$class: 'CleanBeforeCheckout'],
            //[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false, timeout: 120]
            //[$class: 'ChangelogToBranch', options: [compareRemote: 'origin', compareTarget: 'release/1.7.0']]
        ]

    def myExtensions = null

    if (isDefaultBranch) {
       //echo 'Default extensions managed by Jenkins'
       myExtensions = scm.extensions + DEFAULT_EXTENTIONS
    } else {
       myExtensions = DEFAULT_EXTENTIONS
    }

    return myExtensions
}
