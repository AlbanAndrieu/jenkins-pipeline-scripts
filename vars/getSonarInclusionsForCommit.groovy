import groovy.json.JsonSlurper

/**
 * get_project_name
 *
 * Defines separate behaviour for Jenkins `Multibranch Pipeline` and `Bitbucket Team Project`
 * returns project name from JOB_BASE_NAME
 *
 */
def get_project_name(jobName, defaultProject="RISK") {
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
    return tokenized_job_name[0]  // e.g. CMR/develop
  } else {
    return tokenized_job_name[1]  // e.g. RISK/generic_limits/develop
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

def call(body) {
    echo "[JPL] Executing `vars/getSonarInclusionsForCommit.groovy`"
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    echo "[JPL] Base CONFIG params set from JENKINSFILE are: ${config}"

    // Run configuration
    // If number of commits exceeds the Cap, file list will not be searched (to save time); the script will return ["*"]
    config.commitCap            = config.get("commitCap", 50)
    // If number of files changes exceeds the Cap, all files should be Sonar-scanned; the script will return ["*"]
    config.fileCap              = config.get("fileCap", 50)
    config.jenkinsCredentialsId = config.get("jenkinsCredentialsId", "jenkins-https")
    config.baseUrl              = config.get("baseUrl", "https://github.com/")
    config.jobName              = config.get("jobName", env.JOB_NAME)
    config.project              = config.get("project", get_project_name(config.jobName, "TEST"))
    config.repository           = config.get("repository", get_repository_name(config.jobName))
    config.targetRevision       = config.get("targetRevision", "refs/heads/develop")
    config.currentRevision      = config.get("currentRevision", env.GIT_COMMIT)

    echo "[JPL] Full CONFIG after applying the fedault values is: ${config}"

    withCredentials([
        usernamePassword(
        credentialsId: config.jenkinsCredentialsId,
        usernameVariable: 'stashLogin',
        passwordVariable: 'stashPass'
        )
    ]) {
        config.basicAuth = "${stashLogin}:${stashPass}".getBytes().encodeBase64().toString()
    }

    List<String> diffRevisions = diffCommitsForRev(config)
    if (diffRevisions.size() >= config.commitCap) {
      echo "[JPL] Number of unique commits between the two revisions: ${diffRevisions.size()} exceeds the Cap: ${config.commitCap}; returning default value"
      return ["*"]
    }

    echo "[JPL] Found following revisions between source and target branch: ${diffRevisions}"

    List<String> filesChanged = []
    for (diffRevision in diffRevisions) {
        filesChanged += filesForRev(config, diffRevision)
    }
    filesChanged = filesChanged.unique()

    echo "[JPL] Found following changed files source and target branch: ${filesChanged}"

    if (filesChanged.size() >= config.fileCap) {
      echo "[JPL] Number of files changes: ${filesChanged.size()} exceeds the Cap: ${config.fileCap}; returning default value"
      return ["*"]
    }

    filesChanged

}
