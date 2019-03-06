#!/usr/bin/groovy
import jenkins.model.CauseOfInterruption
import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def call() {
  def hi = Hudson.instance
  def pname = env.JOB_NAME.split('/')[0]
  def bname = env.JOB_NAME.split('/')[1]  // branch name

  echo "abortPreviousRunningBuilds : ${pname} - ${bname} #${currentBuild.number}"

  if (! isReleaseBranch()) {

      try {
          milestone 1
          hi.getItem(pname).getItem(env.JOB_BASE_NAME).getBuilds().each{ build ->
              def exec = build.getExecutor()

              echo "  ${build.number} - ${exec}"

              if (null != exec) {

                //print currentBuild.getBuiltOn().getNodeName()
                //def build = currentBuild.build()
                //print exec.getOwner().getNode().getNodeName()

                if (build.number < currentBuild.number && exec != null && build.isBuilding()) {
                  def user = getBuildUser().toString()
                  echo "Aborted by " + user
                exec.interrupt(
                  Result.ABORTED,
                  new jenkins.model.CauseOfInterruption.UserInterruption(
                      "Aborted by ${user} - ${pname} - ${bname} #${currentBuild.number}"
                  )
                )
                println("${pname} - ${bname} / ${env.JOB_BASE_NAME} : Aborted previous running build #${build.number}")
              } else {
                println("${pname} - ${bname} / ${env.JOB_BASE_NAME} : Build is not running or is already built, not aborting #${build.number}")
                }
              }
          }
      } catch(NullPointerException e) {
          // happens the first time if there is no branch at all
          echo 'Error: There were errors in abortPreviousRunningBuilds. '+e.toString()
      } finally {
          // carry on as if nothing went wrong
      }

  } // isReleaseBranch

} // abortPreviousRunningBuilds

@NonCPS
def getBuildUser() {
    if (currentBuild.rawBuild.getCause(Cause.UserIdCause) != null) {
        return currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
    } else {
        return "UNKNOWN SYSTEM USER"
    }
}
