#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

// ConfigFileProvider is buggy, because it is not working inside docker image, bellow is workaround
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withConfigFileProviderWrapper.groovy`'

  vars = vars ?: [:]

  //getJenkinsOpts(vars)

  vars.NPM_SETTINGS_CONFIG = vars.get('NPM_SETTINGS_CONFIG', env.NPM_SETTINGS_CONFIG ?: 'nabla-npmrc-default').trim()
  vars.BOWER_SETTINGS_CONFIG = vars.get('BOWER_SETTINGS_CONFIG', env.BOWER_SETTINGS_CONFIG ?: 'nabla-bowerrc-default').trim()
  vars.MAVEN_SETTINGS_CONFIG = vars.get('MAVEN_SETTINGS_CONFIG', env.MAVEN_SETTINGS_CONFIG ?: 'nabla-settings-nexus').trim()
  vars.MAVEN_SETTINGS_SECURITY_CONFIG = vars.get('MAVEN_SETTINGS_SECURITY_CONFIG', env.MAVEN_SETTINGS_SECURITY_CONFIG ?: 'nabla-settings-security-nexus').trim()
  vars.K8S_SETTINGS_CONFIG = vars.get('K8S_SETTINGS_CONFIG', env.K8S_SETTINGS_CONFIG ?: 'nabla-k8s-default').trim()

  vars.KUBECONFIG = vars.get('KUBECONFIG', env.KUBECONFIG ?: '/home/jenkins/.kube/config').trim()
  vars.JENKINS_USER_HOME = vars.get('JENKINS_USER_HOME', env.JENKINS_USER_HOME ?: '/home/jenkins').trim()

  String MAVEN_SETTINGS_XML = ''
  String MAVEN_SETTINGS_SECURITY_XML = ''

  String NPM_SETTINGS = ''
  String BOWER_SETTINGS = ''

  vars.mavenHome = vars.get('mavenHome', "${vars.JENKINS_USER_HOME}/.m2/").trim()

  // configFileProvider is working on with getDockerOpts(isLocalJenkinsUser: true)
  vars.skipConfigFileProvider = vars.get('skipConfigFileProvider', false).toBoolean()

  if (!vars.skipConfigFileProvider) {
    configFileProvider([
      configFile(fileId: "${vars.NPM_SETTINGS_CONFIG}", targetLocation: "${vars.JENKINS_USER_HOME}/.npmrc", variable: 'NPM_SETTINGS'),
      configFile(fileId: "${vars.BOWER_SETTINGS_CONFIG}", targetLocation: "${vars.JENKINS_USER_HOME}/.bowerrc", variable: 'BOWER_SETTINGS'),
      configFile(fileId: "${vars.MAVEN_SETTINGS_CONFIG}", targetLocation: "${vars.mavenHome}/settings.xml", variable: 'MAVEN_SETTINGS_XML'),
      configFile(fileId: "${vars.MAVEN_SETTINGS_SECURITY_CONFIG}", targetLocation: "${vars.mavenHome}/settings-security.xml", variable: 'MAVEN_SETTINGS_SECURITY_XML'),
      configFile(fileId: "${vars.K8S_SETTINGS_CONFIG}",  targetLocation: "${vars.KUBECONFIG}", variable: 'K8S_SETTINGS'),
      // configFile(fileId: 'jenkins', targetLocation: 'id_rsa'),  // for draftPack
      ]) {
        if (body) {
        body()
        }
      } // configFileProvider
  } // skipConfigFileProvider
}
