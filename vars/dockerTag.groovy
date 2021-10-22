#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

/**
 * <h1>Generated docker tag</h1>
 * <p>
 * This method allow us to normalize docker tagging.
 * Default docker tag is @see helmTag
 * </p>
 *
 * <b>Note:</b> This method should always be called when a docker tag is generated.
 * Goal is to normalize docker tagging everywhere.
 *
 * @param dockerTag Allow to override dockerTag
 * @param commit Allow to override commit
 * @param dbms Allow to override dbms
 * @return docker tag to use
 */
def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/dockerTag.groovy`"

  vars = vars ?: [:]

  vars.dockerTag = vars.get("dockerTag", env.DOCKER_TAG ?: "temp").trim()

  if (body) { body() }

  return helmTag(vars)

}
