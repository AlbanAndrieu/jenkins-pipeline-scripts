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
        "-Xmx2G",
        //"-Djava.io.tmpdir=./target/tmp",
        ].join(" ")

    def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g")
    //def SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: "$WORKSPACE")
    def MAVEN_SETTINGS_CONFIG = vars.get("MAVEN_SETTINGS_CONFIG", env.MAVEN_SETTINGS_CONFIG ?: "mgr-settings-nexus") // fr-maven-default
    def MAVEN_SETTINGS_SECURITY_CONFIG = vars.get("MAVEN_SETTINGS_SECURITY_CONFIG", env.MAVEN_SETTINGS_SECURITY_CONFIG ?: "mgr-settings-security-nexus")
    def MAVEN_VERSION = vars.get("MAVEN_VERSION", env.MAVEN_VERSION ?: "maven 3.5.2") // maven-latest
    def JDK_VERSION = vars.get("JDK_VERSION", env.JDK_VERSION ?: "jdk8") // java-latest

    if (DEBUG_RUN) {
         echo "debug added"
         MAVEN_OPTS_DEFAULT = ["${MAVEN_OPTS_DEFAULT}",
             "-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError",
         ].join(" ")
    }

    def MAVEN_OPTS = vars.get("MAVEN_OPTS", "${MAVEN_OPTS_DEFAULT}")

    def SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonar")

    echo "Maven OPTS have been specified: ${MAVEN_OPTS} - ${CLEAN_RUN}/${DRY_RUN}/${DEBUG_RUN} - ${SONAR_INSTANCE}"

    vars.goal = vars.get("goal", "install")
    vars.profile = vars.get("profile", "sonar").trim()
    vars.skipTests = vars.get("skipTests", false).toBoolean()
    vars.skipResults = vars.get("skipResults", false).toBoolean()
    //vars.buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
    vars.buildCmd = vars.get("buildCmd", "-e").trim()
    vars.skipSonar = vars.get("skipSonar", false).toBoolean()
    vars.skipPitest = vars.get("skipPitest", false).toBoolean()
    vars.buildCmdParameters = vars.get("buildCmdParameters", "").trim()
    vars.artifacts = vars.get("artifacts", ['*_VERSION.TXT', '**/target/*.jar'].join(', '))
    vars.skipArtifacts = vars.get("skipArtifacts", false).toBoolean()
    vars.skipFailure = vars.get("skipFailure", false).toBoolean()
    vars.skipDeploy = vars.get("skipDeploy", true).toBoolean()
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
    vars.pomFile = vars.get("pomFile", "pom.xml").trim()
    vars.mavenGoals = vars.get("mavenGoals", "").trim()
    vars.mavenOutputFile = vars.get("mavenOutputFile", "maven.log").trim()
    vars.mavenHome = vars.get("mavenHome", "/jenkins/.m2/").trim()

    try {
        tee("${vars.mavenOutputFile}") {

            configFileProvider([configFile(fileId: "${MAVEN_SETTINGS_CONFIG}",  targetLocation: "${vars.mavenHome}/settings.xml", variable: 'SETTINGS_XML'),
                configFile(fileId: "${MAVEN_SETTINGS_SECURITY_CONFIG}",  targetLocation: "${vars.mavenHome}/settings-security.xml", variable: 'MAVEN_SETTINGS_SECURITY_CONFIG')]) {
                withMaven(
                    maven: "${MAVEN_VERSION}",
                    jdk: "${JDK_VERSION}",
                    mavenSettingsConfig: "${MAVEN_SETTINGS_CONFIG}",
                    mavenLocalRepo: './.repository',
                    mavenOpts: "${MAVEN_OPTS}",
                    options: [
                        pipelineGraphPublisher(
                            ignoreUpstreamTriggers: !isReleaseBranch(),
                            skipDownstreamTriggers: !isReleaseBranch(),
                            lifecycleThreshold: 'deploy'
                        ),
                        artifactsPublisher(disabled: true)
                    ]
                ) {
                    withSonarQubeEnv("${SONAR_INSTANCE}") {

                        if (!vars.skipResults) {

                            if (!RELEASE_VERSION) {
                                echo 'No RELEASE_VERSION specified'
                                RELEASE_VERSION = getReleasedVersion(vars) ?: "LATEST"
                                //if (!RELEASE_VERSION) {
                                //   error 'No RELEASE_VERSION found'
                                //}
                            }

                            TARGET_TAG = getShortReleasedVersion(vars) ?: "LATEST"
                            echo "Maven RELEASE_VERSION: ${RELEASE_VERSION} - ${TARGET_TAG}"

                            manager.addShortText("${TARGET_TAG}")

                            if (RELEASE) {
                              if (!RELEASE_VERSION) {
                                  RELEASE_VERSION = RELEASE_BASE
                              } else {
                                  RELEASE_VERSION = RELEASE_VERSION
                                  substitutePomXmlVersion {
                                      newVersion = RELEASE_VERSION
                                  }
                              }
                            }

                        } // skipResults

                        if (!vars.skipDeploy) {
                           if ( BRANCH_NAME ==~ /develop|master|master_.+|release\/.+/ ) {
                              vars.goal = "deploy"
                           }
                        }

                        vars.mavenGoals += " -s ${SETTINGS_XML} -Dmaven.repo.local=./.repository "

                        if (CLEAN_RUN) {
                          vars.mavenGoals += " -U clean"
                        }

                        if (!DRY_RUN) {
                            if (vars.goal?.trim()) {
                                vars.mavenGoals += " ${vars.goal}"
                            }
                        } else {
                            vars.mavenGoals += " validate -Dsonar.analysis.mode=preview -Denforcer.skip=true"
                        }

                        if (vars.buildCmdParameters?.trim()) {
                            vars.mavenGoals += " ${vars.buildCmdParameters}"
                        }

                        vars.mavenGoals += getMavenGoalsProfile(vars)

                        vars.mavenGoals = getMavenGoalsPitest(vars)

                        vars.mavenGoals = getMavenGoalsSonar(vars)

                        vars.mavenGoals = getMavenGoalsTest(vars)

                        //vars.mavenGoals = getMavenGoalsZkm(vars)

                        vars.mavenGoals = getMavenGoalsSigning(vars)

                        vars.mavenGoals = getMavenGoalsDocker(vars)

                        echo "Maven GOALS have been specified: ${vars.mavenGoals}"
                        vars.buildCmd += " ${vars.mavenGoals}"

                        // TODO Remove it when tee will be back
                        //vars.buildCmd += " 2>&1 > ${vars.mavenOutputFile} "

                        //wrap([$class: 'Xvfb', autoDisplayName: false, additionalOptions: '-pixdepths 24 4 8 15 16 32', parallelBuild: true]) {
                            // Run the maven build
                            build = sh (
                                    script: "export PATH=$MVN_CMD_DIR:/bin:$PATH && mvn ${vars.buildCmd}",
                                    returnStatus: true
                                    )
                            //if (DEBUG_RUN) {
                            //    writeFile file: '.archive-jenkins-maven-event-spy-logs', text: ''
                            //}
                            echo "BUILD RETURN CODE : ${build}"
                            if (build == 0) {
                                echo "MAVEN SUCCESS"
                            } else {
                                echo "MAVEN FAILURE"
                                if (!vars.skipFailure) {
                                    currentBuild.result = 'FAILURE'
                                    error 'There are errors in maven'
                                }
                            }
                            if (body) { body() }
                        //} // Xvfb
                    } // withSonarQubeEnv
                } // withMaven
            } // configFileProvider

            if (!vars.skipResults) {
                if (!DRY_RUN) {

                    if (!vars.skipArtifacts) {
                        stash includes: "${vars.artifacts}", name: 'maven-artifacts'
                    }

                    stash includes: "**/target/classes/**", name: 'classes'
                }

                if ((!DRY_RUN && !RELEASE) && !vars.skipTests) {
                    junit '**/target/surefire-reports/TEST-*.xml'
                } // if DRY_RUN

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

        archiveArtifacts artifacts: "*.log, **/dependency-check-report.xml, **/ZKM_log.txt, **/ChangeLog.txt, *_VERSION.TXT", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true
    }
}
