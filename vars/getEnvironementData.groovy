#!/usr/bin/groovy

def call(def filePath = "step-2-0-0-build-env.sh") {
    ansiColor('xterm') {
sh '''
#set -e
set -xv

${filePath}

exit 0
'''
    } //ansiColor

    load "./jenkins-env.groovy"
}
