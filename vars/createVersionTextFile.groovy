#!/usr/bin/groovy

def call(def description="TEST", def filename="TEST_VERSION.TXT") {
    build = currentBuild.number.toString()
    commitSHA1 = getCommitId()
    sh """
        echo ${description}: BUILD: ${build} SHA1:${commitSHA1} > "./${filename}"
    """
}
