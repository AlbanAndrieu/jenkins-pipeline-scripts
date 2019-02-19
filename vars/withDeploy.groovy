#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withDeploy.groovy`"

    vars = vars ?: [:]

    echo "Deploy maven artifacts to nexus"

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()

    if (!DRY_RUN) {
        //unstash 'maven-artifacts'

        if (body) { body() }

        withMavenDeployWrapper(vars)

    }

}
