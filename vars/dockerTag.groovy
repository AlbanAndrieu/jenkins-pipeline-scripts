#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/dockerTag.groovy`"

  vars = vars ?: [:]

  vars.dockerTag = vars.get("dockerTag", env.DOCKER_TAG ?: "temp").trim()

  if (body) { body() }

  return helmTag(vars)

}

