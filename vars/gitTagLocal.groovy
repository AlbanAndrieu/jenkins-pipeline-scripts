#!/usr/bin/groovy

@Deprecated
def call(def tagName="LATEST_SUCCESSFULL", def message="Jenkins") {
    try {
        sh """
            git tag -l | xargs git tag -d # remove all local tags;
            #git tag --delete ${tagName};
            git tag -a ${tagName} -m '${message}';
        """
    }
    catch(exc) {
        echo 'Warning: There were errors in gitTagLocal. '+exc.toString()
        sh "git config --global --list && ls -lrta /home/jenkins/.gitconfig"
    }
}
