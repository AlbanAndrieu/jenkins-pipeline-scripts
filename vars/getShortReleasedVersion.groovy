#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getShortReleasedVersion.groovy`"

    vars = vars ?: [:]

    tokens = getReleasedVersion(vars).tokenize('.')
    MAJOR = tokens[tokens.size()-4] // MAJOR
    MINOR = tokens[tokens.size()-3] // MINOR
    RELEASE_VERSION_SHORT = MAJOR + "." + MINOR
    PATCH = tokens[tokens.size()-2]
    CUT = tokens[tokens.size()-1]
    echo "MAJOR: ${MAJOR} - MINOR : ${MINOR} - SHORT : ${RELEASE_VERSION_SHORT} - PATCH : ${PATCH} - CUT - ${CUT}"
    return "${RELEASE_VERSION_SHORT}"
}
