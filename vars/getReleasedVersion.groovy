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
    vars.releasedVersion = vars.get("releasedVersion", "1.0.0").trim()

    try {
        vars.releasedVersion = (readFile("${vars.pomFile}") =~ '<version>(.+)-SNAPSHOT</version>')[0][1]
    }
    catch(exc) {
        echo 'Error: There were errors in getReleasedVersion. '+exc.toString()
    }

    return vars.releasedVersion ?: "LATEST"
}
