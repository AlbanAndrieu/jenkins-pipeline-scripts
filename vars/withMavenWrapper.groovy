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
    vars.profile = vars.get("profile", "sonar")
    vars.skipTests = vars.get("skipTests", false).toBoolean()
    vars.skipResults = vars.get("skipResults", false).toBoolean()
    //vars.buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
    vars.buildCmd = vars.get("buildCmd", "-e")
    vars.skipSonar = vars.get("skipSonar", false).toBoolean()
    vars.skipPitest = vars.get("skipPitest", false).toBoolean()
    vars.buildCmdParameters = vars.get("buildCmdParameters", "")
    vars.artifacts = vars.get("artifacts", ['*_VERSION.TXT', '**/target/*.jar'].join(', '))
    vars.skipFailure = vars.get("skipFailure", false).toBoolean()

    tee("maven.log") {

        configFileProvider([configFile(fileId: 'fr-maven-default',  targetLocation: 'gsettings.xml', variable: 'SETTINGS_XML')]) {
            withMaven(
                maven: 'maven-latest',
                jdk: 'java-latest',
                mavenSettingsConfig: 'maven-default',
                //mavenSettingsFilePath: "${SETTINGS_XML}",
                //globalMavenSettingsConfig: 'maven-default',
                //globalMavenSettingsFilePath: "${SETTINGS_XML}",
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
                            RELEASE_VERSION = getReleasedVersion()
                            //if (!RELEASE_VERSION) {
                            //   error 'No RELEASE_VERSION found'
                            //}
                        }

                        TARGET_TAG = getShortReleasedVersion()
                        echo "Maven RELEASE_VERSION: ${RELEASE_VERSION} - ${TARGET_TAG}"

                        manager.addShortText("${TARGET_TAG}")
	    
                        //sh 'echo SONAR_USER_HOME : ${SONAR_USER_HOME} && mkdir -p ${SONAR_USER_HOME}'
	    
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

                    String MAVEN_GOALS = "-s ${SETTINGS_XML} -Dmaven.repo.local=./.repository "

                    if (CLEAN_RUN) {
                      MAVEN_GOALS += " -U clean"
                    }

                    if (!DRY_RUN) {
                        if (vars.goal?.trim()) {
                            MAVEN_GOALS += " ${vars.goal}"
                        }
                    } else {
                        MAVEN_GOALS += " validate -Dsonar.analysis.mode=preview -Denforcer.skip=true"
                    }

                    if (vars.buildCmdParameters?.trim()) {
                        MAVEN_GOALS += " ${vars.buildCmdParameters}"
                    }
                    
                    MAVEN_GOALS += getMavenGoalsProfile(vars)

                    MAVEN_GOALS += getMavenGoalsPitest(vars)

                    MAVEN_GOALS += getMavenGoalsSonar(vars)

                    MAVEN_GOALS += getMavenGoalsTest(vars)

                    echo "Maven GOALS have been specified: ${MAVEN_GOALS}"
                    vars.buildCmd += " ${MAVEN_GOALS}"

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
                                error 'There are errors in maven'
                                currentBuild.result = 'FAILURE'
                            }
                        }
                        if (body) { body() }
                    //} // Xvfb
                } // withSonarQubeEnv
            } // withMaven
        } // configFileProvider

        if (!vars.skipResults) {
            if (!DRY_RUN) {

                stash includes: "${vars.artifacts}", name: 'maven-artifacts'

                stash allowEmpty: true, includes: 'target/jacoco*.exec, target/lcov*.info, karma-coverage/**/*', name: 'coverage'

                stash allowEmpty: true, includes: "${vars.artifacts}", name: 'app'
                stash includes: "**/target/classes/**", name: 'classes'
            }

            if ((!DRY_RUN && !RELEASE) && !vars.skipTests) {
                junit '**/target/surefire-reports/TEST-*.xml'
            } // if DRY_RUN

            step([
                 $class: "WarningsPublisher",
                 canComputeNew: false,
                 canResolveRelativePaths: false,
                 canRunOnFailed: true,
                 consoleParsers: [
                     [
                         parserName: 'Java Compiler (javac)'
                     ],
                     [
                         parserName: 'Maven'
                     ],
                     [
                         parserName: 'GNU Make + GNU C Compiler (gcc)', pattern: 'error_and_warnings.txt'
                     ]
                 ],
                 //unstableTotalAll: '10',
                 //unstableTotalHigh: '0',
                 //failedTotalAll: '10',
                 //failedTotalHigh: '0',
                 usePreviousBuildAsReference: true,
                 useStableBuildAsReference: true
                 ])
        } // skipResults

    } // tee

}
