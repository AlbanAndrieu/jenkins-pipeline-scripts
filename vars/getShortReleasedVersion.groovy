#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getShortReleasedVersion.groovy`"

    vars = vars ?: [:]

    String RELEASE_VERSION_SHORT = "0.0.1"

    tokens = getReleasedVersion(vars).tokenize('.')

    if (tokens.size() >= 4) {
        MAJOR = tokens[0] // MAJOR
        MINOR = tokens[1] // MINOR
        RELEASE_VERSION_SHORT = MAJOR + "." + MINOR
        PATCH = tokens[2]
        CUT = tokens[3]
        echo "MAJOR: ${MAJOR} - MINOR : ${MINOR} - SHORT : ${RELEASE_VERSION_SHORT} - PATCH : ${PATCH} - CUT - ${CUT}"
    }

    return "${RELEASE_VERSION_SHORT}".trim()
}
