#!/usr/bin/groovy
import hudson.model.*

def call() {
  this.vars = [:]
  call(vars)
}

/**
 * <h1>Check if we can use groovy-postbuild plugin.</h1>
 * <p>
 * Check manager class is available.
 * </p>
 * <b>Note:</b> If manager is not available, exception will be caught.
 *
 * @param skipManager Allow to by pass feature.
 * @return isManager Return if groovy-postbuild can be used.
 */
def call(Map vars) {
  echo '[JPL] Executing `vars/isManager.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.skipManager = vars.get('skipManager', false).toBoolean()

  vars.isManager = vars.get('isManager', false).toBoolean()

  if (!vars.skipManager) {
    try {
      // will break with : groovy.lang.MissingPropertyException: No such property: manager for class: WorkflowScript
      // if plugin not installed
      println manager.getClass()

      if (manager.getClass() == org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder$BadgeManager) {
        vars.isManager = true
      }

      if (isDebugRun(vars)) {
        echo ' isManager : ' + vars.isManager
      } // DEBUG_RUN
    } catch (exc) {
      echo 'Warning: There were errors in isManager : ' + exc
      println hudson.console.ModelHyperlinkNote.encodeTo('https://plugins.jenkins.io/groovy-postbuild/', 'Please install plugin groovy-postbuild')
      vars.isManager = false
    }
  } else {
    echo 'Manager skipped'
  }

  return vars.isManager.toBoolean()
}
