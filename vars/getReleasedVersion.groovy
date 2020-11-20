#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getReleasedVersion.groovy`"

    vars = vars ?: [:]

    vars.pomFile = vars.get("pomFile", "pom.xml").trim()

    if (!env.RELEASE_VERSION) {
        echo 'No env.RELEASE_VERSION specified'
    }

    vars.RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: "") // empty if not specified, so we will get it from pom.xml (default behavior)

    try {
        if (!vars.RELEASE_VERSION?.trim()) {
            echo "echo pomFile : ${vars.pomFile}"
            if (vars.pomFile != null && vars.pomFile.trim() != "") {
				// TODOD readMavenPom
                vars.RELEASE_VERSION = (readFile("${vars.pomFile}") =~ '<version>(.+)-SNAPSHOT</version>')[0][1]
            }
            echo "NEW RELEASE_VERSION : ${vars.RELEASE_VERSION}"
        } // if RELEASE_VERSION
    } catch(exc) {
        echo 'Warning: There were errors in getReleasedVersion : readFile. '+exc.toString()
        try {
          if (vars.pomFile != null && vars.pomFile.trim() != "") {
              vars.RELEASE_VERSION = readMavenPom(file: vars.pomFile).getVersion()
          }
        } catch(excc) {
		    echo 'Warning: There were errors in getReleasedVersion : readMavenPom. '+excc.toString()
		}
    } // catch

    echo "echo RELEASE_VERSION : ${vars.RELEASE_VERSION}"

    environment()

    return vars.RELEASE_VERSION ?: "0.0.1"
}
