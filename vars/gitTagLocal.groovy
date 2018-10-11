#!/usr/bin/groovy

def call(def tagName="LATEST_SUCCESSFULL", def message="Jenkins local") {
    sh """
        git config --global user.email "alban.andrieu@free.fr"
        git config --global user.name "Andrieu, Alban"
        git tag -l | grep -E "^${tagName}\$" && { git tag -d $tagName ; }
        git tag -a ${tagName} -m "${message}"
    """
}
