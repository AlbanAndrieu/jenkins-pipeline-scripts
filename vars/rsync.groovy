#!/usr/bin/groovy
import hudson.model.*

def call(Map params) {
  withCredentials([
    usernamePassword(
      credentialsId: params.credentialsId,
      passwordVariable: 'RSYNC_PASSWORD',
      usernameVariable: 'RSYNC_USER'
    )
  ]) {
    sh """
      rsync --temp-dir=/tmp --archive --dirs --verbose --compress --checksum=sha1 --rsh="sshpass -p ${RSYNC_PASSWORD} ssh -o StrictHostKeyChecking=no -l ${RSYNC_USER}" -- ${params.source} ${params.destination} >&2
    """
  }
}
