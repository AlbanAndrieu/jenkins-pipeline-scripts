#!/usr/bin/env groovy
@Library('jenkins-pipeline-scripts')
import com.test.jenkins.*

String DOCKER_REGISTRY="index.docker.io/v1".trim()
String DOCKER_ORGANISATION="nabla".trim()
String DOCKER_TAG="latest".trim()
String DOCKER_NAME="ansible-jenkins-slave-docker".trim()

String DOCKER_REGISTRY_URL="https://${DOCKER_REGISTRY}".trim()
String DOCKER_REGISTRY_CREDENTIAL=env.DOCKER_REGISTRY_CREDENTIAL ?: "hub-docker-nabla".trim()
String DOCKER_IMAGE="${DOCKER_ORGANISATION}/${DOCKER_NAME}:${DOCKER_TAG}".trim()

String DOCKER_NAME_BUILD="jenkins-pipeline-scripts-test".trim()
String DOCKER_BUILD_TAG=dockerTag("temp").trim()
String DOCKER_BUILD_IMG="${DOCKER_ORGANISATION}/${DOCKER_NAME_BUILD}:${DOCKER_BUILD_TAG}".trim()

def DOCKER_OPTS_COMPOSE = getDockerOpts(isDockerCompose: true)

def branchName = env.BRANCH_NAME

