#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getCommitId.groovy`"

    vars = vars ?: [:]

    try {
      //println(env.GIT_COMMIT)
      if (!env.GIT_COMMIT?.trim()) {
          env.GIT_COMMIT = getCommitSha()
      }
      environment()
    }
    catch(exc) {
        echo 'Error: There were errors in getCommitId. '+exc.toString()
        env.GIT_COMMIT = "TODO"
    }
    return env.GIT_COMMIT
}
