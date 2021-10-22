#!/usr/bin/env groovy
@Library('jenkins-pipeline-scripts@master')

String DOCKER_REGISTRY_HUB=env.DOCKER_REGISTRY_HUB ?: "registry.hub.docker.com".trim()
String DOCKER_ORGANISATION_HUB=env.DOCKER_ORGANISATION_HUB ?: "nabla".trim()

String DOCKER_NAME="ansible-jenkins-slave-docker".trim()

String DOCKER_REGISTRY_HUB_URL=env.DOCKER_REGISTRY_HUB_URL ?: "https://${DOCKER_REGISTRY_HUB}".trim()
String DOCKER_REGISTRY_HUB_CREDENTIAL=env.DOCKER_REGISTRY_HUB_CREDENTIAL ?: "hub-docker-nabla".trim()

String DOCKER_IMAGE_TAG=dockerImageTag(isLatest: true)
String DOCKER_IMAGE="${DOCKER_ORGANISATION_HUB}/${DOCKER_NAME}:${DOCKER_IMAGE_TAG}".trim()

String DOCKER_NAME_BUILD="jenkins-pipeline-scripts-test".trim()
String DOCKER_BUILD_TAG=dockerTag().trim()
String DOCKER_BUILD_IMG="${DOCKER_ORGANISATION_HUB}/${DOCKER_NAME_BUILD}:${DOCKER_BUILD_TAG}".trim()

String DOCKER_OPTS_COMPOSE = getDockerOpts(isDockerCompose: false, isLocalJenkinsUser: false)

