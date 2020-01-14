#!/usr/bin/env groovy

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withChangelog.groovy`"

    vars.showSummary = vars.get("showSummary", true).toBoolean()
    vars.isPublishEnabled = vars.get("isPublishEnabled", false).toBoolean()
    vars.isCleaningEnabled = vars.get("isCleaningEnabled", false).toBoolean()

	//def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
	//def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
	def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()

	if (body) { body() }

	if (!DEBUG_RUN && vars.isCleaningEnabled) {
		sh 'rm CHANGELOG.html'
	}

	writeFile file: "git-changelog-settings.json", text: '''
{
 "fromRepo": ".",
 "fromCommit": "0000000000000000000000000000000000000000",
 "toRef": "refs/tags/LATEST_SUCCESSFULL",

 "ignoreCommitsIfMessageMatches": "^\\[maven-release-plugin\\].*|^\\[Gradle Release Plugin\\].*|^Merge.*",
 "readableTagName": "/([^/]+?)$",
 "dateFormat": "YYYY-MM-dd HH:mm:ss",
 "untaggedName": "Next release",
 "noIssueName": "Other changes",
 "ignoreCommitsWithoutIssue": "true",
 "timeZone": "UTC",
 "removeIssueFromMessage": "true",

 "jiraServer": "https://almtools/jira",
 "jiraIssuePattern": "\\b[a-zA-Z]([a-zA-Z]+)-([0-9]+)\\b",

 "gitHubApi": "https://api.github.com/repos/tomasbjerre/git-changelog-lib",
 "gitHubIssuePattern": "#([0-9]+)",

 "customIssues": [
  { "name": "Incidents", "title": "${PATTERN_GROUP_1}", "pattern": "INC([0-9]*)", "link": "http://inc/${PATTERN_GROUP}" },
  { "name": "CQ", "title": "${PATTERN_GROUP_1}", "pattern": "CQ([0-9]+)", "link": "http://cq/${PATTERN_GROUP_1}" },
  { "name": "Bugs", "title": "Mixed bugs", "pattern": "#bug" }
 ]
}
'''

	def createFileTemplateContent = '''<h1> Git Changelog changelog </h1>

<p>
Changelog of Git Changelog.
</p>

{{#tags}}
<h2> {{name}} </h2>
 {{#issues}}
  {{#hasIssue}}
   {{#hasLink}}
<h2> {{name}} <a href="{{link}}">{{issue}}</a> {{title}} </h2>
   {{/hasLink}}
   {{^hasLink}}
<h2> {{name}} {{issue}} {{title}} </h2>
   {{/hasLink}}
  {{/hasIssue}}
  {{^hasIssue}}
<h2> {{name}} </h2>
  {{/hasIssue}}


   {{#commits}}
<a href="https://github.com/tomasbjerre/git-changelog-lib/commit/{{hash}}">{{hash}}</a> {{authorName}} <i>{{commitTime}}</i>
<p>
<h3>{{{messageTitle}}}</h3>

{{#messageBodyItems}}
 <li> {{.}}</li>
{{/messageBodyItems}}
</p>
  {{/commits}}

 {{/issues}}
{{/tags}}
'''

	step([$class: 'GitChangelogRecorder',
		config: [configFile: 'git-changelog-settings.json',
		createFileTemplateContent: createFileTemplateContent,
		createFileTemplateFile: '',
		createFileUseTemplateContent: true,
		createFileUseTemplateFile: true,
		customIssues: [[link: '', name: '', pattern: '', title: ''], [link: '', name: '', pattern: '', title: '']],
		dateFormat: 'YYYY-MM-dd HH:mm:ss',
		file: 'CHANGELOG.html',
		fromReference: '',
		fromType: 'firstCommit',
		gitHubApi: '',
		gitHubApiTokenCredentialsId: '',
		gitHubIssuePattern: '#([0-9]+)',
		gitLabApiTokenCredentialsId: '',
		gitLabProjectName: '',
		gitLabServer: '',
		ignoreCommitsIfMessageMatches: '^\\[maven-release-plugin\\].*|^\\[Gradle Release Plugin\\].*|^Merge.*',
		ignoreTagsIfNameMatches: '',
		jiraIssuePattern: '\\b[a-zA-Z]([a-zA-Z]+)-([0-9]+)\\b',
		jiraServer: 'https://almtools/jira/',
		jiraUsernamePasswordCredentialsId: '0c7d55cb-866d-4df5-9f92-bada66b5288d',
		noIssueName: 'No issue',
		readableTagName: '/([^/]+?)$',
		showSummaryTemplateFile: 'changelog.mustache',
		showSummaryUseTemplateFile: true,
		showSummary: vars.showSummary,
		subDirectory: '',
		timeZone: 'UTC',
		toReference: '',
		toType: 'refs/remotes/origin/develop',
		untaggedName: 'Unreleased',
		useConfigFile: false,
		useFile: true]])
									
	archiveArtifacts artifacts: 'CHANGELOG.html', excludes: null, fingerprint: false, onlyIfSuccessful: false, allowEmptyArchive: true
	
	if (vars.isPublishEnabled) {
		publishHTML (target: [
		  allowMissing: true,
		  alwaysLinkToLastBuild: false,
		  keepAll: true,
		  reportFiles: 'CHANGELOG.html',
		  reportName: "Changelog"
		])                 
	}

}
