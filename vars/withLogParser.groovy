#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/withLogParser.groovy'

  vars = vars ?: [:]

  getJenkinsOpts(vars)

  vars.parsingRulesPath = vars.get('parsingRulesPath', '/jenkins/deploy-log_parsing_rules').trim()
  vars.failBuildOnError = vars.get('failBuildOnError', true).toBoolean()
  vars.unstableOnWarning = vars.get('unstableOnWarning', true).toBoolean()

  vars.skipLogParser = vars.get('skipLogParser', false).toBoolean()
  vars.logParseFileId = vars.get('logParseFileId', '0').trim()
  //vars.logParserOutputFile = vars.get("logParserOutputFile", "log-parser-${vars.logParseFileId}.json").trim()

  if ( BRANCH_NAME ==~ /master|master_.+|release\/.+/ ) {
    vars.skipLogParser = true
  }

  if (!vars.skipLogParser) {
    try {
      if (body) { body() }

      logParser failBuildOnError: vars.failBuildOnError, parsingRulesPath: vars.parsingRulesPath, showGraphs: true, unstableOnWarning: vars.unstableOnWarning, useProjectRule: false
      } catch (exc) {
      echo 'Warn: There was a problem with log parser ' + exc
      } finally {
      archiveArtifacts artifacts: "${vars.logParserOutputFile}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'Log parser skipped'
  }
}
