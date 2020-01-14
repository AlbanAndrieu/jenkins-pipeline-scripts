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

    echo "[JPL] Executing `vars/gitCheckoutTEST.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

    if (DEBUG_RUN && !body) {
        echo 'No body specified'
    }

    vars.relativeTargetDir = vars.get("relativeTargetDir", "bm")
    vars.isDefaultBranch = vars.get("isDefaultBranch", false).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", true).toBoolean()
    vars.isScmEnabled = vars.get("isScmEnabled", true).toBoolean()
    vars.isShallowEnabled = vars.get("isShallowEnabled", false).toBoolean()

    // TODO
    //def GIT_BRANCH_NAME = vars.get("GIT_BRANCH_NAME", "develop")

    script {

        checkout scm

        //gitCheckoutTESTRepo(vars) {
        //
        //   dir ("test") {
        //
        //       getGitData(vars)
        //
        //       if (body) { body() }
        //
        //   } // dir
        //
        //}

        // TODO
        //def GIT_BRANCH_NAME_BUILDMASTER = vars.get("GIT_BRANCH_NAME_BUILDMASTER", "develop")

        gitCheckoutBMRepo(vars)

        getGitData(vars)

        if (body) { body() }

    } // script
}
