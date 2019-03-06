#!/usr/bin/env groovy
@Library('jenkins-pipeline-scripts') _

def DOCKER_REGISTRY="hub.docker.com"
def DOCKER_ORGANISATION="nabla"
def DOCKER_TAG="latest"
def DOCKER_NAME="ansible-jenkins-slave-docker"

def DOCKER_REGISTRY_URL="https://${DOCKER_REGISTRY}"
def DOCKER_REGISTRY_CREDENTIAL='nabla'
def DOCKER_IMAGE="${DOCKER_REGISTRY}/${DOCKER_ORGANISATION}/${DOCKER_NAME}:${DOCKER_TAG}"

def DOCKER_OPTS_ROOT = [
    '-v /etc/passwd:/etc/passwd:ro',
    '-v /etc/group:/etc/group:ro',
    '--entrypoint=\'\'',    
].join(" ")

def DOCKER_OPTS_BASIC = [
    '--dns-search=nabla.mobi',
    '-v /usr/local/sonar-build-wrapper:/usr/local/sonar-build-wrapper',
    '-v /workspace/slave/tools/:/workspace/slave/tools/',
    '-v /jenkins:/home/jenkins',
    DOCKER_OPTS_ROOT,   
].join(" ")

def DOCKER_OPTS_COMPOSE = [
    DOCKER_OPTS_BASIC,
    '-v /var/run/docker.sock:/var/run/docker.sock',
].join(" ")


def branchName = env.BRANCH_NAME

pipeline {
  agent none
  parameters {
    booleanParam(defaultValue: false, description: 'Dry run', name: 'DRY_RUN')
    booleanParam(defaultValue: true, description: 'Clean before run', name: 'CLEAN_RUN')
    booleanParam(defaultValue: false, description: 'Debug run', name: 'DEBUG_RUN')
    booleanParam(defaultValue: false, description: 'Debug mvnw', name: 'MVNW_VERBOSE')
    booleanParam(defaultValue: false, name: "RELEASE", description: "Perform release-type build.")
    string(defaultValue: "", name: "RELEASE_BASE", description: "Commit tag or branch that should be checked-out for release")
    string(defaultValue: "", name: "RELEASE_VERSION", description: "Release version for artifacts")
  }  
  environment {
    DRY_RUN = "${params.DRY_RUN}".toBoolean()
    CLEAN_RUN = "${params.CLEAN_RUN}".toBoolean()
    DEBUG_RUN = "${params.DEBUG_RUN}".toBoolean()
    MVNW_VERBOSE = "${params.MVNW_VERBOSE}".toBoolean()
    RELEASE = "${params.RELEASE}".toBoolean()
    RELEASE_BASE = "${params.RELEASE_BASE}"
    RELEASE_VERSION = "${params.RELEASE_VERSION}"
  }    
  options {
    disableConcurrentBuilds()
    skipStagesAfterUnstable()
    parallelsAlwaysFailFast()
    ansiColor('xterm')
    timeout(time: 180, unit: 'MINUTES')
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
          args DOCKER_OPTS_COMPOSE
          label 'docker-compose'
        }
      }
      environment {
        JENKINS_URL = "http://localhost/jenkins/"
      }
      steps {
        script {
          properties(createPropertyList())
          setBuildName()
          RESULT = sh(returnStdout: true, script: "./build.sh").trim()

          echo "RESULT : ${RESULT}"

        }
      }
    } // stage setup
    stage('\u27A1 Build - Maven') {
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
      steps {
        script {

          if (CLEAN_RUN) {
              sh "$WORKSPACE/clean.sh"
          }

          //profile: "sonar,run-integration-test"

          sh "ls -lrtan /home/jenkins/ || true"
          //buildCmdParameters: "-Dserver=jetty9x -Dmaven.repo.local=./.repository"

          withMavenWrapper(goal: "install",
              profile: "jacoco",
              skipSonar: true,
              skipPitest: true,
              buildCmdParameters: "-Dserver=jetty9x",
              artifacts: "**/target/dependency/jetty-runner.jar, **/target/test-config.jar, **/target/test.war, **/target/*.zip") {

                //sh 'chown -R jenkins:docker .[^.]* *'

          }

          withShellCheckWrapper(pattern: "*.sh")

          //jacoco buildOverBuild: false, changeBuildStatus: false, execPattern: '**/target/**-it.exec'

          //perfpublisher healthy: '', metrics: '', name: '**/target/surefire-reports/**/*.xml', threshold: '', unhealthy: ''

        } // script
      } // steps
    } // stage Maven    
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
          withSonarQubeWrapper(verbose: true, skipMaven: true, project: "NABLA", repository: "jenkins-pipeline-scripts") {

          }

          parallel "sample default maven project": {
            build job: "github.com/AlbanAndrieu/nabla-servers-bower-sample/master",
            wait: true
          },
          "sample maven project": {
            build job: "github.com/AlbanAndrieu/nabla-servers-bower/master",
            wait: true
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
