#!/usr/bin/groovy
import hudson.model.*

//@Deprecated please do not use directly, instead use dockerPush

@Deprecated
def pushAndRemoveDockerImage(def container, def image, def tag, def remove=true) {
  // Push docker image to DTR and clean local docker images
  // Assumes that tag is in the safe format (i.e. it is unique by using git sha1)
  // If it is not unique tag (like "latest"), this method should be used with remove=false
  try {
    echo 'Pushing docker container'
    echo "Container is ${container}"
    container.push()
    if (env.BRANCH_NAME ==~ /develop/) {
      container.push('develop')
      container.push('latest')
    }
    // No need to explicitly removed "latest", it will be overwritten by any subsequent build
    // of the same job. Removing "latest" during build nablas race condition with parallel builds
    // For that reason, cleaning is implemented as nightly cron job for docker VM's
    if (remove) {
      sh "docker rmi ${image}:${tag}"
    }
    } catch (exception) {
    currentBuild.result = 'UNSTABLE'
    echo "Issue: ${exception.getMessage()}"
    echo "Trace: ${exception.getStackTrace()}"
    echo 'Error: There was a problem Pushing / Deleting local docker image.'
  }
}

@Deprecated
def call(def container, def image, def tag) {
  if (null != container) {
    pushAndRemoveDockerImage(container, image, tag, false)
  }
}

@Deprecated
def call(container, image, tag, date, buildNumber, releaseBranch) {
  if (null != container) {
    if (releaseBranch) {
      doDockerImagePush(container, image, tag)
    }
    if (env.BRANCH_NAME ==~ /develop/) {
      doDockerImagePush(container, image, 'develop')
      doDockerImagePush(container, image, 'latest')
    //doDockerImagePush(container, image, date+"_"+buildNumber)
    }
  }
}
