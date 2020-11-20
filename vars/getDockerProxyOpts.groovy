#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getDockerProxyOpts.groovy`"

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.isProxy = vars.get("isProxy", true).toBoolean()

  String DOCKER_OPTS_PROXY = ""

  if (vars.isProxy == true) {
      DOCKER_OPTS_PROXY += " " + [ "--build-arg http_proxy=${env.HTTP_PROXY}",
                                   "--build-arg HTTP_PROXY=${env.HTTP_PROXY}",
                                   "--build-arg https_proxy=${env.HTTPS_PROXY}",
                                   "--build-arg HTTPS_PROXY=${env.HTTPS_PROXY}",
                                   "--build-arg no_proxy='${env.NO_PROXY}'",
                                   "--build-arg NO_PROXY='${env.NO_PROXY}'" ].join(" ") + " "
  }

  return DOCKER_OPTS_PROXY

}
