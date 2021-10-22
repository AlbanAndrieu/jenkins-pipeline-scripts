#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Give docker build tag</h1>
 * <p>
 * This method allow us to normalize docker build tag.
 * Default docker tag is latest
 * </p>
 *
 * <b>Note:</b> This method should always be called when a build image is used.
 * Goal is to normalize usage of docker build image everywhere across time.
 *
 * @param dockerImageTag Allow to override dockerImageTag
 * @param isLatest Allow to override commit
 * @return docker build tag to use
 */
def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/dockerImageTag.groovy`"

  vars = vars ?: [:]

  vars.dockerImageTag = vars.get("dockerImageTag", env.DOCKER_IMAGE_TAG ?: "1.2.2").trim()

  vars.isLatest = vars.get("isLatest", false).toBoolean()

  if (vars.isLatest) {
    vars.dockerImageTag = "latest"
  }

  if (body) { body() }

  echo " dockerImageTag : " + vars.dockerImageTag

  return vars.dockerImageTag.toLowerCase() ?: "latest"
}
