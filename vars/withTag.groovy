#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withTag.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    //gitChangelog from: [type: 'REF', value: 'refs/remotes/origin/develop'], ignoreCommitsWithoutIssue: true, returnType: 'STRING', to: [type: 'REF', value: 'refs/tags/LATEST_SUCCESSFULL']
    TARGET_PROJECT = sh(returnStdout: true, script: "echo ${env.JOB_NAME} | cut -d'/' -f 2").trim().toUpperCase()

    setBuildName()
    createVersionTextFile("${TARGET_PROJECT} ${env.BRANCH_NAME}","${TARGET_PROJECT}_VERSION.TXT")

    if (!DRY_RUN && !RELEASE) {

        echo "Tag repo to git"

        //utils.manualPromotion()

        if (isReleaseBranch()) {
            def TARGET_TAG = getSemVerReleasedVersion() ?: "LATEST"
            
            def tagName="${TARGET_TAG}_SUCCESSFULL"
            def message="Jenkins"
            def remote="origin"

            try {
                sh """
                    git config --global user.email "jenkins@nabla.mobi";
                    git config --global user.name "jenkins";
                    git tag -l | xargs git tag -d # remove all local tags;
                    git push --delete ${remote} ${tagName} || echo "Could not delete remote tag: does not exist or no access rights" || true;
                    git tag --delete ${tagName} || echo "Could not delete local tag: does not exist or no access rights" || true; # remove local tag
                    git fetch --tags --prune > /dev/null 2>&1 || true;
                    git tag -a ${tagName} -m '${message}'; # create new tag
                    git push ${remote} ${tagName} --force || echo "Could not push tag: invalid name or no access rights";            
                """
            } catch(exc) {
                echo 'Warning: There were errors while tagging. '+exc.toString()
                sh "git config --global --list && ls -lrta /home/jenkins/.gitconfig"
            }    

        }
    } // if DRY_RUN

    if (body) { body() }

}
