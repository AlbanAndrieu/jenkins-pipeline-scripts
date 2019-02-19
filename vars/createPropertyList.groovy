#!/usr/bin/groovy

def call() {
    def daysToKeep         = isReleaseBranch() ? '30' : '10'
    def numToKeep          = isReleaseBranch() ? '20' : '5'
    def artifactDaysToKeep = isReleaseBranch() ? '30' : '10'
    def artifactNumToKeep  = isReleaseBranch() ? '3'  : '1'
    def cronString         = isReleaseBranch() ? 'H H(3-7) * * 1-5' : ''
    def pollSCMString      = isReleaseBranch() ? 'H H(3-7) * * 1-5' : 'H/10 * * * *'

    def triggers           = isReleaseBranch() ? [snapshotDependencies(), cron(cronString)] : [cron(cronString)]

    def propertyList = [
        buildDiscarder(
            logRotator(
                daysToKeepStr:         daysToKeep,
                numToKeepStr:          numToKeep,
                artifactDaysToKeepStr: artifactDaysToKeep,
                artifactNumToKeepStr:  artifactNumToKeep
            )
        ), pipelineTriggers(triggers)
    ]
    return propertyList
}
