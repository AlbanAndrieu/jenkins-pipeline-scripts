#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getSemVerReleasedVersion.groovy`"

    vars = vars ?: [:]

    tokens = getReleasedVersion(vars).tokenize('.')
    MAJOR = tokens[tokens.size()-4] // MAJOR
    MINOR = tokens[tokens.size()-3] // MINOR
    PATCH = tokens[tokens.size()-2]
    RELEASE_VERSION_SEMVER = MAJOR + "." + MINOR + "." + PATCH
    CUT = tokens[tokens.size()-1]
    echo "MAJOR: ${MAJOR} - MINOR : ${MINOR} - SEMVER : ${RELEASE_VERSION_SEMVER} - PATCH : ${PATCH} - CUT - ${CUT}"

    return "${RELEASE_VERSION_SEMVER}".trim()
}
