#!/usr/bin/groovy
import hudson.model.*

def call(def DOCKER_IMAGE = "", def REPORT_NAME = "", def AQUA_VERSION = "latest") {
  def AQUA_IMAGE = "nabla/test/aquasec-scanner-cli:${AQUA_VERSION}"
  def AQUA_USER = '--user scanner'
  def AQUA_PASS = '--password password'
  def AQUA_HOST = '--host http://albandri:8080'
  def AQUA_REPORT = 'aqua.html'
  def AQUA_OPTS = [
    '--local',
    '--direct-cc',
    "--jsonfile /mnt/${AQUA_REPORT}.AquaSec",
    "--htmlfile /mnt/${AQUA_REPORT}"
  ].join(" ")
  try {
    sh "rm -f ${pwd()}/${AQUA_REPORT}"
    sh "rm -f ${pwd()}/${AQUA_REPORT}.AquaSec"
    sh "docker pull ${AQUA_IMAGE}"
    sh "docker run --rm --volume ${pwd()}:/mnt --volume /var/run/docker.sock:/var/run/docker.sock ${AQUA_IMAGE} scan ${AQUA_USER} ${AQUA_PASS} ${AQUA_HOST} ${AQUA_OPTS} ${DOCKER_IMAGE} > /dev/null"
    sh "docker run --rm --volume ${pwd()}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ubuntu chown -R \$(id -u):\$(id -g) ."
  } catch (exc) {
    echo "Warn: There was a problem with aqua scan image \'${DOCKER_IMAGE}\' " + exc.toString()
  }
  if (fileExists("${pwd()}/${AQUA_REPORT}")) {
    publishHTML([
      allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: '',
      reportFiles : "${AQUA_REPORT}", reportName: "Aqua_${REPORT_NAME}", reportTitles: "${DOCKER_IMAGE}"
    ])
  } else {
    echo "Aqua scan output file ${AQUA_REPORT} was not generated"
  }
}
