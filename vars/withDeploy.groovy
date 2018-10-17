#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    echo "Deploy maven artifacts to nexus"

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)

    if (!DRY_RUN) {
        unstash 'maven-artifacts'

        withMavenDeployWrapper(vars)

        if (body) { body() }

    }

}
