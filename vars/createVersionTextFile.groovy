#!/usr/bin/groovy

def call(def description="TEST", def filename="TEST_VERSION.TXT", def releaseVersion="1.0.1") {

    echo "[JPL] Executing `vars/createVersionTextFile.groovy`"
    
    this.vars = [:]

    vars.releaseVersion = releaseVersion.trim()
    vars.filename = filename.trim()
    vars.description = description.trim()

    vars.build = currentBuild.number.toString()
    vars.commitSHA1 = getCommitId()

    sh """
        echo ${vars.description}: BUILD: ${vars.build} SHA1:${vars.commitSHA1} > "./${vars.filename}"
    """
}
