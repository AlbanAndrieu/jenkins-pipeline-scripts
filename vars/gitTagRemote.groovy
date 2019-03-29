#!/usr/bin/groovy

def call(def tagName="LATEST_SUCCESSFULL", def remote="origin") {
    // Push empty reference ( :tag) to delete remote tag
    // Assumes that remote is consistently named to origin
    try {
        sh """
            git push ${remote} :${tagName} || echo "Could not delete tag: does not exist or no access rights"
            git push ${remote} ${tagName} --force || echo "Could not push tag: invalid name or no access rights"
        """
    }
    catch(exc) {
        echo 'Error: There were errors in gitTagRemote. '+exc.toString()
        sh "git config --global --list && ls -lrta /home/jenkins/.gitconfig"
    }    
}
