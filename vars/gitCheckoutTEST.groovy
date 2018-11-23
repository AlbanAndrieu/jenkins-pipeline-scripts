#!/usr/bin/groovy
import java.*
import java.lang.*
import hudson.*
import hudson.model.*
import jenkins.model.Jenkins
import com.cloudbees.groovy.cps.NonCPS

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: false)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)

    if (DEBUG_RUN && !body) {
        echo 'No body specified'
    }

    def relativeTargetDir = vars.get("relativeTargetDir", "test")
    def isDefaultBranch = vars.get("isDefaultBranch", true).toBoolean()
    def isScmEnabled = vars.get("isScmEnabled", true).toBoolean()

    // TODO
    //def GIT_BRANCH_NAME = vars.get("GIT_BRANCH_NAME", "develop")

    script {

        checkout scm

        //gitCheckoutTESTRepo(relativeTargetDir: relativeTargetDir, isDefaultBranch: isDefaultBranch, isScmEnabled: isScmEnabled) {
        //
        //   dir ("test") {
        //
        //       getGitData()
        //
        //       if (body) { body() }
        //
        //   } // dir
        //
        //}

        // TODO
        //def GIT_BRANCH_NAME_BUILDMASTER = vars.get("GIT_BRANCH_NAME_BUILDMASTER", "develop")

        gitCheckoutBMRepo(relativeTargetDir: "bm")

        getGitData()

        if (body) { body() }

    } // script
}
