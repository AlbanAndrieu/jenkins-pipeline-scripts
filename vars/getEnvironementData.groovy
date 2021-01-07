#!/usr/bin/groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {

  echo "[JPL] Executing `vars/getEnvironementData.groovy`"

  vars = vars ?: [:]

  if (!body) {
      echo 'No body specified'
  }

  vars.DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
  vars.filePath = vars.get("filePath", "step-2-0-0-build-env.sh").trim()
  try {

		if (vars.DEBUG_RUN) {
		    sh "set -xv && ${vars.filePath}"
		} else {
		    sh "${vars.filePath}"
		}

		load "./jenkins-env.groovy"

    echo "GIT_COMMIT_SHORT: ${env.GIT_COMMIT_SHORT}"

    //printEnvironment()
    def fields = env.getEnvironment()
    fields.each {
         key, value -> println("${key} = ${value}");
     }

    println(env.PATH)

    sh "printenv | sort"

    //echo "PULL_REQUEST_ID : ${env.PULL_REQUEST_ID}"
    //echo "BRANCH_JIRA : ${env.BRANCH_JIRA}"
    //echo "PROJECT_BRANCH : ${env.PROJECT_BRANCH}"

    getGitRepoName()

    echo "JOB_NAME : ${env.JOB_NAME} - ${env.JOB_BASE_NAME}"
    String MY_JOB_NAME = env.JOB_NAME.replaceFirst(env.JOB_BASE_NAME, "").replaceAll("RKTMP", "").split("/")[0].toLowerCase().trim()
    echo "MY_JOB_NAME : ${MY_JOB_NAME}"
    //echo "GIT_URL : ${env.GIT_URL} - ${env.GIT_URL_1}"
    //echo "REPO_URL : ${env.REPO_URL} - ${env.GIT_REPO}"

    echo "BRANCH_NAME : ${env.BRANCH_NAME}"
    echo "GIT_BRANCH_NAME : ${env.GIT_BRANCH_NAME}"
    echo "TARGET_TAG : ${env.TARGET_TAG}"

    echo "RELEASE : ${env.RELEASE}"
    //echo "RELEASE_VERSION : ${env.RELEASE_VERSION}"
  } catch(exc) {
    echo 'Warning: There were errors in getEnvironementData. '+exc.toString()
  } finally {
    echo "DONE"
  }
}
