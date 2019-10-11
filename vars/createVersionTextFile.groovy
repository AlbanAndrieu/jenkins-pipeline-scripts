#!/usr/bin/groovy

// Use  instead createManifest.groovy
@Deprecated
def call(def description="TEST", def filename="TEST_VERSION.TXT", def pomFile="pom.xml", def releaseVersion="1.0.1") {

    echo "[JPL] Executing `vars/createVersionTextFile.groovy`"

    this.vars = [:]
    
    vars.pomFile = pomFile.trim()
    vars.releaseVersion = getReleasedVersion(vars) ?: releaseVersion.trim()
    vars.filename = filename.trim()
    vars.description = description.trim()

    vars.build = currentBuild.number.toString()
    vars.commitSHA1 = getCommitId()

    vars.commitRevision = getCommitRevision()

    vars.fileContents = "${vars.description}:${vars.releaseVersion} BUILD:${vars.build}"

    if (vars.commitSHA1?.trim()) {
        vars.fileContents += " SHA1:${vars.commitSHA1}"
    }

    if (vars.commitRevision?.trim()) {
        vars.fileContents += " REV:${vars.commitRevision}"
    }

    sh """
        echo ${vars.fileContents} > "./${vars.filename}"
    """

    //TODO createManifest(vars)
    archiveArtifacts artifacts: "${vars.filename}", onlyIfSuccessful: false, allowEmptyArchive: true

    return vars.fileContents

}