pipeline {
  //agent none
  agent {
    docker {
      image DOCKER_IMAGE
      alwaysPull true
      reuseNode true
      registryUrl DOCKER_REGISTRY_HUB_URL
      registryCredentialsId DOCKER_REGISTRY_HUB_CREDENTIAL
      args DOCKER_OPTS_COMPOSE
      label 'molecule'
    }
  }
  parameters {
    booleanParam(defaultValue: false, description: 'Dry run', name: 'DRY_RUN')
    booleanParam(defaultValue: false, description: 'Clean before run', name: 'CLEAN_RUN')
    booleanParam(defaultValue: false, description: 'Debug run', name: 'DEBUG_RUN')
    booleanParam(defaultValue: false, description: 'Debug mvnw', name: 'MVNW_VERBOSE')
    booleanParam(defaultValue: false, name: "RELEASE", description: "Perform release-type build.")
    string(defaultValue: "", name: "RELEASE_BASE", description: "Commit tag or branch that should be checked-out for release")
    string(defaultValue: "1.0.0", name: "RELEASE_VERSION", description: "Release version for artifacts")
    booleanParam(defaultValue: false, description: 'Build only to have package. no test / no docker', name: 'BUILD_ONLY')
    booleanParam(defaultValue: false, description: 'Run acceptance tests', name: 'BUILD_TEST')
    booleanParam(defaultValue: false, description: 'Build gradle', name: 'BUILD_GRADLE')
    booleanParam(defaultValue: false, description: 'Build jenkins docker images', name: 'BUILD_DOCKER')
    booleanParam(defaultValue: false, description: 'Build with sonar', name: 'BUILD_SONAR')
  }
  options {
    disableConcurrentBuilds()
    //skipStagesAfterUnstable()
    parallelsAlwaysFailFast()
    ansiColor('xterm')
    timeout(time: 180, unit: 'MINUTES')
    timestamps()
  }
  stages {
    stage('Setup') {
      steps {
        script {
          setUp(description: "JPL")

          def myenv = load "src/test/jenkins/lib/myenv.groovy"
          properties(myenv.getPropertyList())

          //myenv.defineEnvironment()

          myenv.printEnvironment()

        }
      }
    } // stage setup
    stage('\u27A1 Build - Maven') {
      when {
        expression { env.BRANCH_NAME ==~ /release\/.+|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
      }
      steps {
        script {

          def branchName = env.BRANCH_NAME
          sh "echo TEST : $branchName"

          withMavenWrapper(goal: "install",
              //profile: "jacoco",
              skipSonar: true,
              skipPitest: true,
              skipArtifacts: true,
              buildCmdParameters: "-Dserver=jetty9x",
              //mavenHome: "/home/jenkins/.m2/",
              skipMavenSettings: false,
              artifacts: "**/target/dependency/jetty-runner.jar, **/target/test-config.jar, **/target/test.war, **/target/*.zip")

          withShellCheckWrapper(pattern: "*.sh")

          step([
              $class: 'CoberturaPublisher',
              autoUpdateHealth: false,
              autoUpdateStability: false,
              coberturaReportFile: '**/coverage.xml',
              //coberturaReportFile: 'target/site/cobertura/coverage.xml'
              failUnhealthy: false,
              failUnstable: false,
              failNoReports: false,
              maxNumberOfBuilds: 0,
              onlyStable: false,
              sourceEncoding: 'ASCII',
              zoomCoverageChart: false
              ])

          //recordIssues enabledForFailure: true, tool: pit()
          //taskScanner()
          recordIssues enabledForFailure: true,
                     aggregatingResults: true,
                     id: "analysis-java-jps",
                     tools: [mavenConsole(),
                             java(reportEncoding: 'UTF-8'),
                             javaDoc(),
                             spotBugs(),
                             checkStyle(),
                             cpd(pattern: '**/target/cpd.xml'),
                             pmdParser(pattern: '**/target/pmd.xml')
                     ],
                     filters: [excludeFile('.*\\/target\\/.*'),
                               excludeFile('node_modules\\/.*'),
                               excludeFile('npm\\/.*'),
                               excludeFile('bower_components\\/.*')]

          //jacoco buildOverBuild: false, changeBuildStatus: false, execPattern: '**/target/**-it.exec'

          //perfpublisher healthy: '', metrics: '', name: '**/target/surefire-reports/**/*.xml', threshold: '', unhealthy: ''

        } // script
      } // steps
    } // stage Maven
    stage('\u27A1 Build - Gradle') {
      when {
        expression { env.BRANCH_NAME ==~ /release\/.+|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
        expression { params.BUILD_GRADLE == true }
      }
      steps {
        script {

          if (env.CLEAN_RUN) {
              sh "$WORKSPACE/clean.sh"
          }

          sh "echo JAVA_HOME : $JAVA_HOME"
          //sh "echo JENKINS_USER_HOME : $JENKINS_USER_HOME"
          sh "echo HOME : $HOME"

          sh "pwd && ls -lrta /jenkins/ || true"
          sh "ls -lrta /jenkins/.gradle || true"
          sh "mkdir -p /jenkins/.gradle || true"
          sh "export HOME=/jenkins/home && ./gradlew build --stacktrace || true"

          publishHTML (target: [
            allowMissing: true,
            alwaysLinkToLastBuild: false,
            keepAll: true,
            reportDir: 'build/reports/tests/test/',
            reportFiles: 'index.html',
            reportName: "Gradle Report"
          ])

        } // script
      } // steps
    } // stage Maven
    stage('\u2795 Quality - SonarQube analysis') {
      when {
        expression { env.BRANCH_NAME ==~ /release\/.+|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
        expression { params.BUILD_ONLY == false && params.BUILD_SONAR == true }
      }
      environment {
        SONAR_USER_HOME = "$WORKSPACE"
      }
      steps {
        script {
          withSonarQubeWrapper(verbose: true,
            skipFailure: false,
            skipSonarCheck: false,
            skipMaven: true,
            isScannerHome: false,
            sonarExecutable: "/usr/local/sonar-runner/bin/sonar-scanner",
            reportTaskFile: ".scannerwork/report-task.txt",
            project: "NABLA",
            repository: "jenkins-pipeline-scripts")
        }
      } // steps
    } // stage SonarQube analysis
    stage('\u27A1 Build - Docker') {
        when {
          expression { env.BRANCH_NAME ==~ /release\/.+|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
          expression { params.BUILD_ONLY == false && params.BUILD_DOCKER == true }
        }
        steps {
          script {

              tee("docker-build.log") {

                dockerHadoLint(dockerFilePath: "./", skipDockerLintFailure: true, dockerFileId: "1")

                // this give the registry
                DOCKER_BUILD_ARGS = ["--build-arg JENKINS_USER_HOME=/home/jenkins --build-arg=MICROSCANNER_TOKEN=NzdhNTQ2ZGZmYmEz"].join(" ")
                //DOCKER_BUILD_ARGS += getDockerProxyOpts()
                if (env.CLEAN_RUN) {
                  DOCKER_BUILD_ARGS += ["--no-cache",
                                       "--pull",
                                       ].join(" ")
                }
                DOCKER_BUILD_ARGS += [ " --label 'version=1.0.0'",
                                     ].join(" ")

                def container = docker.build("${DOCKER_BUILD_IMG}", "${DOCKER_BUILD_ARGS} . ")
                container.inside {
                  sh 'echo DEBUGING image : $PATH'
                  sh 'git --version || true'
                  sh 'java -version || true'
                  sh 'id jenkins || true'
                  sh 'ls -lrta'
                  //sh 'ls -lrta /home/jenkins/ || true'
                  sh 'date > /tmp/test.txt'
                  sh "cp /tmp/test.txt ${WORKSPACE}"
                  sh "cp ${HOME}/microscanner.log ${WORKSPACE} || true"
                  archiveArtifacts artifacts: 'test.txt, *.log', excludes: null, fingerprint: false, onlyIfSuccessful: false
                }

                dockerFingerprintFrom dockerfile: './Dockerfile', image: "${DOCKER_BUILD_IMG}"

              } // tee

          } // script
        } // steps
    } // Build - Docker
    stage('\u2795 Quality - E2E tests') {
      when {
        expression { env.BRANCH_NAME ==~ /release\/.+|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
        expression { params.BUILD_TEST == true && params.BUILD_ONLY == false }
      }
      steps {
        script {
          try {
            parallel "sample default maven project": {
              def e2e = build job: 'github.com/AlbanAndrieu/nabla-servers-bower-sample/master', propagate: false, wait: true
              result = e2e.result
              if (result.equals("SUCCESS")) {
                echo "E2E SUCCESS"
              } else {
                 echo "E2E UNSTABLE"
                 error 'FAIL E2E'
                 currentBuild.result = 'UNSTABLE'
                 //sh "exit 1" // this fails the stage
              }
            } // parallel
          } catch (exc) {
            echo "E2E FAILURE"
            //currentBuild.result = 'FAILURE'
            //build = "FAIL" // make sure other exceptions are recorded as failure too
            echo "WARNING : There was a problem with e2e job test " + exc.toString()
          }
        }
      } // steps
    } // stage SonarQube analysis
  } // stages
  post {
    always {
      recordIssues enabledForFailure: true,
        tools: [taskScanner(),
                tagList()
        ]

      //archiveArtifacts allowEmptyArchive: true, artifacts: '*.log. *.json', excludes: null, fingerprint: false, onlyIfSuccessful: false

      node('any||flyweight') {
        withLogParser(unstableOnWarning: false)
      }

    } // always
    //cleanup {
    //  wrapCleanWs(isEmailEnabled: false)
    //} // cleanup
  } // post
} // pipeline
