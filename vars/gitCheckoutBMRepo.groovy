#!/usr/bin/groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/gitCheckoutBMRepo.groovy`"

    vars = vars ?: [:]

    def GIT_REPO_PROJECT = vars.get("GIT_PROJECT_TEST", "NABLA").trim()
    def GIT_PROJECT = vars.get("GIT_PROJECT", "nabla").trim()
    def GIT_BROWSE_URL = vars.get("GIT_BROWSE_URL", "https://github.com/AlbanAndrieu//${GIT_PROJECT}/").trim()
    def GIT_URL = vars.get("GIT_URL", "https://github.com/AlbanAndrieu/${GIT_PROJECT}.git").trim()
    def JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", 'stash-jenkins').trim()
    //def GIT_URL = vars.get("GIT_URL", "ssh://git@github.com:AlbanAndrieu/${GIT_PROJECT}.git").trim()
    //def JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", 'jenkins-ssh')

    vars.relativeTargetDir = vars.get("relativeTargetDir", GIT_PROJECT).trim()
    vars.isDefaultBranch = vars.get("isDefaultBranch", false).toBoolean()
    vars.gitDefaultBranchName = vars.get("gitDefaultBranchName", "master").trim()

    vars.timeout = vars.get("timeout", 20)
    //vars.isCleaningEnabled = vars.get("isCleaningEnabled", false).toBoolean()
    vars.isShallowEnabled = vars.get("isShallowEnabled", false).toBoolean()
    //vars.noTags = vars.get("noTags", false).toBoolean()

    vars.GIT_BRANCH_NAME_BUILDMASTER = vars.get("GIT_BRANCH_NAME_BUILDMASTER", "develop")

    echo "GIT_URL : ${GIT_URL}"

    checkout([
        $class: 'GitSCM',
        //branches: scm.branches,
        //branches: [[name: vars.GIT_BRANCH_NAME_BUILDMASTER]],
        branches: getDefaultCheckoutBranches(vars),
        browser: [
            $class: 'Stash',
            repoUrl: "${GIT_BROWSE_URL}"],
        doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
        //doGenerateSubmoduleConfigurations: false,
        extensions: getDefaultCheckoutExtensions(vars),
        gitTool: 'git-latest',
        submoduleCfg: [],
        userRemoteConfigs: [[
            credentialsId: "${JENKINS_CREDENTIALS}",
            url: "${GIT_URL}"]
        ]
    ])

    if (body) { body() }

}
