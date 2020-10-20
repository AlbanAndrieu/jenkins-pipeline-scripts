#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withMavenWrapper.groovy`"

    vars = vars ?: [:]

    String MAVEN_OPTS_DEFAULT = ["-Djava.awt.headless=true",
        //"-Dsun.zip.disableMemoryMapping=true",
        //"-Dmaven.repo.local=./.repository",
        //"-Djava.io.tmpdir=./target/tmp",
        ].join(" ")

    getJenkinsOpts(vars)

    vars.MAVEN_SETTINGS_CONFIG = vars.get("MAVEN_SETTINGS_CONFIG", env.MAVEN_SETTINGS_CONFIG ?: "nabla-settings-nexus").trim()
    vars.MAVEN_SETTINGS_SECURITY_CONFIG = vars.get("MAVEN_SETTINGS_SECURITY_CONFIG", env.MAVEN_SETTINGS_SECURITY_CONFIG ?: "nabla-settings-security-nexus").trim()
    vars.MAVEN_VERSION = vars.get("MAVEN_VERSION", env.MAVEN_VERSION ?: "maven 3.5.2").trim() // maven-latest
    //def JENKINS_USER_HOME = vars.get("JENKINS_USER_HOME", env.JENKINS_USER_HOME ?: "/home/jenkins").trim()
    vars.JDK_VERSION = vars.get("JDK_VERSION", env.JDK_VERSION ?: "jdk8").trim() // java-latest

    String MAVEN_SETTINGS_XML = ""
    String MAVEN_SETTINGS_SECURITY_XML = ""

    String NPM_SETTINGS = ""
    String BOWER_SETTINGS = ""
    if (vars.DEBUG_RUN) {
         echo "debug added"
         MAVEN_OPTS_DEFAULT = ["${MAVEN_OPTS_DEFAULT}",
             "-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError",
         ].join(" ")
    }

    vars.MAVEN_OPTS = vars.get("MAVEN_OPTS", "${MAVEN_OPTS_DEFAULT}")

    echo "Maven OPTS have been specified: ${vars.MAVEN_OPTS} - ${vars.CLEAN_RUN}/${vars.DRY_RUN}/${vars.DEBUG_RUN} - ${vars.SONAR_INSTANCE}"

    vars.goal = vars.get("goal", "install").trim()
    vars.profile = vars.get("profile", "sonar").trim()
    vars.skipTests = vars.get("skipTests", false).toBoolean()
    vars.skipResults = vars.get("skipResults", false).toBoolean()
    //vars.buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
    vars.buildCmd = vars.get("buildCmd", "-B -e").trim()
    vars.skipSonar = vars.get("skipSonar", false).toBoolean()
    vars.skipPitest = vars.get("skipPitest", false).toBoolean()
    vars.buildCmdParameters = vars.get("buildCmdParameters", "").trim()
    vars.artifacts = vars.get("artifacts", ['*_VERSION.TXT', '**/target/*.jar'].join(', '))
    vars.skipArtifacts = vars.get("skipArtifacts", false).toBoolean()
    vars.skipFailure = vars.get("skipFailure", false).toBoolean()
    vars.skipDeploy = vars.get("skipDeploy", true).toBoolean()
    vars.skipMavenSettings = vars.get("skipMavenSettings", true).toBoolean()
    vars.skipSonarCheck = vars.get("skipSonarCheck", true).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
    vars.pomFile = vars.get("pomFile", "pom.xml").trim()
    vars.mavenGoals = vars.get("mavenGoals", "").trim()
    vars.shellOutputFile = vars.get("shellOutputFile", "maven.log").trim()
    vars.mavenHome = vars.get("mavenHome", "${vars.JENKINS_USER_HOME}/.m2/").trim()

    try {
        tee("${vars.shellOutputFile}") {

            // TODO configFileProvider do not work on docker, it is working on VM only and might fail then when JENKINS_USER_HOME do not exist
            configFileProvider([
                //configFile(fileId: "fr-npmrc-default",  targetLocation: "${vars.JENKINS_USER_HOME}/.npmrc", variable: NPM_SETTINGS),
                //configFile(fileId: "fr-bowerrc-default",  targetLocation: ${vars.JENKINS_USER_HOME}/.bowerrc", variable: BOWER_SETTINGS),
                configFile(fileId: "${vars.MAVEN_SETTINGS_CONFIG}",  targetLocation: "${vars.mavenHome}/settings.xml", variable: MAVEN_SETTINGS_XML),
                configFile(fileId: "${vars.MAVEN_SETTINGS_SECURITY_CONFIG}",  targetLocation: "${vars.mavenHome}/settings-security.xml", variable: MAVEN_SETTINGS_SECURITY_XML)
                ]) {
                //withMaven(
                //    maven: "${vars.MAVEN_VERSION}",
                //    jdk: "${vars.JDK_VERSION}",
                //    //mavenSettingsConfig: "${vars.MAVEN_SETTINGS_CONFIG}",
                //    mavenLocalRepo: './.repository',
                //    mavenOpts: "${vars.MAVEN_OPTS}",
                //    options: [
                //        pipelineGraphPublisher(
                //            ignoreUpstreamTriggers: !isReleaseBranch(),
                //            skipDownstreamTriggers: !isReleaseBranch(),
                //            lifecycleThreshold: 'deploy'
                //        ),
                //        artifactsPublisher(disabled: true)
                //    ]
                //) {
                    //withSonarQubeEnv("${vars.SONAR_INSTANCE}") {
                        if (!vars.skipResults) {

                            if (!vars.RELEASE_VERSION) {
                                echo 'No RELEASE_VERSION specified'
                                vars.RELEASE_VERSION = getReleasedVersion(vars) ?: "0.0.1"
                                //if (!vars.RELEASE_VERSION) {
                                //   error 'No RELEASE_VERSION found'
                                //}
                            }

                            TARGET_TAG = getShortReleasedVersion(vars) ?: "0.0.1"
                            echo "Maven RELEASE_VERSION: ${vars.RELEASE_VERSION} - ${TARGET_TAG}"

                            manager.addShortText("${TARGET_TAG}")

                            if (vars.RELEASE) {
                              if (!vars.RELEASE_VERSION) {
                                  vars.RELEASE_VERSION = vars.RELEASE_BASE
                              } else {
                                  vars.RELEASE_VERSION = vars.RELEASE_VERSION
                                  substitutePomXmlVersion {
                                      newVersion = vars.RELEASE_VERSION
                                  }
                              }
                            }

                        } // skipResults

                        if (!vars.skipDeploy) {
                           if ( BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
                              vars.goal = "deploy"
                              vars.mavenGoals += " --fail-never "
                           }
                        }

                        vars.mavenGoals += " -Dmaven.repo.local=./.repository "

                        if (!vars.skipMavenSettings) {
                           vars.mavenGoals += " -s ${vars.mavenHome}/settings.xml "
                        }

                        if (vars.CLEAN_RUN) {
                          vars.mavenGoals += " -U clean "
                        }

                        if (!vars.DRY_RUN) {
                            if (vars.goal?.trim()) {
                                vars.mavenGoals += " ${vars.goal} "
                            }
                        } else {
                            vars.mavenGoals += " validate -Dsonar.analysis.mode=preview -Denforcer.skip=true "
                        }

                        if (vars.buildCmdParameters?.trim()) {
                            vars.mavenGoals += " ${vars.buildCmdParameters}"
                        }

                        vars.mavenGoals += getMavenGoalsProfile(vars)

                        vars.mavenGoals = getMavenGoalsPitest(vars)

                        vars.mavenGoals = getMavenGoalsSonar(vars)

                        vars.mavenGoals = getMavenGoalsTest(vars)

                        vars.mavenGoals = getMavenGoalsSigning(vars)

                        vars.mavenGoals = getMavenGoalsDocker(vars)

                        echo "Maven OPTS have been specified: ${vars.MAVEN_OPTS}"
                        echo "Maven GOALS have been specified: ${vars.mavenGoals}"
                        vars.buildCmd += " ${vars.mavenGoals}"

                        // TODO Remove it when tee will be back
                        //if (vars.skipSonar.toBoolean())
                        //    vars.buildCmd += " 2>&1 > ${vars.shellOutputFile} "

                        //wrap([$class: 'Xvfb', autoDisplayName: false, additionalOptions: '-pixdepths 24 4 8 15 16 32', parallelBuild: true]) {
                            // Run the maven build
                            build = sh (
                                    script: """#!/bin/bash -l
                                    export NODE_PATH=${env.WORKSPACE}
                                    export PATH=./node/npm/bin/:./node/:/bin:$PATH
                                    export MAVEN_OPTS=\"${vars.MAVEN_OPTS}\"
                                    mvn --version
                                    mvn ${vars.buildCmd}""",
                                    returnStatus: true
                                    )
                            //if (DEBUG_RUN) {
                            //    writeFile file: '.archive-jenkins-maven-event-spy-logs', text: ''
                            //}
                            echo "MAVEN RETURN CODE : ${build}"
                            if (build == 0) {
                                echo "MAVEN SUCCESS"
                            } else {
                                echo "WARNING : Maven failed, check output at \'${vars.shellOutputFile}\' "
                                if (!vars.skipFailure) {
                                    echo "MAVEN FAILURE"
                                    //currentBuild.result = 'UNSTABLE'
                                    currentBuild.result = 'FAILURE'
                                    error 'There are errors in maven'
                                } else {
                                    echo "MAVEN FAILURE skipped"
                                    //error 'There are errors in maven'
                                }
                            }
                            if (body) { body() }
                        //} // Xvfb
                        if (!vars.skipSonarCheck && !vars.skipSonar) {
                            vars.sonarCheckOutputFile = "maven-sonar-check.log"
                            withSonarQubeCheck(vars)
                        }
                    //} // withSonarQubeEnv
                //} // withMaven
            } // configFileProvider

            if (!vars.skipResults) {
                if (!vars.DRY_RUN) {

                    if (!vars.skipArtifacts) {
                        stash includes: "${vars.artifacts}", name: 'maven-artifacts'
                    }

                    stash allowEmpty: true, includes: "**/target/classes/**", name: 'classes'
                }

            } // skipResults

        } // tee
    } catch (e) {
        step([$class: 'ClaimPublisher'])
        throw e
    } finally {
        //step([
        //     $class: "WarningsPublisher",
        //     canComputeNew: false,
        //     canResolveRelativePaths: false,
        //     canRunOnFailed: true,
         //    consoleParsers: [
         //        [
         //            parserName: 'Java Compiler (javac)'
         //        ],
         //        [
         //            parserName: 'Maven'
         //        ]
         //    ],
         //    usePreviousBuildAsReference: true,
         //    useStableBuildAsReference: true
         //    ])

        if (!vars.skipResults) {
            stash allowEmpty: true, includes: 'target/jacoco*.exec, target/lcov*.info, karma-coverage/**/*', name: 'coverage'

            stash allowEmpty: true, includes: "${vars.artifacts}", name: 'app'
        }

        archiveArtifacts artifacts: "*.log, **/report-task.txt, **/dependency-check-report.xml, **/ZKM_log.txt, **/ChangeLog.txt, *_VERSION.TXT, ${vars.artifacts}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true

        if ((!vars.DRY_RUN && !vars.RELEASE) && !vars.skipTests && !vars.skipResults) {
            junit '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports-embedded/TEST-*.xml'
        } // if DRY_RUN
    }
}
