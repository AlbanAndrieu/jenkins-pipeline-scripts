#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withSonarQubeTEST.groovy`"

    vars = vars ?: [:]

    vars.repository = vars.get("repository", "test")
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", false).toBoolean()

    wrapInTEST(vars) {

        withSonarQubeWrapper(vars, body)
    }

}
