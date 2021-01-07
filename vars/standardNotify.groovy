#!/usr/bin/groovy

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def content = '${SCRIPT, template="pipeline.template"}'
  String JOB_NAME = getGitRepoName(vars)
  emailext(
      subject: "${currentBuild.currentResult}: ${JOB_NAME} [${env.BUILD_NUMBER}]",
      to: emailextrecipients([
      [$class: 'CulpritsRecipientProvider'],
      [$class: 'DevelopersRecipientProvider'],
      [$class: 'RequesterRecipientProvider']
      ]), body: content
  )
}
