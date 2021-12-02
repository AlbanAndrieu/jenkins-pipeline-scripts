#!/usr/bin/groovy
import java.*
import hudson.*
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  vars = vars ?: [:]

  if (!body) {
    echo 'No body specified'
  }

  def relativeTargetDir = vars.get('relativeTargetDir', 'bm')
  def isDefaultBranch = vars.get('isDefaultBranch', true).toBoolean()

  script {
        gitCheckoutBMRepo(vars) {
      dir ('bm') {
        getGitData(vars)

        if (body) { body() }
            } // dir
        }

        gitCheckoutTESTRepo(vars)
    } // script
}
