#!/usr/bin/groovy
// com/test/jenkins/sonar/Sonar.groovy
package com.test.jenkins.sonar

import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

class Sonar implements Serializable {
	/**
	 * get_project_name
	 *
	 * Defines separate behaviour for Jenkins `Multibranch Pipeline` and `Bitbucket Team Project`
	 * returns project name from JOB_BASE_NAME
	 *
	 */
	@NonCPS
	static def get_project_name(jobName, defaultProject="RISK") {
		def tokenized_job_name = jobName.split('/')
		if (tokenized_job_name.size() == 2) {
			return defaultProject  // e.g. CMR/develop, there is no good way to get Stash project name
		} else {
			return tokenized_job_name[0]
		}
	}

	/**
	 * get_repository_name
	 *
	 * Defines separate behaviour for Jenkins `Multibranch Pipeline` and `Bitbucket Team Project`
	 * returns repository name from JOB_BASE_NAME
	 *
	 */
	@NonCPS
	static def get_repository_name(jobName) {
		def tokenized_job_name = jobName.split('/')
		if (tokenized_job_name.size() == 2) {
			return tokenized_job_name[0]  // e.g. CMR/develop
		} else {
			return tokenized_job_name[1]  // e.g. RISK/generic_limits/develop
		}
	}

	/**
	 * diffCommitsForRev
	 *
	 * returns a list of commits between current and target branch revision. Target defaults to develop.
	 */
	@NonCPS
	static def List<String> diffCommitsForRev(Map<String,String> config) {
		try {
			URLConnection filesForRevConn = new URL(
					"${config.baseUrl}/rest/api/1.0/projects/${config.project}/repos/${config.repository}/commits?since=${config.targetRevision}&until=${config.currentRevision}&limit=${config.commitCap}"
					).openConnection()

			filesForRevConn.setRequestProperty("Authorization", "Basic ${config.basicAuth}")

			def filesForRev = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(filesForRevConn.getInputStream())))

			List<String> diffRevisions = filesForRev.values.collect {
				it.id
			}

			diffRevisions

		}
		catch(exc) {
			echo 'Error: There were errors to connecting to sonar. '+exc.toString() // but we do not fail the whole build because of that
		}
	}

	/**
	 * filesForRev
	 *
	 * returns a list of commits between current and target branch revision. Target defaults to develop.
	 */
	@NonCPS
	static def List<String> filesForRev(Map<String,String> config, String revision) {
		try {
		URLConnection filesForRevConn = new URL(
				"${config.baseUrl}/rest/api/1.0/projects/${config.project}/repos/${config.repository}/commits/${revision}/changes?limit=${config.fileCap}"
				).openConnection()

		filesForRevConn.setRequestProperty("Authorization", "Basic ${config.basicAuth}")
		
		println("[JPL] URLConnection: ${filesForRevConn.getURL()}")

		def filesForRev = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(filesForRevConn.getInputStream()))) // LazyMap

		List<String> diffRevisions = filesForRev.values.collect {
			it.path.toString
		}

		diffRevisions
		}
		catch(exc) {
			echo 'Error: There were errors to connecting to sonar. '+exc.toString() // but we do not fail the whole build because of that
		}
	}

	@NonCPS
	static def getSonarInclusions(Map vars, Closure body=null) {

		println("[JPL] Executing `Sonar getSonarInclusions`")

		vars = vars ?: [:]

		def sonarCmdParameters = vars.get("sonarCmdParameters", "")
		//vars.project = vars.get("project", "RISK")

		def sonarInclusions = filesChanged(vars) {}.collect{ filename -> "${filename}" }.join(",")

		if (body) { body() }

		println("[JPL] Sonar Sources: ${sonarInclusions}")

		if (sonarInclusions?.trim()) {
			sonarCmdParameters = " -Dsonar.inclusions=\"${sonarInclusions}\" "
		}

		return sonarCmdParameters

	}

	@NonCPS
	static def filesChanged(Map vars, Closure body=null) {

		println("[JPL] Executing `Sonar getSonarInclusions` filesChanged")

		def config = [:]
		body.resolveStrategy = Closure.DELEGATE_FIRST
		body.delegate = config
		body()

		println("[JPL] Base CONFIG params set from JENKINSFILE are: ${config}")

		vars = vars ?: [:]

		// Run configuration
		// If number of commits exceeds the Cap, file list will not be searched (to save time); the script will return ["*"]
		vars.commitCap            = vars.get("commitCap", 50)
		// If number of files changes exceeds the Cap, all files should be Sonar-scanned; the script will return ["*"]
		vars.fileCap              = vars.get("fileCap", 50)
		vars.baseUrl              = vars.get("baseUrl", "https://scm-git-eur.misys.global.ad/")
		vars.jobName              = vars.get("jobName", "unknown")
		vars.project              = vars.get("project", get_project_name(vars.jobName, "RISK"))
		vars.repository           = vars.get("repository", get_repository_name(vars.jobName))
		vars.targetRevision       = vars.get("targetRevision", "refs/heads/develop")
		vars.currentRevision      = vars.get("currentRevision", "0")

		//println("[JPL] Repository: ${vars.repository}")

		println("[JPL] Full CONFIG after applying the default values is: ${vars}")

		List<String> diffRevisions = diffCommitsForRev(vars)
		if (diffRevisions.size() >= vars.commitCap) {
			println("[JPL] Number of unique commits between the two revisions: ${diffRevisions.size()} exceeds the Cap: ${vars.commitCap}; returning default value")
			return ["*"]
		}

		println("[JPL] Found following revisions between source and target branch: ${diffRevisions}")

		List<String> filesChanged = []
		for (diffRevision in diffRevisions) {
			filesChanged += filesForRev(vars, diffRevision)
		}
		filesChanged = filesChanged.unique()

		println("[JPL] Found following changed files source and target branch: ${filesChanged}")

		if (filesChanged.size() >= vars.fileCap) {
			println("[JPL] Number of files changes: ${filesChanged.size()} exceeds the Cap: ${vars.fileCap}; returning default value")
			return ["*"]
		}

		filesChanged

	}

}

//def call(Closure body=null) {
//	this.vars = [:]
//	this.getSonarInclusions(vars, body)
//}
