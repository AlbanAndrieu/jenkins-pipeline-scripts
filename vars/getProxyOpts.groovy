#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getProxyOpts.groovy`"

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.isProxy = vars.get("isProxy", true).toBoolean()

  String DOCKER_OPTS_PROXY = ""

  if (vars.isProxy == true) {
    DOCKER_OPTS_PROXY += " " + [ "-e http_proxy=${env.HTTP_PROXY}",
      "-e HTTP_PROXY=${env.HTTP_PROXY}",
      "-e https_proxy=${env.HTTPS_PROXY}",
      "-e HTTPS_PROXY=${env.HTTPS_PROXY}",
      "-e no_proxy='${env.NO_PROXY}'",
      "-e NO_PROXY='${env.NO_PROXY}'" ].join(" ") + " "
  }

  return DOCKER_OPTS_PROXY

}
