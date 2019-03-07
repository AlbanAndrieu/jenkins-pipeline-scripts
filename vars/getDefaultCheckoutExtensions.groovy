#!/usr/bin/groovy

def call(isDefaultBranch = false, relativeTargetDir = "", timeout = 20, isCleaningEnabled = true) {

    def DEFAULT_EXTENTIONS = [
            //[$class: 'LocalBranch', localBranch: "**"],
            [$class: 'LocalBranch', localBranch: "${scm.branches[0]}"],
            [$class: 'RelativeTargetDirectory', relativeTargetDir: relativeTargetDir],
            [$class: 'MessageExclusion', excludedMessage: '.*\\\\[maven-release-plugin\\\\].*'],
            [$class: 'IgnoreNotifyCommit'],
            [$class: 'CheckoutOption', timeout: timeout],
            //[$class: 'ChangelogToBranch', options: [compareRemote: 'origin', compareTarget: 'release/1.7.0']]
        ]

    // Saving time and disk space when you just want to access the latest version of a repository.
    def DEFAULT_CLONE_OPTIONS_EXTENTIONS = [
            [$class: 'CloneOption', depth: 0, noTags: true, reference: '/var/lib/gitcache/test.git', shallow: true, honorRefspec: true, timeout: timeout]
            //[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false, timeout: timeout]
        ]

    def DEFAULT_CLEAN_OPTIONS_EXTENTIONS = [
            //[$class: 'WipeWorkspace'],
            //[$class: 'CleanCheckout'],
            [$class: 'CleanBeforeCheckout'],
        ]

    def myExtensions = null

    if (isDefaultBranch && isCleaningEnabled) {
       echo 'Default extensions managed by Jenkins'
       myExtensions = scm.extensions + DEFAULT_EXTENTIONS + DEFAULT_CLONE_OPTIONS_EXTENTIONS
    } else {
       myExtensions = DEFAULT_EXTENTIONS
       if (isCleaningEnabled) {
            echo 'Adding cleaning to extensions'
            myExtensions += DEFAULT_CLEAN_OPTIONS_EXTENTIONS
       }
    }

    return myExtensions
}
