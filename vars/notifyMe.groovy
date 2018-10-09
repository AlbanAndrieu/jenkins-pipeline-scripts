#!/usr/bin/groovy

def call() {
  //// send to Slack
  //slackSend (color: '#FFFF00', message: "STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
  //slackSend channel: '#general', color: 'good', message: '[${currentBuild.result}] #${env.BUILD_NUMBER} ${env.BUILD_URL}', teamDomain: 'kitconcept', token: '<ADD-TOKEN-HERE>'
  //
  //// send to HipChat
  //hipchatSend (color: 'YELLOW', notify: true,
  //    message: "STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})"
  //)

  def content = '${SCRIPT, template="pipeline.template"}'
  //to: "${GIT_AUTHOR_EMAIL}"

  // send to email
  emailext (
      //subject: "STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      subject: ("${currentBuild.result}: ${env.TARGET_PROJECT} ${currentBuild.displayName}"),
      //body: """<p>${env.TARGET_PROJECT} STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]': build on branch ${BRANCH_NAME} resulted in ${currentBuild.result} :</p>
      //  <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
      body: content,
      attachLog: false,
      compressLog: true,
      to: emailextrecipients([
          [$class: 'CulpritsRecipientProvider'],
          [$class: 'DevelopersRecipientProvider'],
          [$class: 'RequesterRecipientProvider']
      ])
    )

} // notifyMe
