#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/cleanStash.groovy`"

    vars.isCleaningStashEnabled = vars.get("isCleaningStashEnabled", false).toBoolean()

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

    if (vars.isCleaningStashEnabled == true || CLEAN_RUN == true) {
        stash name: "git", excludes: "**", allowEmpty: true
        stash name: "sources", excludes: "**", allowEmpty: true
        stash name: "sources-tools", excludes: "**", allowEmpty: true
        stash name: "maven-artifacts", excludes: "**", allowEmpty: true
        stash name: "app", excludes: "**", allowEmpty: true
        stash name: "classes", excludes: "**", allowEmpty: true
        stash name: "bw-outputs", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts-centos7", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts-centos6", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts-win", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts-centos6-debug", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts-centos7-debug", excludes: "**", allowEmpty: true
        stash name: "scons-artifacts-win-debug", excludes: "**", allowEmpty: true
    }

    if (body) { body() }

}
