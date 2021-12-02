#!/usr/bin/groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  vars = vars ?: [:]

  vars.isDefaultBranch = vars.get('isDefaultBranch', false).toBoolean()
  vars.gitDefaultBranchName = vars.get('gitDefaultBranchName', 'master').trim()

  def myBranches = null

  if (vars.isDefaultBranch) {
    //echo 'Default branches managed by Jenkins'
    myBranches = scm.branches
    } else {
    myBranches = [[name: vars.gitDefaultBranchName]]
  }

  return myBranches
}
