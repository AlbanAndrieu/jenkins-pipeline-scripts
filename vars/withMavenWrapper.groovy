#!/usr/bin/groovy
//import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    String MAVEN_OPTS_DEFAULT = ["-Djava.awt.headless=true",
        //"-Dsun.zip.disableMemoryMapping=true",
        //"-Dmaven.repo.local=./.repository",
        "-Xmx2G",
        //"-Djava.io.tmpdir=./target/tmp",
        ].join(" ")

    if (env.DEBUG_RUN) {
         echo "debug added"
         MAVEN_OPTS_DEFAULT = ["${MAVEN_OPTS_DEFAULT}",
             "-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError",
         ].join(" ")
    }

    def MAVEN_OPTS = vars.get("MAVEN_OPTS", "${MAVEN_OPTS_DEFAULT}")

    echo "Maven OPTS have been specified: ${MAVEN_OPTS}"

    def goal = vars.get("goal", "install")
    def profile = vars.get("profile", "sonar")
    def skipTests = vars.get("skipTests", false)
    def buildCmd = vars.get("buildCmd", "./mvnw -B -e ")
    def skipSonar = vars.get("skipSonar", false)
    def skipPitest = vars.get("skipPitest", false)
    def buildCmdParameters = vars.get("buildCmdParameters", "")

    configFileProvider([configFile(fileId: 'fr-maven-default',  targetLocation: 'gsettings.xml', variable: 'SETTINGS_XML')]) {
        withMaven(
            maven: 'maven-latest',
            jdk: 'java-latest',
            globalMavenSettingsConfig: 'fr-maven-default',
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
            withSonarQubeEnv("${env.SONAR_INSTANCE}") {

                if (!env.RELEASE_VERSION) {
                    echo 'No RELEASE_VERSION specified'
                    env.RELEASE_VERSION = getReleasedVersion()
                    //if (!env.RELEASE_VERSION) {
                    //   error 'No RELEASE_VERSION found'
                    //}
                }
                env.TARGET_TAG = getShortReleasedVersion()
                echo "Maven RELEASE_VERSION: ${env.RELEASE_VERSION} - ${env.TARGET_TAG}"

                manager.addShortText("${TARGET_TAG}")

                //sh 'echo SONAR_USER_HOME : ${SONAR_USER_HOME} && mkdir -p ${SONAR_USER_HOME}'

                if (env.RELEASE) {
                  if (!env.RELEASE_VERSION) {
                      env.RELEASE_VERSION = env.RELEASE_BASE
                  } else {
                      env.RELEASE_VERSION = env.RELEASE_VERSION
                      substitutePomXmlVersion {
                          newVersion = env.RELEASE_VERSION
                      }
                  }
                }

                String MAVEN_GOALS = "-s ${SETTINGS_XML}"

                if (env.CLEAN_RUN) {
                  MAVEN_GOALS += " -U clean"
                }

                if (!env.DRY_RUN) {
                    MAVEN_GOALS += " ${goal}"
                } else {
                    MAVEN_GOALS += " validate -Dsonar.analysis.mode=preview -Denforcer.skip=true"
                }

                MAVEN_GOALS += " ${buildCmdParameters} -Dmaven.test.skip=${skipTests} -P ${profile}"

                if (!skipPitest && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/))) {
                    if (!env.DRY_RUN && !env.RELEASE) {
                        echo "pitest added"
                        MAVEN_GOALS += " -DwithHistory"
                        MAVEN_GOALS += " org.pitest:pitest-maven:mutationCoverage"
                    }
                } // if DRY_RUN

                if ( !skipSonar && ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/))) {
                    echo "sonar added"
                    if (env.BRANCH_NAME ==~ /develop/) {
                        MAVEN_GOALS += " -Dsonar.branch.name=develop"
                    } else {
                        MAVEN_GOALS += " -Dsonar.branch.name=${env.BRANCH_NAME}"
                        MAVEN_GOALS += " -Dsonar.branch.target=develop"
                    }
                    MAVEN_GOALS += " sonar:sonar"
                }

                if ((env.BRANCH_NAME ==~ /release\/.*/) || (env.BRANCH_NAME ==~ /master\/.*/)) {
                    echo "skip test added"
                    MAVEN_GOALS += " -Dmaven.test.failure.ignore=true -Dmaven.test.failure.skip=true"
                }

                echo "Maven GOALS have been specified: ${MAVEN_GOALS}"
                buildCmd += "${MAVEN_GOALS}"

                //wrap([$class: 'Xvfb', autoDisplayName: false, additionalOptions: '-pixdepths 24 4 8 15 16 32', parallelBuild: true]) {
                    // Run the maven build
                    sh buildCmd
                    if (env.DEBUG_RUN) {
                        writeFile file: '.archive-jenkins-maven-event-spy-logs', text: ''
                    }
			        if (body) { body() }
                //} // Xvfb
            } // withSonarQubeEnv
        } // withMaven
    } // configFileProvider

}
