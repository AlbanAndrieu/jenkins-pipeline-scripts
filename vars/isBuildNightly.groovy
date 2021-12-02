#!/usr/bin/groovy
import hudson.model.*

def call() {
  this.vars = [:]
  call(vars)
}

/**
 * <h1>Tell you if nightly build is enabled.</h1>
 * <p>
 * Check if <code>BUILD_NIGHTLY</code> parameter has been filled, otherwise return default value.
 * </p>
 *
 * <b>Note:</b> Nighly build will be a jenkins trigger defined by a cron.
 * @See setUp or createPropertyList
 *
 * @param BUILD_NIGHTLY Allow to override default return.
 * @return Return true if parameter has been setted to true by user (if not overridden by BUILD_NIGHTLY param).
 */
def call(Map vars) {
  echo '[JPL] Executing `vars/isBuildNightly.groovy`'

  vars = vars ?: [:]

  vars.BUILD_NIGHTLY = vars.get('BUILD_NIGHTLY', env.BUILD_NIGHTLY ?: true).toBoolean()
  echo "VAR IS: ${vars.BUILD_NIGHTLY}, ENV is: ${env.BUILD_NIGHTLY}"

  try {
    if (!params.BUILD_NIGHLTY.toBoolean()) {
      echo 'BUILD_NIGHTLY setted to false'
      vars.BUILD_NIGHTLY = false
    }
  } catch (exc) {
    echo 'Warning: There were errors in isBuildNightly : ' + exc
  }

  // Default behaviour we do the nightly build
  return vars.BUILD_NIGHTLY
}
