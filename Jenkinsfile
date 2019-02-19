#!/usr/bin/env groovy
@Library('nabla-pipeline-scripts') _

def DOCKER_REGISTRY="hub.docker.com"
def DOCKER_ORGANISATION="nabla"
def DOCKER_TAG="latest"
def DOCKER_NAME="ansible-jenkins-slave-docker"

def DOCKER_REGISTRY_URL="https://${DOCKER_REGISTRY}"
def DOCKER_REGISTRY_CREDENTIAL='nabla'
def DOCKER_IMAGE="${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_NAME}:${DOCKER_TAG}"

def DOCKER_OPTS = [
  '--dns-search=misys.global.ad',
  '-v /etc/passwd:/etc/passwd:ro ',
  '-v /etc/group:/etc/group:ro '
].join(" ")

def branchName = env.BRANCH_NAME

pipeline {
  agent none
  options {
    disableConcurrentBuilds()
    skipStagesAfterUnstable()
    parallelsAlwaysFailFast()
    ansiColor('xterm')
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
  }
  stages {
    stage('Setup') {
      agent {
        docker {
          image DOCKER_IMAGE
          alwaysPull true
          reuseNode true
          registryUrl DOCKER_REGISTRY_URL
          registryCredentialsId DOCKER_REGISTRY_CREDENTIAL
          args DOCKER_OPTS
          label 'docker-compose'
        }
      }
      steps {
        script {
          properties(createPropertyList())
          setBuildName()
          RESULT = sh(returnStdout: true, script: "./build.sh").trim()

          echo "RESULT : ${RESULT}"

          parallel "sample default maven project": {
            build job: "github.com/AlbanAndrieu/nabla-servers-bower-sample/master",
            wait: true
          },
          "sample maven project": {
            build job: "github.com/AlbanAndrieu/nabla-servers-bower/master",
            wait: true
          }
        }
      }
    } // stage setup
    stage('SonarQube analysis') {
      agent {
        docker {
          image DOCKER_IMAGE
          alwaysPull true
          reuseNode true
          registryUrl DOCKER_REGISTRY_URL
          registryCredentialsId DOCKER_REGISTRY_CREDENTIAL
          args DOCKER_OPTS_COMPOSE
          label 'docker-compose'
        }
      }
      environment {
        SONAR_USER_HOME = "$WORKSPACE"
      }
      steps {
        script {
          withSonarQubeWrapper(verbose: true, skipMaven: true, project: "NABLA", repository: "nabla-pipeline-scripts") {

          }
        }
      } // steps
    } // stage SonarQube analysis
  } // stages
  post {
    always {
      node('docker-compose') {
        runHtmlPublishers(["LogParserPublisher", "AnalysisPublisher"])
        archiveArtifacts artifacts: "**/*.log", onlyIfSuccessful: false, allowEmptyArchive: true
      } // node

      wrapCleanWs()
    }
  } // post
} // pipeline
