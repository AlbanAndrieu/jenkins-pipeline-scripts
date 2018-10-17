#!/usr/bin/groovy
//import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN.toBoolean() ?: true)
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN.toBoolean() ?: false)
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE.toBoolean() ?: false)
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    def excludeFolders = vars.get("excludeFolders", ", bm")
    def projectName = vars.get("projectName", ", TEST_Checkmarx")
    def preset = vars.get("preset", '17')

    def groupId = vars.get("groupId", '000')
    def password = vars.get("password", '{AAA/BBB=}')

    if (!DRY_RUN && !RELEASE) {

         def userInput = true
         def didTimeout = false
         def userAborted = false
         def startMillis = System.currentTimeMillis()
         def timeoutMillis = 10000

         try {
              timeout(time: 15, unit: 'SECONDS') { // change to a convenient timeout for you
                      userInput = input(
                      id: 'Checkmarx', message: 'Skip Checkmarx?', parameters: [
                      [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'Please confirm you agree with this']
                      ])
              }
         } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
           cause = e.causes.get(0)
           echo "Aborted by " + cause.getUser().toString()
           if (cause.getUser().toString() != 'SYSTEM') {
             startMillis = System.currentTimeMillis()
           } else {
             endMillis = System.currentTimeMillis()
             if (endMillis - startMillis >= timeoutMillis) {
               echo "Approval timed out. Continuing with deployment."
               didTimeout = true
             } else {
               userAborted = true
               echo "SYSTEM aborted, but looks like timeout period didn't complete. Aborting."
             }
           }
         } catch(err) { // timeout reached or input false
             def user = err.getCauses()[0].getUser()
             if('SYSTEM' == user.toString()) { // SYSTEM means timeout.
                 didTimeout = true
             } else {
                 userInput = false
                 echo "Aborted by: [${user}]"
             }
         }

         if (didTimeout) {
             echo "no input was received before timeout"

             unstash 'sources'

             step([
                 $class: 'CxScanBuilder',
                 avoidDuplicateProjectScans: true,
                 comment: '',
                 excludeFolders: '.repository, target, .node_cache, .tmp, .node_tmp, .git, .grunt, .bower, .mvnw, bower_components, node_modules, npm, node, lib, libs, docs, hooks, help, test, Sample, vendors, dist, build, site, fonts, images, coverage, .mvn, ansible' + excludeFolders,
                 excludeOpenSourceFolders: '',
                 exclusionsSetting: 'job',
                 failBuildOnNewResults: true,
                 failBuildOnNewSeverity: 'HIGH',
                 filterPattern: '''
!**/_cvs/**/*, !**/.svn/**/*,   !**/.hg/**/*,   !**/.git/**/*,  !**/.bzr/**/*, !**/bin/**/*,
!**/obj/**/*,  !**/backup/**/*, !**/.idea/**/*, !**/*.DS_Store, !**/*.ipr,     !**/*.iws,
!**/*.bak,     !**/*.tmp,       !**/*.aac,      !**/*.aif,      !**/*.iff,     !**/*.m3u, !**/*.mid, !**/*.mp3,
!**/*.mpa,     !**/*.ra,        !**/*.wav,      !**/*.wma,      !**/*.3g2,     !**/*.3gp, !**/*.asf, !**/*.asx,
!**/*.avi,     !**/*.flv,       !**/*.mov,      !**/*.mp4,      !**/*.mpg,     !**/*.rm,  !**/*.swf, !**/*.vob,
!**/*.wmv,     !**/*.bmp,       !**/*.gif,      !**/*.jpg,      !**/*.png,     !**/*.psd, !**/*.tif, !**/*.swf,
!**/*.jar,     !**/*.zip,       !**/*.rar,      !**/*.exe,      !**/*.dll,     !**/*.pdb, !**/*.7z,  !**/*.gz,
!**/*.tar.gz,  !**/*.tar,       !**/*.gz,       !**/*.ahtm,     !**/*.ahtml,   !**/*.fhtml, !**/*.hdm,
!**/*.hdml,    !**/*.hsql,      !**/*.ht,       !**/*.hta,      !**/*.htc,     !**/*.htd, !**/*.war, !**/*.ear,
!**/*.htmls,   !**/*.ihtml,     !**/*.mht,      !**/*.mhtm,     !**/*.mhtml,   !**/*.ssi, !**/*.stm,
!**/*.stml,    !**/*.ttml,      !**/*.txn,      !**/*.xhtm,     !**/*.xhtml,   !**/*.class, !**/*.iml, !Checkmarx/Reports/*.*,
!**/*.csv,     !**/test/**/*,   !**/*Test.java, !**/*_UT.java,  !**/*_UT.groovy,!**/*_IT.java, !**/*_IT.groovy,  !**/*Test.groovy,
!**/Sample/**/*, !**/.xrp, !**/.yml,
!**/.xls, !**/.xlsx, !**/.doc, !**/.pdf, !**/.pfx, !**/.xll,
!**/.dylib, !**/.lib, !**/.a, !**/.so, !**/.pkg, !**/.swp, !**/.ttf, !**/.msi, !**/.chm,
''',
                    fullScanCycle: 10,
                    fullScansScheduled: false,
                    generatePdfReport: true,
                    highThreshold: 50,
                    includeOpenSourceFolders: '',
                    incremental: true,
                    lowThreshold: 1000,
                    mediumThreshold: 100,
                    osaArchiveIncludePatterns: '*.zip, *.war, *.ear, *.tgz',
                    osaHighThreshold: 10,
                    osaInstallBeforeScan: false,
                    osaLowThreshold: 1000,
                    osaMediumThreshold: 100,
                    useOwnServerCredentials: true,
                    credentialsId: 'nabla.checkmarx',
                    groupId: groupId,
                    password: password,
                    preset: preset,
                    projectName: projectName,
                    serverUrl: 'https://nabla-checkmarx',
                    skipSCMTriggers: true,
                    sourceEncoding: '1',
                    username: '',
                    vulnerabilityThresholdEnabled: true,
                    vulnerabilityThresholdResult: 'FAILURE',
                    waitForResultsEnabled: true
                ])

                if (body) { body() }

        } else if (userInput == true) {
            // do something
            echo "this was successful"
        } else {
            // do something else
            echo "this was not successful"
            //currentBuild.result = 'FAILURE'
        } // if didTimeout
    } // if DRY_RUN

}
