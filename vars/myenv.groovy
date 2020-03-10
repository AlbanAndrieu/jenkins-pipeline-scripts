#!/usr/bin/groovy

import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

import static com.test.jenkins.GlobalEnvironmentVariables.createGlobalEnvironmentVariables

//@groovy.transform.Field
//def DOCKER_REGISTRY_TMP =

//@groovy.transform.Field
//def DOCKER_LIST = [:]

def getPropertyList() {
	return createPropertyList()
}

def getbranchType() {

	def branchName = "${env.BRANCH_NAME}"
	if (branchName ==~ /release\/.+/ ) {
		return 'staging'
	} else if ( branchName ==~ /master|master_.+/ ) {
		return 'master'
	} else if ( branchName ==~ /PR-\d+/ ) {
		return 'pr'
	} else {
		return branchName
	}
}

@NonCPS
def defineEnvironment() {

	//createGlobalEnvironmentVariables('Var1','DummyValue')
	// if you have Failed to run image Error: docker: invalid reference format.: repository name must be lowercase
	createGlobalEnvironmentVariables('DOCKER_REGISTRY_TMP','registry-tmp.nabla.mobi').toLowerCase().trim()
	createGlobalEnvironmentVariables('DOCKER_REGISTRY','registry.nabla.mobi').toLowerCase().trim()
	createGlobalEnvironmentVariables('DOCKER_REGISTRY_TMP_URL',"https://${DOCKER_REGISTRY_TMP}").trim()
	createGlobalEnvironmentVariables('DOCKER_REGISTRY_URL',"https://${DOCKER_REGISTRY}").trim()
	createGlobalEnvironmentVariables('DOCKER_REGISTRY_CREDENTIAL','jenkins').trim()
	createGlobalEnvironmentVariables('DOCKER_ORGANISATION','nabla').trim()

	createGlobalEnvironmentVariables('COMPOSE_HTTP_TIMEOUT','200')

	createGlobalEnvironmentVariables('SONAR_INSTANCE','sonar').trim()
	createGlobalEnvironmentVariables('SONAR_SCANNER_OPTS','-Xmx2g').trim()
	createGlobalEnvironmentVariables('SONAR_USER_HOME',"$WORKSPACE").trim()
	createGlobalEnvironmentVariables('JENKINS_CREDENTIALS','jenkins-ssh').trim()

	createGlobalEnvironmentVariables('CLEAN_RUN',false)
	createGlobalEnvironmentVariables('DRY_RUN',false)
	createGlobalEnvironmentVariables('DEBUG_RUN',false)
	createGlobalEnvironmentVariables('RELEASE_VERSION',null)
	createGlobalEnvironmentVariables('RELEASE',false)
	createGlobalEnvironmentVariables('RELEASE_BASE',null)

	println GroovySystem.version

}

@NonCPS
def printEnvironment() {
	//def fields = env.getEnvironment()
	def fields = env.getEnvironment
	fields.each { key, value ->
		println("${key} = ${value}");
	}

	println(env.PATH)
}

return this
