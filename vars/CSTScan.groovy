#!/usr/bin/groovy
import hudson.model.*

@Deprecated
// use instead withCSTWrapper
def call(def DOCKER_IMAGE = "", def CONFIG_FILE = "config.yaml", def CST_VERSION = "1.5.0") {
  def CST_IMAGE = "gcr.io/gcp-runtimes/container-structure-test:${CST_VERSION}"
  try {
    sh "docker pull ${CST_IMAGE}"
    sh "docker run --rm --volume ${pwd()}:/data --volume /var/run/docker.sock:/var/run/docker.sock ${CST_IMAGE} test --image ${DOCKER_IMAGE} --config /data/${CONFIG_FILE}"
    sh "docker run --rm --volume ${pwd()}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ubuntu chown -R \$(id -u):\$(id -g) ."
  } catch (exc) {
    echo "Warn: There was a problem with cst scan image \'${DOCKER_IMAGE}\' " + exc.toString()
  }
}
