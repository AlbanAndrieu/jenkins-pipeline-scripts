#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getSemVerReleasedVersion.groovy`"

    vars = vars ?: [:]

    String RELEASE_VERSION_SEMVER = "0.0.1"

    tokens = getReleasedVersion(vars).tokenize('.')

    if (tokens.size() >= 4) {
        MAJOR = tokens[0] // MAJOR
        MINOR = tokens[1] // MINOR
        PATCH = tokens[2]
        RELEASE_VERSION_SEMVER = MAJOR + "." + MINOR + "." + PATCH
        CUT = tokens[3]
        echo "MAJOR: ${MAJOR} - MINOR : ${MINOR} - SEMVER : ${RELEASE_VERSION_SEMVER} - PATCH : ${PATCH} - CUT - ${CUT}"
    }

    return "${RELEASE_VERSION_SEMVER}".trim()
}
