#!/usr/bin/groovy
import java.*
import hudson.*
import hudson.model.*
import jenkins.model.*
import com.cloudbees.groovy.cps.NonCPS

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def preconfigureExecutableJob(def nodeName, Closure body) {

    if (!body) {
        error 'no body specified, mandatory'
    }

    return {
        node(nodeName) {
            stage(nodeName) {
                checkout scm
                //sh "${env.WORKSPACE}/Scripts/release/step-0-2-run-workspace-cleaning.sh"
                //dockerCleaning()
                if (body) { body() }
            }
        }
    }
}

def call(Map vars, Closure body) {

    echo "[JPL] Executing `vars/withBuildNodesWrapper.groovy`"

    vars = vars ?: [:]

    if (!body) {
        error 'no body specified, mandatory'
    }

    vars.nodeLabel = vars.get("nodeLabel", [
      "docker-perf",
      "docker-inside",
      "java-new",
      "javascript",
      "docker32G",
      "sun4sol-u6",
      "x86sol-u6"]).trim()

    def nodeNames = getNodesMatchingLabels(vars.nodeLabel)
    def executableJobs = [:]
    for (nodeName in nodeNames) {
      executableJobs[nodeName] = preconfigureExecutableJob(nodeName, body)
    }

    //timeout(time: 120, unit: 'MINUTES') {
      parallel executableJobs
    //}

    if (body) { body() }

}
