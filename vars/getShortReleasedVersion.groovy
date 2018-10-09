#!/usr/bin/groovy

def call(body) {
    tokens = getReleasedVersion().tokenize('.')
    MAJOR = tokens[tokens.size()-4] // MAJOR
    MINOR = tokens[tokens.size()-3] // MINOR
    RELEASE_VERSION_SHORT = MAJOR + "." + MINOR
    PATCH = tokens[tokens.size()-2]
    CUT = tokens[tokens.size()-1]
    echo "MAJOR: ${MAJOR} - MINOR : ${MINOR} - SHORT : ${RELEASE_VERSION_SHORT} - PATCH : ${PATCH} - CUT - ${CUT}"
    return "${RELEASE_VERSION_SHORT}"
}
