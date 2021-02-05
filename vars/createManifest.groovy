#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

@NonCPS
def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/createManifest.groovy`"

    vars = vars ?: [:]

    vars.releaseVersion = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: "1.0.1")
    vars.projectName = vars.get("projectName", getGitRepoName(vars).toUpperCase() ?: "TEST").trim()
    vars.fileName = vars.get("fileName", "${vars.projectName}_VERSION.TXT").trim()
    vars.description = vars.get("description", "${vars.projectName}").trim()

    vars.build = currentBuild.number.toString()
    vars.commitSHA1 = getCommitId()

    vars.commitRevision = getCommitRevision()

    //def fileContents = readFile file: "${fileName}", encoding: "UTF-8"
    //fileContents = fileContents.replace("hello", "world")
    //echo fileContents

    vars.fileContents = "${vars.description}:${vars.releaseVersion} BUILD:${vars.build}"

    if (vars.commitSHA1?.trim()) {
        vars.fileContents += " SHA1:${vars.commitSHA1}"
    }

    if (vars.commitRevision?.trim()) {
        vars.fileContents += " REV:${vars.commitRevision}"
    }

    if (body) { body() }

    writeFile file: "${vars.fileName}", text: vars.fileContents, encoding: "UTF-8"

    return vars.fileContents

}
