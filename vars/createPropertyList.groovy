#!/usr/bin/groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/createPropertyList.groovy`"

    vars = vars ?: [:]
    
    vars.daysToKeep = vars.get("daysToKeep", isReleaseBranch() ? '30' : '10').trim()
    vars.numToKeep = vars.get("numToKeep", isReleaseBranch() ? '20' : '5').trim()

    vars.artifactDaysToKeep = vars.get("artifactDaysToKeep", isReleaseBranch() ? '30' : '10').trim()
    vars.artifactNumToKeep = vars.get("artifactNumToKeep", isReleaseBranch() ? '5'  : '3').trim()

    vars.cronString = vars.get("cronString", isReleaseBranch() ? 'H H(3-7) * * 1-5' : '').trim()
    vars.pollSCMString = vars.get("pollSCMString", isReleaseBranch() ? 'H H(3-7) * * 1-5' : 'H/10 * * * *').trim()

    def triggers = isReleaseBranch() ? [snapshotDependencies(), cron(vars.cronString)] : [cron(vars.cronString)]

    def propertyList = [
        buildDiscarder(
            logRotator(
                daysToKeepStr:         vars.daysToKeep,
                numToKeepStr:          vars.numToKeep,
                artifactDaysToKeepStr: vars.artifactDaysToKeep,
                artifactNumToKeepStr:  vars.artifactNumToKeep
            )
        ), pipelineTriggers(triggers)
    ]

    if (body) { body() }

    return propertyList
}
