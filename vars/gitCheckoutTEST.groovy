#!/usr/bin/groovy
import java.*
import hudson.*
import hudson.model.*
import jenkins.model.*
import com.cloudbees.groovy.cps.NonCPS

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

    if (DEBUG_RUN && !body) {
        echo 'No body specified'
    }

    vars.relativeTargetDir = vars.get("relativeTargetDir", "test")
    vars.isDefaultBranch = vars.get("isDefaultBranch", true).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", true).toBoolean()
    vars.isScmEnabled = vars.get("isScmEnabled", true).toBoolean()

    // TODO
    //def GIT_BRANCH_NAME = vars.get("GIT_BRANCH_NAME", "develop")

    script {

        checkout scm

        //gitCheckoutTESTRepo(vars) {
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
