#!/usr/bin/groovy
import hudson.model.*

/**
 * get_project_name
 *
 * Defines separate behaviour for Jenkins `Multibranch Pipeline` and `Bitbucket Team Project`
 * returns project name from JOB_BASE_NAME
 *
 */
def get_project_name(jobName, defaultProject="NABLA") {
  def tokenized_job_name = jobName.split('/')
  if (tokenized_job_name.size() == 2) {
    return defaultProject  // e.g. CMR/develop, there is no good way to get Stash project name
  } else {
    return tokenized_job_name[0]
  }
}

/**
 * get_repository_name
 *
 * Defines separate behaviour for Jenkins `Multibranch Pipeline` and `Bitbucket Team Project`
 * returns repository name from JOB_BASE_NAME
 *
 */
def get_repository_name(jobName) {
  def tokenized_job_name = jobName.split('/')
  if (tokenized_job_name.size() == 2) {
    return tokenized_job_name[0]  // e.g. NABLA/develop
  } else {
    return tokenized_job_name[1]  // e.g. TMP/nabla/develop
  }
}

/**
 * diffCommitsForRev
 *
 * returns a list of commits between current and target branch revision. Target defaults to develop.
 */
List<String> diffCommitsForRev(Map<String,String> config) {
    URLConnection filesForRevConn = new URL(
        "${config.baseUrl}/rest/api/1.0/projects/${config.project}/repos/${config.repository}/commits?since=${config.targetRevision}&until=${config.currentRevision}&limit=${config.commitCap}"
    ).openConnection()

    filesForRevConn.setRequestProperty("Authorization", "Basic ${config.basicAuth}")

    def filesForRev = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(filesForRevConn.getInputStream())))

    List<String> diffRevisions = filesForRev.values.collect {
        it.id
    }

    diffRevisions
}

/**
 * diffCommitsForRev
 *
 * returns a list of commits between current and target branch revision. Target defaults to develop.
 */
List<String> filesForRev(Map<String,String> config, String revision) {
    URLConnection filesForRevConn = new URL(
        "${config.baseUrl}/rest/api/1.0/projects/${config.project}/repos/${config.repository}/commits/${revision}/changes?limit=${config.fileCap}"
    ).openConnection()

    filesForRevConn.setRequestProperty("Authorization", "Basic ${config.basicAuth}")

    def filesForRev = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(filesForRevConn.getInputStream()))) // LazyMap

    List<String> diffRevisions = filesForRev.values.collect {
        it.path.toString
    }

    diffRevisions
}

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getSonarInclusions.groovy`"

    vars = vars ?: [:]

    def sonarCmdParameters = vars.get("sonarCmdParameters", "")
    def project = vars.get("project", "RISK")
    def repository = vars.get("repository", "")

    echo "[JPL] Repository: ${repository}"

    if ( env.BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
        echo "[JPL] isReleaseBranch, so no check for `vars/getSonarInclusions.groovy`"
    } else {

        def sonarInclusions = filesChanged(vars) {}.collect{ filename -> "${env.WORKSPACE}/${filename}" }.join(",")

        if (body) { body() }

        echo "[JPL] Sonar Sources: ${sonarInclusions}"

	    if (sonarInclusions?.trim()) {
	    	  sonarCmdParameters = " -Dsonar.inclusions=\"${sonarInclusions}\" "
	    }

    } // else

    return sonarCmdParameters

}

def filesChanged(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getSonarInclusions.groovy` filesChanged"

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    echo "[JPL] Base CONFIG params set from JENKINSFILE are: ${config}"

    vars = vars ?: [:]

    // Run configuration
    // If number of commits exceeds the Cap, file list will not be searched (to save time); the script will return ["*"]
    vars.commitCap            = vars.get("commitCap", 50)
    // If number of files changes exceeds the Cap, all files should be Sonar-scanned; the script will return ["*"]
    vars.fileCap              = vars.get("fileCap", 50)
    vars.jenkinsCredentialsId = vars.get("jenkinsCredentialsId", "jenkins")
    vars.baseUrl              = vars.get("baseUrl", "https://github.com/")
    vars.jobName              = vars.get("jobName", env.JOB_NAME)
    vars.project              = vars.get("project", get_project_name(vars.jobName, "NABLA"))
    vars.repository           = vars.get("repository", get_repository_name(vars.jobName))
    vars.targetRevision       = vars.get("targetRevision", "refs/heads/develop")
    vars.currentRevision      = vars.get("currentRevision", env.GIT_COMMIT)

    echo "[JPL] Full CONFIG after applying the fedault values is: ${vars}"

    withCredentials([
        usernamePassword(
        credentialsId: vars.jenkinsCredentialsId,
        usernameVariable: 'stashLogin',
        passwordVariable: 'stashPass'
        )
    ]) {
      vars.basicAuth = "${stashLogin}:${stashPass}".getBytes().encodeBase64().toString()
    }

    List<String> diffRevisions = diffCommitsForRev(vars)
    if (diffRevisions.size() >= vars.commitCap) {
      echo "[JPL] Number of unique commits between the two revisions: ${diffRevisions.size()} exceeds the Cap: ${vars.commitCap}; returning default value"
      return ["*"]
    }

    echo "[JPL] Found following revisions between source and target branch: ${diffRevisions}"

    List<String> filesChanged = []
    for (diffRevision in diffRevisions) {
        filesChanged += filesForRev(vars, diffRevision)
    }
    filesChanged = filesChanged.unique()

    echo "[JPL] Found following changed files source and target branch: ${filesChanged}"

    if (filesChanged.size() >= vars.fileCap) {
      echo "[JPL] Number of files changes: ${filesChanged.size()} exceeds the Cap: ${vars.fileCap}; returning default value"
      return ["*"]
    }

    filesChanged

}
