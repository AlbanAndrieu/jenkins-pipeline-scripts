#!/usr/bin/groovy
import hudson.model.*

def call() {
  this.vars = [:]
  call(vars)
}

/**
 * <h1>Check if the build job was started by an user.</h1>
 * <p>
 * Check if the build job was started by an user or jenkins itself (upstream of cron trigger)).
 * </p>
 *
 * @param isStartedByUser Allow override default return.
 * @return isStartedByUser Return true if job was started by user.
 */
def call(Map vars) {

  echo '[JPL] Executing `vars/isStartedByUser.groovy`'

  vars = vars ?: [:]

  def isStartedByUser = vars.get('isStartedByUser', false).toBoolean()

  try {
    isStartedByUser = currentBuild.rawBuild.getCause(Cause$UserIdCause) != null

    if (isDebugRun(vars)) {
      echo ' isStartedByUser : ' + isStartedByUser
      echo "buildCauses : ${currentBuild.buildCauses}" // same as currentBuild.getBuildCauses()
      echo "Cause UserCause : ${currentBuild.getBuildCauses('hudson.model.Cause$UserCause')}"
      echo "TimerTrigger TimerTriggerCause : ${currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause')}"
      } // DEBUG_RUN
    } catch (exc) {
    echo 'Warning: There were errors in isStartedByUser : ' + exc
  }

  return isStartedByUser.toBoolean()
}
