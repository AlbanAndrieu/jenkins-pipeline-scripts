#!/usr/bin/groovy
import hudson.model.*

def withStageDeploy(Map vars) {
    stage("Deploy") {
        //when {
        //    expression { BRANCH_NAME ==~ /(release|master|develop)/ }
        //}
        //steps {
            script {
                vars.put("isScmEnabled", false)
                vars.put("isMavenEnabled", true)
                wrapInTEST(vars) {
                    withDeploy(vars) {
                        unstash 'maven-artifacts'
                    }
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

def withStageArchive(Map vars) {
    stage("Archive") {
        //when {
        //    expression { BRANCH_NAME ==~ /release|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
        //}
        //steps {
            script {
                vars.put("isScmEnabled", false)
                wrapInTEST(vars) {
            withArchive(vars) {

                        unstash 'scons-artifacts-centos7'
                        //unstash 'scons-artifacts-rhel7'
                        //unstash 'scons-artifacts-osx'
                        unstash 'app'

                    }
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

def withStageTag(Map vars) {
    stage("Git Tag") {
        //when {
        //    expression { BRANCH_NAME ==~ /(release|master|develop)/ }
        //}
        //steps {
            script {
                vars.put("isScmEnabled", true)
                vars.put("isCleaningEnabled", true)
                wrapInTEST(vars) {
            withTag(vars)
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

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    def parallelTasks = [:]

    parallelTasks['Archive Artifacts'] = {
        withStageArchive(vars.clone())
    }

    if (!DRY_RUN && isReleaseBranch()) {

        parallelTasks['Deploy'] = {
            withStageDeploy(vars.clone())
    }

    parallelTasks['Git Tag'] = {
            withStageTag(vars.clone())
        }

    }

    parallel parallelTasks

    if (body) { body() }

}
