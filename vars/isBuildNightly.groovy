#!/usr/bin/groovy
import hudson.model.*

def call() {
  this.vars = [:]
  call(vars)
}

/**
 * <h1>Tell you if nighlty build is enabled.</h1>
 * <p>
 * Check if <code>BUILD_NIGHLTY</code> parameter has been filled, otherwise return default value.
 * </p>
 *
 * <b>Note:</b> Nighly build will be a jenkins trigger defined by a cron.
 * @See setUp or createPropertyList
 *
 * @param BUILD_NIGHLTY Allow to override default return.
 * @return Return true if parameter has been setted to true by user (if not overriden by BUILD_NIGHLTY param).
 */
def call(Map vars) {

  echo "[JPL] Executing `vars/isBuildNightly.groovy`"

  vars = vars ?: [:]

  vars.BUILD_NIGHLTY = vars.get("BUILD_NIGHLTY", env.BUILD_NIGHLTY ?: true).toBoolean()
  echo "VAR IS: ${vars.BUILD_NIGHLTY.toString()}, ENV is: ${env.BUILD_NIGHLTY.toString()}"

  try {
    if (!params.BUILD_NIGHLTY.toBoolean()) {
      echo "BUILD_NIGHLTY setted to false"
      vars.BUILD_NIGHLTY = false
    }

  } catch(exc) {
    echo 'Warning: There were errors in isBuildNightly : '+exc.toString()
  }

  // Default behaviour we do the nightly build
  return vars.BUILD_NIGHLTY

}
