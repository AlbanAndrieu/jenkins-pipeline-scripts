#!/usr/bin/groovy

// TODO
def gitClone(repoUrl, relativeTargetDir) {
	git_cmd = sh (
		script: "git checkout ${repoUrl} ${relativeTargetDir}",
		returnStdout: true
	).trim()
    return git_cmd
}

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/gitCheckoutTESTRepo.groovy`"

    vars = vars ?: [:]

    def GIT_REPO_PROJECT = vars.get("GIT_PROJECT_TEST", "NABLA").trim()
    def GIT_PROJECT_TEST = vars.get("GIT_PROJECT_TEST", "nabla-servers-bower-sample").trim()
    def GIT_BROWSE_URL_TEST = vars.get("GIT_BROWSE_URL_TEST", "https://github.com/AlbanAndrieu/${GIT_PROJECT_TEST}/")
    def GIT_URL_TEST = vars.get("GIT_URL_TEST", "https://github.com/AlbanAndrieu/${GIT_PROJECT_TEST}.git")
    def JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", 'jenkins-https') 
    //def GIT_URL_TEST = vars.get("GIT_URL", "ssh://git@github.com:AlbanAndrieu/${GIT_REPO_PROJECT}/${GIT_PROJECT}.git")
    //def JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", "jenkins-ssh")

    def relativeTargetDir = vars.get("relativeTargetDir", GIT_PROJECT_TEST)
    def isDefaultBranch = vars.get("isDefaultBranch", false).toBoolean()
    def isScmEnabled = vars.get("isScmEnabled", true).toBoolean()

    //echo "isDefaultBranch=" + isDefaultBranch

    def GIT_BRANCH_NAME = vars.get("GIT_BRANCH_NAME", "develop")

    if (isScmEnabled) {

       checkout([
           $class: 'GitSCM',
           branches: getDefaultCheckoutBranches(isDefaultBranch, GIT_BRANCH_NAME),
           browser: [
               $class: 'Stash',
               repoUrl: "${GIT_BROWSE_URL_TEST}"],
           doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
           extensions: getDefaultCheckoutExtensions(isDefaultBranch, relativeTargetDir),
           gitTool: 'git-latest',
           submoduleCfg: [],
           userRemoteConfigs: [[
               credentialsId: "${JENKINS_CREDENTIALS}",
               url: "${GIT_URL_TEST}"]
           ]
       ])

       if (body) { body() }

    } else {
        echo "scm disabled, using shell!"
        // This is a workaround because of the timeout which cannot be extended in jenkins

        // TODO
        gitClone(GIT_BROWSE_URL_TEST, relativeTargetDir)

        if (body) { body() }
    }
}
