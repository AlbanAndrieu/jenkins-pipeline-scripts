#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withCheckmarxWrapper.groovy`'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.CHECKMARX_CREDENTIALS = vars.get('CHECKMARX_CREDENTIALS', env.CHECKMARX_CREDENTIALS ?: 'jenkins.checkmarx').trim()
  vars.CHECKMARX_URL = vars.get('CHECKMARX_URL', env.CHECKMARX_URL ?: 'https://checkmarx').trim()

  vars.excludeFolders = vars.get('excludeFolders', ', bm').trim()
  vars.preset = vars.get('preset', '17')

  vars.groupId = vars.get('groupId', '000').trim()
  vars.password = vars.get('password', '{AAA/BBB=}').trim()
  vars.generatePdfReport = vars.get('generatePdfReport', false).toBoolean()
  vars.lowThreshold = vars.get('lowThreshold', 1000)
  vars.mediumThreshold = vars.get('mediumThreshold', 100)
  vars.highThreshold = vars.get('highThreshold', 50)
  vars.avoidDuplicateProjectScans = vars.get('avoidDuplicateProjectScans', false).toBoolean()
  vars.incremental = vars.get('incremental', true).toBoolean()
  vars.waitForResultsEnabled = vars.get('waitForResultsEnabled', true).toBoolean()

  RELEASE_VERSION = getSemVerReleasedVersion(vars)

  String projectName = 'NABLA_' + getGitRepoName(vars) ?: 'TEST' + '_[' + RELEASE_VERSION + ']'
  vars.projectName = vars.get('projectName', projectName).trim().replaceAll(' ', '-')
  vars.skipCheckmarxFailure = vars.get('skipCheckmarxFailure', false).toBoolean()
  vars.skipCheckmarx = vars.get('skipCheckmarx', false).toBoolean()

  if (!vars.skipCheckmarx) {
    if ((env.BRANCH_NAME ==~ /PR-.*/) || (env.BRANCH_NAME ==~ /feature\/.*/) || (env.BRANCH_NAME ==~ /bugfix\/.*/)) {
      echo 'Force incremental mode'
      vars.incremental = true
      echo 'Force Default 2017 - light preset mode'
      vars.preset = '100013'
    }
    if (env.BRANCH_NAME == 'master') {
      echo 'Disable incremental mode'
      vars.incremental = false
    }

    if ((env.BRANCH_NAME == 'develop') || (env.BRANCH_NAME == 'master') || (env.BRANCH_NAME ==~ /release\/.*/)) {
      echo 'Avoid duplicate project scans'
      vars.avoidDuplicateProjectScans = true
    }

    if (!vars.DRY_RUN && !vars.RELEASE) {
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
          echo 'Aborted by ' + cause.getUser()
          if (cause.getUser().toString() != 'SYSTEM') {
            startMillis = System.currentTimeMillis()
           } else {
            endMillis = System.currentTimeMillis()
            if (endMillis - startMillis >= timeoutMillis) {
              echo 'Approval timed out. Continuing.'
              didTimeout = true
             } else {
              userAborted = true
              echo "SYSTEM aborted, but looks like timeout period didn't complete. Aborting."
            }
          }
         } catch (err) {
          echo 'timeout reached or input false'
          def user = err.getCauses()[0].getUser()
          if ('SYSTEM' == user.toString()) { // SYSTEM means timeout.
            didTimeout = true
             } else {
            userInput = false
            echo "Aborted by: [${user}]"
          }
        }

        if (didTimeout) {
          echo 'no input was received before timeout'

          if (body) { body() }

          step([
                 $class: 'CxScanBuilder',
                 avoidDuplicateProjectScans: vars.avoidDuplicateProjectScans,
                 comment: 'Jenkins JPL',
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
                        credentialsId: vars.CHECKMARX_CREDENTIALS,
                    groupId: vars.groupId,
                    password: vars.password,
                    preset: vars.preset,
                    projectName: vars.projectName,
                        serverUrl: vars.CHECKMARX_URL,
                    skipSCMTriggers: true,
                    sourceEncoding: '1',
                    username: '',
                    vulnerabilityThresholdEnabled: true,
                    vulnerabilityThresholdResult: 'FAILURE',
                        waitForResultsEnabled: vars.waitForResultsEnabled
                ])
        } else if (userInput == true) {
          // do something
          echo 'Manual skip requested'
        } else {
          //currentBuild.result = 'FAILURE'
          if (!vars.skipCheckmarxFailure) {
            echo 'CHECKMARX UNSTABLE'
            currentBuild.result = 'UNSTABLE'
                } else {
            echo 'CHECKMARX FAILURE skipped'
          //error 'There are errors in aqua' // not needed
          }
          echo "WARNING : Scan failed, check output at ${vars.CHECKMARX_URL}/CxWebClient."
        } // if didTimeout
       } catch (exc) {
        echo 'WARNING : There was a problem retrieving checkmarx scan' + exc
      }
    } // if DRY_RUN
  } else {
    echo 'Checkmarx scan skipped'
  }
}
