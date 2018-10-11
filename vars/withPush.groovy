#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def withDeploy() {
    stage("Deploy") {
        //when {
        //    expression { BRANCH_NAME ==~ /(release|master|develop)/ }
        //}
        //steps {
            script {
                echo "Deploy maven artifacts to nexus"

                wrapInARC(isMavenEnabled: true) {

                    withMavenDeployWrapper()

                }
            } // script
        //} // steps
        //post {
        //    success {
        //        script {
        //            manager.createSummary("completed.gif").appendText("<h2>5-1 &#2690;</h2>", false)
        //            manager.addShortText("deployed")
        //            manager.createSummary("gear2.gif").appendText("<h2>Successfully deployed</h2>", false)
        //        } //script
        //    }
        //} // post
    } // stage deploy
    //stage("deploy") {
    //    echo "Deploying the app ${app}] on node [${node}]"
    //}
    //stage("test") {
    //    echo "Testing"
    //}
}

def withArchive(Map vars) {
    stage("Archive") {
        //when {
        //    expression { BRANCH_NAME ==~ /release|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
        //}
        //steps {
            script {
                wrapInTEST(isMavenEnabled: true) {
                    script {

                        //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: true)
                        def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
                        //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)

                        if (!DRY_RUN) {
                            unstash 'maven-artifacts'
                            //unstash 'scons-artifacts-centos7'
                            //unstash 'scons-artifacts-rhel7'
                            //unstash 'scons-artifacts-osx'

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
                    } // script
                } // wrapInTEST
            } // script
        //} // steps
        //post {
        //    success {
        //        script {
        //            manager.createSummary("completed.gif").appendText("<h2>5-2 &#2690;</h2>", false)
        //        } //script
        //    } // success
        //} // post
    } // stage Archive Artifacts
}

def withTag(Map vars) {
    stage("Git Tag") {
        //when {
        //    expression { BRANCH_NAME ==~ /(release|master|develop)/ }
        //}
        //steps {
            script {
                echo "Tag repo to git"

                wrapInTEST(isMavenEnabled: true) {

                    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: true)
                    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
                    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
                    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
                    def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)
                    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

                    //gitChangelog from: [type: 'REF', value: 'refs/remotes/origin/develop'], ignoreCommitsWithoutIssue: true, returnType: 'STRING', to: [type: 'REF', value: 'refs/tags/LATEST_SUCCESSFULL']
                    env.TARGET_PROJECT = sh(returnStdout: true, script: "echo ${env.JOB_NAME} | cut -d'/' -f -1").trim()

                    //utils = load "Jenkinsfile-vars"
                    setBuildName()
                    createVersionTextFile("Test ${env.BRANCH_NAME}","${env.TARGET_PROJECT}_VERSION.TXT")

                    if (!DRY_RUN && !RELEASE) {

                        //utils.manualPromotion()

                        if (isReleaseBranch()) {
                            de TARGET_TAG = getSemVerReleasedVersion() ?: "LATEST"
                            gitTagLocal("${TARGET_TAG}_SUCCESSFULL")
                            gitTagRemote("${TARGET_TAG}_SUCCESSFULL")
                        }
                    } // if DRY_RUN

                } // wrapInTEST
            } // script
        //} // steps
        //post {
        //    failure {
        //        script {
        //            //manager.addShortText("X - &#9760;")
        //            manager.addBadge("red.gif", "<p>&#x2620;</p>")
        //        } //script
        //    }
        //    success {
        //        script {
        //            manager.createSummary("completed.gif").appendText("<h2>5.3 &#2690;</h2>", false)
        //        } //script
        //    } // success
        //} // post
    } // stage deploy
}

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body) {

    vars = vars ?: [:]

    def artifacts = vars.get("artifacts", "**/target/*.war")

    def parallelTasks = [:]

    parallelTasks['Deploy'] = {
        withDeploy()
    }

    parallelTasks['Archive Artifacts'] = {
        withArchive(artifacts : artifacts)
    }

    parallelTasks['Git Tag'] = {
        withTag(vars)
    }

    parallel parallelTasks

    if (body) { body() }

}
