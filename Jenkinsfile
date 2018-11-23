#!/usr/bin/env groovy
@Library('jenkins-pipeline-scripts@develop') _

def daysToKeep     = isReleaseBranch() ? '30' : '10'
def numToKeep      = isReleaseBranch() ? '20' : '5'
def artifactDaysToKeep = isReleaseBranch() ? '30' : '10'
def artifactNumToKeep  = isReleaseBranch() ? '3'  : '1'
def cronString     = isReleaseBranch() ? 'H H 1 * *' : ''

def DOCKER_REGISTRY="hub.docker.com"
def DOCKER_TAG="latest"
def DOCKERNAME="ansible-jenkins-slave-docker"

def DOCKER_REGISTRY_URL="https://${DOCKER_REGISTRY}"
def DOCKER_REGISTRY_CREDENTIAL='nabla'
def DOCKER_IMAGE="${DOCKER_REGISTRY}/aandrieu/${DOCKERNAME}:1.0.0"

def DOCKER_OPTS = [
  '--dns-search=misys.global.ad',
  '-v /etc/passwd:/etc/passwd:ro ',
  '-v /etc/group:/etc/group:ro '
].join(" ")

def branchName = env.BRANCH_NAME

pipeline {
  agent none
  triggers {
      cron(cronString)
  }
  options {
      disableConcurrentBuilds()
      ansiColor('xterm')
      timeout(time: 360, unit: 'MINUTES')
      timestamps()
      buildDiscarder(
          logRotator(
              daysToKeepStr: daysToKeep,
              numToKeepStr: numToKeep,
              artifactDaysToKeepStr: artifactDaysToKeep,
              artifactNumToKeepStr: artifactNumToKeep
              )
      )
  }
  stages {
      stage('Setup') {
          agent {
              docker {
                  image DOCKER_IMAGE
                  reuseNode true
                  registryUrl DOCKER_REGISTRY_URL
                  registryCredentialsId DOCKER_REGISTRY_CREDENTIAL
                  args DOCKER_OPTS
                  label 'docker-inside'
              }
          }
          steps {
              script {
                  //if (isReleaseBranch()) {

                  RESULT = sh(returnStdout: true, script: "./build.sh").trim()

                  echo "RESULT : ${RESULT}"

                  branchName = branchName.replaceAll(/\\//, '%2F')

                  parallel "sample default maven project": {
                                   build job: "github.com/AlbanAndrieu/nabla-servers-bower-sample/${branchName}",
                                   wait: true
                           },
                           "sample maven project": {
                                   build job: "github.com/AlbanAndrieu/nabla-servers-bower/${branchName}",
                                   wait: true
                           }
                  //}
              }
          }
      } // stage setup
  } // stages
} // pipeline
