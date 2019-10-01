#!/usr/bin/groovy
import hudson.model.*

def call(def DRAFT_PACK = "", def DRAFT_BRANCH = "develop", def DRAFT_VERSION = "0.0.1") {
  def DRAFT_IMAGE = "nabla/test/draft:${DRAFT_VERSION}"
  try {
    sh "docker pull ${DRAFT_IMAGE}"
    configFileProvider([configFile(fileId: 'mgr.jenkins', targetLocation: 'id_rsa')]) {
      sh "chmod 0777 ${pwd()}/id_rsa"
      sh "chmod -R 0777 ${pwd()}"
      sh "docker run --env DRAFT_BRANCH=${DRAFT_BRANCH} --rm --volume ${pwd()}/id_rsa:/home/jenkins/id_rsa --volume ${pwd()}:/home/jenkins/docker --workdir /home/jenkins/docker ${DRAFT_IMAGE} create -p ${DRAFT_PACK} "
      sh "docker run --rm --volume ${pwd()}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ubuntu chown -R \$(id -u):\$(id -g) ."
      sh "rm -f ${pwd()}/id_rsa"
    }
  } catch (exc) {
    echo "Warn: There was a problem with install of draft pack \'${DRAFT_PACK}\' from \'${DRAFT_BRANCH}\' " + exc.toString()
  }
}
