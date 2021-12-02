#!/usr/bin/groovy

@Deprecated
def call(def tagName='LATEST_SUCCESSFUL', def message='Jenkins') {
  def JENKINS_USER_HOME = vars.get('JENKINS_USER_HOME', env.JENKINS_USER_HOME ?: '/home/jenkins')

  try {
    sh """#!/bin/bash -l
        git tag -l | xargs git tag -d # remove all local tags;
        #git tag --delete ${tagName};
        git tag -a ${tagName} -m '${message}';
        """
  }
    catch (exc) {
    echo 'Warning: There were errors in gitTagLocal. ' + exc
    sh "git config --global --list && ls -lrta ${JENKINS_USER_HOME}/.gitconfig"
    }
}
