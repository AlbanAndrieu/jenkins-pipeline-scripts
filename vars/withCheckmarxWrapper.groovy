#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withCheckmarxWrapper.groovy`"

    vars = vars ?: [:]

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    //def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    //def RELEASE_VERSION = vars.get("RELEASE_VERSION", env.RELEASE_VERSION ?: null)
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()
    //def RELEASE_BASE = vars.get("RELEASE_BASE", env.RELEASE_BASE ?: null)

    vars.excludeFolders = vars.get("excludeFolders", ", bm").trim()
    vars.projectName = vars.get("projectName", "NABLA_" + env.JOB_BASE_NAME ?: "TEST").trim().replaceAll(' ','-')
    vars.preset = vars.get("preset", '17')

    vars.groupId = vars.get("groupId", '000').trim()
    vars.password = vars.get("password", '{AAA/BBB=}').trim()
    vars.generatePdfReport = vars.get("generatePdfReport", false).toBoolean()
    vars.lowThreshold = vars.get("lowThreshold", 1000)
    vars.mediumThreshold = vars.get("mediumThreshold", 100)
    vars.highThreshold = vars.get("highThreshold", 50)
    vars.avoidDuplicateProjectScans = vars.get("avoidDuplicateProjectScans", false).toBoolean()
    vars.incremental = vars.get("incremental", true).toBoolean()

    if ((env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/)) {
        echo "Force incremental mode"
        vars.incremental = true
        echo "Force Default 2017 - light preset mode"
        vars.preset = '100013'
    }
    if (env.BRANCH_NAME == 'master') {
        echo "Disable incremental mode"
        vars.incremental = false
    }

    if ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME == 'master') || (env.BRANCH_NAME ==~ /release\/.*/)) {
        echo "Avoid duplicate project scans"
        vars.avoidDuplicateProjectScans = true
    }

    if (!DRY_RUN && !RELEASE) {

         try {
         def userInput = false
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
               echo "Approval timed out. Continuing."
               didTimeout = true
             } else {
               userAborted = true
               echo "SYSTEM aborted, but looks like timeout period didn't complete. Aborting."
             }
           }
         } catch(err) {
             echo "timeout reached or input false"
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

             if (body) { body() }

             step([
                 $class: 'CxScanBuilder',
                 avoidDuplicateProjectScans: vars.avoidDuplicateProjectScans,
                 comment: '',
                 excludeFolders: '.repository, target, .node_cache, .tmp, .node_tmp, .git, .grunt, .bower, .mvnw, bower_components, node_modules, npm, node, lib, libs, docs, hooks, help, test, Sample, vendors, dist, build, site, fonts, images, coverage, .mvn, ansible' + vars.excludeFolders,
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
                    generatePdfReport: vars.generatePdfReport,
                    includeOpenSourceFolders: '',
                    incremental: vars.incremental,
                    lowThreshold: vars.lowThreshold,
                    mediumThreshold: vars.mediumThreshold,
                    highThreshold: vars.highThreshold,
                    osaArchiveIncludePatterns: '*.zip, *.war, *.ear, *.tgz',
                    osaInstallBeforeScan: false,
                    osaLowThreshold: 1000,
                    osaMediumThreshold: 100,
                    osaHighThreshold: 10,
                    useOwnServerCredentials: true,
                    credentialsId: 'nabla.checkmarx',
                    groupId: vars.groupId,
                    password: vars.password,
                    preset: vars.preset,
                    projectName: vars.projectName,
                    serverUrl: 'https://nabla-checkmarx',
                    skipSCMTriggers: true,
                    sourceEncoding: '1',
                    username: '',
                    vulnerabilityThresholdEnabled: true,
                    vulnerabilityThresholdResult: 'FAILURE',
                    waitForResultsEnabled: true
                ])

        } else if (userInput == true) {
            // do something
            echo "skip requested"
        } else {
            // do something else
            echo "this was not successful"
            //currentBuild.result = 'FAILURE'
        } // if didTimeout
       } catch (exc) {
         echo "WARNING : There was a problem retrieving checkmarx scan" + exc.toString()
       }
    } // if DRY_RUN

}