pipeline {
  //agent none
  agent {
    docker {
      image DOCKER_IMAGE
      alwaysPull true
      reuseNode true
      registryUrl DOCKER_REGISTRY_URL
      registryCredentialsId DOCKER_REGISTRY_CREDENTIAL
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
  }
  environment {
    DRY_RUN = "${params.DRY_RUN}".toBoolean()
    CLEAN_RUN = "${params.CLEAN_RUN}".toBoolean()
    DEBUG_RUN = "${params.DEBUG_RUN}".toBoolean()
    MVNW_VERBOSE = "${params.MVNW_VERBOSE}".toBoolean()
    RELEASE = "${params.RELEASE}".toBoolean()
    RELEASE_BASE = "${params.RELEASE_BASE}"
    RELEASE_VERSION = "${params.RELEASE_VERSION}"
    //SONAR_INSTANCE = "sonartest"
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
          if (env.CLEAN_RUN == true) {
            cleanWs(isEmailEnabled: false, disableDeferredWipeout: true, deleteDirs: true)
          }

          def myenv = load "src/test/jenkins/lib/myenv.groovy"
          properties(myenv.getPropertyList())

          //myenv.defineEnvironment()

          myenv.printEnvironment()

          setBuildName()
          RESULT = sh(returnStdout: true, script: './clean.sh').trim()

          echo "RESULT : ${RESULT}"

        }
      }
    } // stage setup
    stage('\u27A1 Build - Maven') {
      steps {
        script {

          if (env.CLEAN_RUN) {
              sh "$WORKSPACE/clean.sh"
          }

          //profile: "sonar,run-integration-test"

          sh "echo TEST : $branchName"
          //buildCmdParameters: "-Dserver=jetty9x -Dmaven.repo.local=./.repository"

          withMavenWrapper(goal: "install",
              profile: "jacoco",
              skipSonar: true,
              skipPitest: true,
              skipArtifacts: true,
              buildCmdParameters: "-Dserver=jetty9x -Dsonar.findbugs.allowuncompiledcode=true",
              mavenHome: "/home/jenkins/.m2/",
              artifacts: "**/target/dependency/jetty-runner.jar, **/target/test-config.jar, **/target/test.war, **/target/*.zip") {

                //sh 'chown -R jenkins:docker .[^.]* *'

          }

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

          recordIssues enabledForFailure: true, tool: checkStyle()
          recordIssues enabledForFailure: true, tool: cpd(pattern: '**/target/cpd.xml')
          recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
          //recordIssues enabledForFailure: true, tool: pit()
          //taskScanner()
          recordIssues enabledForFailure: true,
                     aggregatingResults: true,
                     id: "analysis-java",
                     tools: [mavenConsole(), java(reportEncoding: 'UTF-8'), javaDoc(),
                             spotBugs(),
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
      steps {
        script {

          if (env.CLEAN_RUN) {
              sh "$WORKSPACE/clean.sh"
          }

          sh './gradlew build --stacktrace'

          publishHTML([reportDir: 'build/reports/tests/test', reportFiles: 'index.html', reportName: 'HTML Report'])

        } // script
      } // steps
    } // stage Maven
    stage('\u2795 Quality - SonarQube analysis') {
      environment {
        SONAR_USER_HOME = "$WORKSPACE"
      }
      steps {
        script {
          withSonarQubeWrapper(verbose: true,
              skipFailure: false,
              skipSonarCheck: false,
              skipMaven: true,
              buildCmdParameters: "-Dsonar.findbugs.allowuncompiledcode=true",
              isScannerHome: false,
              sonarExecutable: "/usr/local/sonar-runner/bin/sonar-scanner",
              reportTaskFile: ".scannerwork/report-task.txt",
              project: "NABLA",
              repository: "jenkins-pipeline-scripts") {
          }
        }
      } // steps
    } // stage SonarQube analysis
    stage('\u27A1 Build - Docker') {
        //environment {
        //    CST_CONFIG = "docker/ubuntu18/config-BUILD.yaml"
        //}
        when {
            expression { env.BRANCH_NAME ==~ /release\/.+|master|develop|PR-.*|feature\/.*|bugfix\/.*/ }
        }
        steps {
            script {

                tee("docker-build.log") {

                    // this give the registry
                    // sh(returnStdout: true, script: "echo ${DOCKER_BUILD_IMG} | cut -d'/' -f -1").trim()
                    DOCKER_BUILD_ARGS = ["--build-arg JENKINS_USER_HOME=/home/jenkins --build-arg=MICROSCANNER_TOKEN=NzdhNTQ2ZGZmYmEz"].join(" ")
                    if (env.CLEAN_RUN) {
                        DOCKER_BUILD_ARGS = ["--no-cache",
                                             "--pull",
                                             ].join(" ")
                    }
                    DOCKER_BUILD_ARGS = [ "${DOCKER_BUILD_ARGS}",
                                          "--target build", // See issue https://issues.jenkins-ci.org/browse/JENKINS-44609?page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel&showAll=true
                                          "--label 'version=1.0.0'",
                                        ].join(" ")

                    //def container = docker.build("${DOCKER_BUILD_IMG}", "${DOCKER_BUILD_ARGS} . ")
                    //container.inside {
                    //    sh 'echo DEBUGING image : $PATH'
                    //    sh 'git --version || true'
                    //    sh 'java -version || true'
                    //    sh 'id jenkins || true'
                    //    sh 'ls -lrta'
                    //    //sh 'ls -lrta /home/jenkins/ || true'
                    //    sh 'date > /tmp/test.txt'
                    //    sh "cp /tmp/test.txt ${WORKSPACE}"
                    //    sh "cp ${HOME}/microscanner.log ${WORKSPACE} || true"
                    //    archiveArtifacts artifacts: 'test.txt, *.log', excludes: null, fingerprint: false, onlyIfSuccessful: false
                    //}

                    //dockerFingerprintFrom dockerfile: 'docker/ubuntu16/Dockerfile', image: "${DOCKER_BUILD_IMG}"

                } // tee

            } // script
        } // steps
    } // Build - Docker
    stage('\u2795 Quality - E2E tests') {
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
    //stage('\u2795 Quality - Security - Checkmarx') {
    //  steps {
    //    script {
	//       withCheckmarxWrapper(projectName: 'jenkins-pipeline-scripts',
	//     	                    preset: '1',
	//     	                    groupId: '1234',
	//     	                    lowThreshold: 10,
	//     	                    mediumThreshold: 0,
	//     	                    highThreshold: 0)
    //    } // script
    //  } // steps
    //} // stage Security
  } // stages
  post {
    always {
      node('any') {
        runHtmlPublishers(["LogParserPublisher", "AnalysisPublisher"])
      }
    } // always
    cleanup {
      wrapCleanWsOnNode(isEmailEnabled: false)
    } // cleanup
  } // post
} // pipeline
