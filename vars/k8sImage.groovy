#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Allow to set the image that will be used on a current deployment.</h1>
 * Do kubectl set image.
 *
 * <b>Note:</b> Allow to test only the new docker image on an already deployed release on the cluster.
 *
 * @param skipHelmImage Do not change the docker image.
 */
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sImage.groovy`'

  vars = vars ?: [:]

  vars.DOCKER_ORGANISATION = vars.get('DOCKER_ORGANISATION', env.DOCKER_ORGANISATION ?: 'nabla').trim()
  vars.DOCKER_REGISTRY_ACR = env.DOCKER_REGISTRY_ACR ?: 'p21d13401013001.azurecr.io'.toLowerCase().trim()

  vars.HELM_KUBECONTEXT = k8sCluster(vars)

  vars.HELM_NAMESPACE = helmNamespace(vars)

  helmChartName(vars)

  vars.dockerTag = vars.get('dockerTag', env.DOCKER_TAG ?: 'latest').toLowerCase().trim()

  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()

  vars.helmDeploymentName = vars.get('helmDeploymentName', "${env.HELM_RELEASE}-${vars.helmChartName}").toLowerCase().trim()
  vars.helmContainerName = vars.get('helmContainerName', "${vars.helmChartName}").toLowerCase().trim()

  vars.skipHelmImageFailure = vars.get('skipHelmImageFailure', true).toBoolean()
  vars.skipHelmImage = vars.get('skipHelmImage', false).toBoolean()
  vars.k8sImageOutputFile = vars.get('k8sImageOutputFile', "helm-image-${vars.helmFileId}.log").trim()

  //if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
  //  vars.skipHelmImage = true
  //}

  if (!vars.skipHelmImage) {
    try {
      if (body) { body() }

      sh "kubectl rollout status --watch=true deployment ${vars.helmDeploymentName} || true"

      echo "kubectl set image deployment/${vars.helmDeploymentName} ${vars.helmContainerName}=${vars.DOCKER_REGISTRY_ACR}/${vars.DOCKER_ORGANISATION}/${vars.imageName}:${vars.dockerTag} --record"
      String k8sImageCmd = 'kubectl '

      //k8sImageCmd += " ${vars.helmDir}/${vars.helmChartName}/charts "
      if (vars.KUBECONFIG?.trim()) {
        k8sImageCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.HELM_KUBECONTEXT?.trim()) {
        k8sImageCmd += " --context ${vars.HELM_KUBECONTEXT} "
      }
      if (vars.HELM_NAMESPACE?.trim()) {
        k8sImageCmd += " --namespace ${vars.HELM_NAMESPACE} "
      }

      k8sImageCmd += " set image deployment/${vars.helmDeploymentName} ${vars.helmContainerName}=${vars.DOCKER_REGISTRY_ACR}/${vars.DOCKER_ORGANISATION}/${vars.imageName}:${vars.dockerTag} --record "

      // TODO Remove it when tee will be back
      k8sImageCmd += " > ${vars.k8sImageOutputFile} 2>&1 "

      helm = sh (script: k8sImageCmd, returnStatus: true)
      echo "HELM IMAGE RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM IMAGE SUCCESS'
      } else {
        echo "WARNING : Helm image failed, check output at \'${env.BUILD_URL}artifact/${vars.k8sImageOutputFile}\' "
        setBuildDescription(description: "<br/><b><font color=\"red\">Failed</font></b> k8sImage: <a href='${env.BUILD_URL}artifact/${vars.k8sImageOutputFile}/*view*/'>${vars.helmChartName}</a>")
        if (!vars.skipHelmImageFailure) {
          echo 'HELM IMAGE UNSTABLE'
          currentBuild.result = 'UNSTABLE'
          error 'There are errors in helm image'
        } else {
          echo 'HELM IMAGE UNSTABLE skipped'
        //error 'There are errors in helm'
        }
      }

      sh "kubectl rollout history deployment ${vars.helmDeploymentName} || true"
    } catch (exc) {
      echo 'Warn: There was a problem with helm image : ' + exc
    } finally {
      cleanEmptyFile(vars)
      archiveArtifacts artifacts: "${vars.k8sImageOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
      echo "Check : ${env.BUILD_URL}artifact/${vars.k8sImageOutputFile}"
    }
  } else {
    echo 'K8s Image skipped'
  }
}
