#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withCSTWrapper.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.DOCKER_RUNTIME_TAG = vars.get('DOCKER_RUNTIME_TAG', env.DOCKER_RUNTIME_TAG ?: 'latest').trim()
  vars.DOCKER_NAME_RUNTIME = vars.get('DOCKER_NAME_RUNTIME', env.DOCKER_NAME_RUNTIME ?: 'ansible-jenkins-slave').trim()

  def CST_VERSION = vars.get('CST_VERSION', env.CST_VERSION ?: 'latest').trim()

  vars.configFile = vars.get('configFile', env.configFile ?: 'config.yaml').trim()
  vars.scanner_image = vars.get('scanner_image', "gcr.io/gcp-runtimes/container-structure-test:${CST_VERSION}").trim()
  vars.imageName = vars.get('imageName', "${vars.DOCKER_NAME_RUNTIME}").trim()
  vars.imageTag = vars.get('imageTag', "${vars.DOCKER_RUNTIME_TAG}").trim()
  vars.localImage = vars.get('localImage', "${vars.DOCKER_ORGANISATION}/${vars.imageName}:${vars.imageTag}").trim()
  vars.buildCmdParameters = vars.get('buildCmdParameters', "docker pull ${vars.scanner_image}").trim()
  vars.buildCmd = vars.get('buildCmd', '').trim()
  vars.isFingerprintEnabled = vars.get('isFingerprintEnabled', false).toBoolean()
  vars.cstFileId = vars.get('cstFileId', vars.draftPack ?: '0').trim()
  vars.cstOutputFile = vars.get('cstOutputFile', "cst-${vars.cstFileId}.log").trim()
  vars.skipCSTFailure = vars.get('skipCSTFailure', false).toBoolean()
  vars.skipCST = vars.get('skipCST', false).toBoolean()

  //vars.locationType = vars.get("locationType", "hosted").trim() // hosted or local -> only local for CST

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipCST = true
  }

  if (!vars.skipCST) {
    try {
      tee("${vars.cstOutputFile}") {
        sh "find . -name \"${vars.configFile}*\" || true"
        sh 'pwd'

        try {
          vars.buildCmdParameters += " && docker pull ${vars.localImage} || true"

          vars.buildCmdParameters += ' && docker run'
          vars.buildCmdParameters += ' --rm'
          vars.buildCmdParameters += " --volume '${pwd()}:/data'"
          vars.buildCmdParameters += ' --volume /var/run/docker.sock:/var/run/docker.sock'
          vars.buildCmdParameters += " ${vars.scanner_image}"
          vars.buildCmdParameters += ' test '
          vars.buildCmdParameters += " --image ${vars.localImage} "
          vars.buildCmdParameters += " --config /data/${vars.configFile}"
          // TODO Remove it when tee will be back
          vars.buildCmdParameters += " 2>&1 > ${vars.cstOutputFile} "

          vars.buildCmdParameters += " && docker run --rm --volume ${pwd()}:/ws --workdir /ws --volume /etc/passwd:/etc/passwd --volume /etc/group:/etc/group ubuntu chown -R \$(id -u):\$(id -g) ."

          if (vars.buildCmdParameters?.trim()) {
            vars.buildCmd += " ${vars.buildCmdParameters}"
          }

              //vars.buildCmd +=        " > /dev/null"

          // Run the cst build
          build = sh (
                      script: "${vars.buildCmd}",
                      returnStatus: true
                      )
          echo "CST RETURN CODE : ${build}"
          if (build == 0) {
            echo 'CST SUCCESS'
              } else {
            if (!vars.skipCSTFailure) {
              echo 'CST UNSTABLE'
              currentBuild.result = 'UNSTABLE'
              echo "WARNING : Scan failed, check output at \'${env.BUILD_URL}artifact/${vars.cstOutputFile}\' "
                  } else {
              echo 'CST FAILURE skipped'
              error 'There are errors in cst'
            }
          }
          if (body) { body() }
            } catch (exc) {
          //currentBuild.result = 'FAILURE'
          echo "WARNING : There was a problem with cst scan image \'${vars.localImage}\' \'${vars.configFile}\' " + exc.toString()
        }
          } // tee
        } finally {
      archiveArtifacts artifacts: "${vars.cstOutputFile}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'CST scan skipped'
  }
}
