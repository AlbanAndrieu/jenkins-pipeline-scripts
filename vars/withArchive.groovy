#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: false)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)

    if (!DRY_RUN) {
        unstash 'maven-artifacts'

        if (body) { body() }

        def artifacts = vars.get("artifacts", ['*_VERSION.TXT', '**/target/*.jar'].join(', '))

        echo "artifacts : ${artifacts}"

        archiveArtifacts artifacts: "${artifacts}", excludes: null, fingerprint: true, onlyIfSuccessful: true

        step([
            $class: 'LogParserPublisher',
            parsingRulesPath:
            '/jenkins/deploy-log_parsing_rules',
            failBuildOnError: false,
            unstableOnWarning: false,
            useProjectRule: false
            ])

        step([
            $class: "AnalysisPublisher",
            canComputeNew: false,
            checkStyleActivated: false,
            defaultEncoding: '',
            dryActivated: false,
            findBugsActivated: false,
            healthy: '',
            opentasksActivated: false,
            pmdActivated: false,
            unHealthy: ''
            ])
    } // if

}
