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
 * getSonarInclusions.groovy
 *
 * Create a comma "," separated list of files changed in Pull Request specified by pullRequestId
 *
 * List can be reused for "sonar.inclusions" to perform differential scan
 */
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  // Defaults
  config.jenkinsCredentialsId = config.get("jenkinsCredentialsId", "jenkins-https")
  config.scmBaseUrl           = config.get("scmBaseUrl", "https://github.com/")
  config.pullRequestId        = config.get("pullRequestId", env.PULL_REQUEST_ID)
  config.workspace            = config.get("workspace", env.WORKSPACE)
  config.jobName              = config.get("jobName", env.JOB_NAME)

  config.project              = config.get("project", get_project_name(config.jobName, "TEST"))
  config.repository           = config.get("repository", get_repository_name(config.jobName))

  def authString
  def files

  withCredentials([
    usernamePassword(
      credentialsId: config.jenkinsCredentialsId,
      usernameVariable: 'stash_login',
      passwordVariable: 'stash_pass'
    )
  ]) {
    authString = "${stash_login}:${stash_pass}".getBytes().encodeBase64().toString()
  }

  URLConnection pullRequestDiffConn = new URL(
    "${config.scmBaseUrl}/rest/api/1.0/projects/${config.project}/repos/${config.repository}/pull-requests/${config.pullRequestId}/changes?limit=1000"
  ).openConnection()
  pullRequestDiffConn.setRequestProperty("Authorization", "Basic ${authString}")
  def pullRequestDiff = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(pullRequestDiffConn.getInputStream())))

  if (workspace == null) {
    files = pullRequestDiff.values.collect {
      it.path.toString
    }.join(",")
  } else {
    files = pullRequestDiff.values.collect {
      workspace + "/" + it.path.toString
    }.join(",")
  }
  println("[JPL] Following files will be included in Sonar Scan ${files}")
  return files
}
