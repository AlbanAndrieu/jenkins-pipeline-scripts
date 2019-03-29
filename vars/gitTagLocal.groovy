#!/usr/bin/groovy

def call(def tagName="LATEST_SUCCESSFULL", def message="Jenkins local") {
    try {
        sh """
            git config --global user.email "alban.andrieu@free.fr"
            git config --global user.name "Andrieu, Alban"
            git tag -l | grep -E "^${tagName}\$" && { git tag -d $tagName ; }
            git tag -a ${tagName} -m "${message}"
        """
    }
    catch(exc) {
        echo 'Error: There were errors in gitTagLocal. '+exc.toString()
        sh "git config --global --list && ls -lrta /home/jenkins/.gitconfig"
    }
}
