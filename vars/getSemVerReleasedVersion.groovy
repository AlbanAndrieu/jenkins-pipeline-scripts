#!/usr/bin/groovy

def call(body) {
    tokens = getReleasedVersion().tokenize('.')
    MAJOR = tokens[tokens.size()-4] // MAJOR
    MINOR = tokens[tokens.size()-3] // MINOR
    PATCH = tokens[tokens.size()-2]
    RELEASE_VERSION_SEMVER = MAJOR + "." + MINOR + "." + PATCH
    CUT = tokens[tokens.size()-1]
    echo "MAJOR: ${MAJOR} - MINOR : ${MINOR} - SEMVER : ${RELEASE_VERSION_SEMVER} - PATCH : ${PATCH} - CUT - ${CUT}"
    return "${RELEASE_VERSION_SEMVER}"
}
