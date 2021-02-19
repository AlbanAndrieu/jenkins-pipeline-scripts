#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/draftBranch.groovy`"

  vars = vars ?: [:]

  vars.draftBranch = vars.get("draftBranch", env.DRAFT_BRANCH ?: env.BRANCH_NAME).trim()
  vars.commit = vars.get("commit", env.GIT_COMMIT ?: "").trim() // getCommitId.groovy

  //vars.pomFile = vars.get("pomFile", "./pom.xml").trim()
  //RELEASE_VERSION = getSemVerReleasedVersion(vars) ?: "${vars.draftBranch}"

  try {

    //println(env.DRAFT_BRANCH)
    if (!env.DRAFT_BRANCH?.trim() || env.DRAFT_BRANCH.trim() == "null" || env.DRAFT_BRANCH == null) {
      env.DRAFT_BRANCH = env.BRANCH_NAME

      echo "NEW DRAFT_BRANCH : ${env.DRAFT_BRANCH}"
    } // DRAFT_BRANCH is empty
    environment()
  } catch(exc) {
      echo 'Error: There were errors in draftBranch : '+exc.toString()
      env.DRAFT_BRANCH = "develop"
  }
  echo "DRAFT_BRANCH : ${env.DRAFT_BRANCH}"

  if (body) { body() }

  return env.DRAFT_BRANCH.toLowerCase()

}
