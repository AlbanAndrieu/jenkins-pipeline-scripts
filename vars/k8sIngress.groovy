#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/k8sIngress.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.HELM_KUBECONTEXT = k8sCluster(vars)

  //vars.HELM_NAMESPACE = helmNamespace(vars)

  vars.HELM_NAMESPACE_INGRESS = vars.get('HELM_NAMESPACE_INGRESS', env.HELM_NAMESPACE_INGRESS ?: 'ingress-nginx').trim()

  //vars.k8IngressSelector = vars.get("k8sIngressSelector", 'app.kubernetes.io/component=controller').trim()
  //vars.k8IngressSelector = vars.get("k8sIngressSelector", 'app=default-http-backend').trim()
  vars.k8IngressSelector = vars.get('k8sIngressSelector', 'app=ingress-nginx').trim()

  vars.skipKubeIngress = vars.get('skipKubeIngress', false).toBoolean()
  vars.kubeIngressId = vars.get('kubeIngressId', vars.draftPack ?: '0').trim()
  vars.kubeIngressOutputFile = vars.get('kubeIngressOutputFile', "k8s-namespace-${vars.kubeIngressId}.json").trim()
  //vars.kubeIngressYmlOutputFile = vars.get("kubeIngressYmlOutputFile", "k8s-config-${vars.kubeIngressId}.yml").trim()
  //vars.kubeClusterInfoOutputFile = vars.get("kubeClusterInfoOutputFile", "k8s-cluster-info-${vars.kubeIngressId}.json").trim()

  if (!vars.skipKubeIngress) {
    try {
    //tee("${vars.kubeIngressOutputFile}") {

      String k8sIngressCmd = 'kubectl '

      //k8sIngressCmd += " ${vars.helmDir}/${vars.helmChartName}/charts "
      if (vars.KUBECONFIG?.trim()) {
        k8sIngressCmd += " --kubeconfig ${vars.KUBECONFIG} "
      }
      if (vars.HELM_KUBECONTEXT?.trim()) {
        k8sIngressCmd += " --kube-context ${vars.HELM_KUBECONTEXT} "
      }

      if (!vars.HELM_NAMESPACE_INGRESS?.trim()) {
        echo 'Namespace ingress is mandatory'
      } else {
        //if (vars.HELM_NAMESPACE?.trim()) {
        //  k8sIngressCmd += " --namespace ${vars.HELM_NAMESPACE} "
        //}
        if (vars.HELM_NAMESPACE_INGRESS?.trim()) {
          k8sIngressCmd += " --namespace ${vars.HELM_NAMESPACE_INGRESS} "
        }

        sh """#!/bin/bash -l
        echo "Wait for ingress : ${vars.HELM_NAMESPACE_INGRESS}"
        ${k8sIngressCmd} wait --for=condition=ready pod  --selector=${vars.k8IngressSelector} --timeout=120s
        """
      } // HELM_NAMESPACE_INGRESS

      //sh """#!/bin/bash -l
      //POD_NAME=$(kubectl get pods -l ${vars.k8IngressSelector} -o jsonpath='{.items[0].metadata.name}')
      //kubectl exec -it $POD_NAME -- /nginx-ingress-controller --version
      //"""

      //sh "kubectl --kubeconfig=${vars.KUBECONFIG} get namespaces --show-labels"

    //} // tee
    } catch (exc) {
      echo 'Warn: There was a problem with k8sIngress : ' + exc
    //println hudson.console.ModelHyperlinkNote.encodeTo(env.BUILD_URL + "/artifact/${vars.kubeIngressYmlOutputFile}", "${vars.kubeIngressYmlOutputFile}")
    //println hudson.console.ModelHyperlinkNote.encodeTo(env.BUILD_URL + "/artifact/${vars.kubeClusterInfoOutputFile}", "${vars.kubeClusterInfoOutputFile}")
    //} finally {
    //  cleanEmptyFile(vars)
    //  archiveArtifacts artifacts: "k8s-*.yml, **/k8s-*.log, ${vars.KUBECONFIG}, ${vars.kubeIngressOutputFile}, ${vars.kubeClusterInfoOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'KubeIngress skipped'
  }
}
