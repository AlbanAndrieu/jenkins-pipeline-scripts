#!/usr/bin/groovy

def call(def tagName="LATEST_SUCCESSFULL", def message="Jenkins local") {
    sh """
        //git config --global user.email "todo@test.com"
        g//it config --global user.name "Test"
        git tag -l | grep -E "^${tagName}\$" && { git tag -d $tagName ; }
        git tag -a ${tagName} -m "${message}"
    """
}
