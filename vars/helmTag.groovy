#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/helmTag.groovy`"

  vars = vars ?: [:]

  vars.helmTag = vars.get("helmTag", env.HELM_TAG ?: "0.0.1").trim()
  vars.commit = vars.get("commit", env.GIT_COMMIT ?: "").trim() // getCommitId.groovy
  vars.buildId = vars.get("buildId", env.BUILD_ID ?: env.BUILD_NUMBER).trim()
  vars.date = vars.get("date", timestamp())
  vars.pomFile = vars.get("pomFile", "./pom.xml").trim()

  RELEASE_VERSION = getSemVerReleasedVersion(vars) ?: "${vars.helmTag}"

  RELEASE_VERSION += "-" + vars.date

  if (vars.commit != null && vars.commit.trim() != "" ) {
    def commitShortSHA1 = vars.commit.take(7)
    RELEASE_VERSION += ".sha${commitShortSHA1}"
  }

  RELEASE_VERSION += "." + vars.buildId

  if (body) { body() }

  return RELEASE_VERSION.toLowerCase()

}

def timestamp() {
    def now = new Date()
    def timezone="Europe/Paris"
    return now.format("yyyyMMdd'T'HHmmss", TimeZone.getTimeZone(timezone))
}
