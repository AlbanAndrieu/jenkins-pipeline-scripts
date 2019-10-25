#!/usr/bin/groovy

@Deprecated
def call(def tagName="LATEST_SUCCESSFULL", def remote="origin") {

    def JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins")
    
    // Push empty reference ( :tag) to delete remote tag
    // Assumes that remote is consistently named to origin
    try {
        sh """
            git push --delete ${remote} ${tagName} || echo "Could not delete remote tag: does not exist or no access rights" || true;
            git tag --delete ${tagName}
            git fetch --tags --prune > /dev/null 2>&1;
            git push ${remote} ${tagName} --force || echo "Could not push tag: invalid name or no access rights";
        """
    }
    catch(exc) {
        echo 'Warning: There were errors in gitTagRemote. '+exc.toString()
        sh "git config --global --list && ls -lrta ${JENKINS_USER_HOME}/.gitconfig"
    }
}
