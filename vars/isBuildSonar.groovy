#!/usr/bin/groovy
import hudson.model.*

def call() {
  this.vars = [:]
  call(vars)
}

/**
 * <h1>Tell you if sonar build is enabled.</h1>
 * <p>
 * Check if <code>BUILD_SONAR</code> parameter has been filled, otherwise return default value.
 * </p>
 *
 * <b>Note:</b> Sonar build should be the default.
 *
 * @param BUILD_SONAR Allow to override default return.
 * @return Return true if parameter has been setted to true by user (if not overridden by BUILD_SONAR param).
 */
def call(Map vars) {

  echo '[JPL] Executing `vars/isBuildSonar.groovy`'

  vars = vars ?: [:]

  // Default behaviour we do the sonar build
  vars.BUILD_SONAR = vars.get('BUILD_SONAR', env.BUILD_SONAR ?: true).toBoolean()

  try {
    if (!params.BUILD_SONAR.toBoolean()) {
      echo 'BUILD_SONAR setted to false'
      vars.BUILD_SONAR = false
      return vars.BUILD_SONAR
    } else {
      return vars.BUILD_SONAR
    }
  } catch (exc) {
    // When there is no parms defined most of the time
    echo 'Warning: There were errors in isBuildSonar : ' + exc
  }

  // Default behaviour we do the sonar build
  return true
}
