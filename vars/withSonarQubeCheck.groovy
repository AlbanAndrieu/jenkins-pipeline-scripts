#!/usr/bin/groovy
import hudson.model.*
import static com.test.jenkins.sonar.Sonar.sonarRestCall;
import static com.test.jenkins.sonar.Sonar.getSonarReportProperties;

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withSonarQubeCheck.groovy`"

    vars = vars ?: [:]

    getJenkinsOpts(vars)

    vars.reportTaskFile = vars.get("reportTaskFile", ".scannerwork/report-task.txt").trim() // .scannerwork/report-task.txt or .sonar/report-task.txt
    vars.timeout = vars.get("timeout", 5)
    vars.sleep = vars.get("sleep", 1)
    vars.isFingerprintEnabled = vars.get("isFingerprintEnabled", false).toBoolean()
    vars.isAbortPipeline = vars.get("isAbortPipeline", false).toBoolean() // true = set pipeline to UNSTABLE, false = don't
    vars.skipFailure = vars.get("skipFailure", true).toBoolean()
    vars.sonarCheckOutputFile = vars.get("sonarCheckOutputFile", "sonar-check.log").trim()
    vars.sonarCheckResultFile = vars.get("sonarCheckResultFile", "sonar-result.log").trim()

    script {
        tee("${vars.sonarCheckOutputFile}") {

        try {

            withSonarQubeEnv("${vars.SONAR_INSTANCE}") {

                def qg = null

                // Wait until sonar scan is completed. Known issues - it freezes at first execution of waitForQualityGate()
                retry(10) {
                    sleep(time: vars.sleep, unit:"MINUTES")
                    println "Wait for Quality Gate"

                    timeout(time: vars.timeout, unit: 'MINUTES') {

                      qg = waitForQualityGate(abortPipeline: vars.isAbortPipeline)
                      if (qg.status == 'OK')
                        echo "Quality Gate status is OK"
                      else
                        echo "WARNING: Quality Gate status is ${qg.status}"

                    } // timeout

                } // retry

                // Read branch and projectKey from report-task.txt
                def report = readProperties file: vars.reportTaskFile
                def branch = report["branch"]
                def projectKey = report["projectKey"]
                def dashboardUrl = report["dashboardUrl"]

                // Create REST call to SonarQube for issues count grouped by severity
                def apiUrl = "https://${vars.SONAR_INSTANCE}/api/issues/search"
                def severities = ""
                def query = "branch=${branch}"
                query += "&componentKeys=${projectKey}"
                query += "&resolved=false"
                query += "&severities=${severities}"
                query += "&facets=severities"
                query += "&ps=1"
                query += "&additionalFields=_all"

                def results = [:]
                // Make REST call
                withCredentials([usernamePassword(credentialsId: "${vars.SONAR_CREDENTIALS}", passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {

                   if (body) {
                     body()
                   }

                  def ret = sonarRestCall(apiUrl, USER, PASSWORD, "GET", query)
                  // Parse issues count from json response
                  ret_json = new groovy.json.JsonSlurperClassic().parseText(ret)
                  for (item in ret_json["facets"]) {
                    if (item["property"] == "severities")
                      severities = item["values"]
                  }
                  for (item in severities) {
                    results.put(item["val"], item["count"])
                  }
                } // withCredentials
                writeFile file: "${vars.sonarCheckResultFile}", text: """---Sonar scan summary---
Dashboard URL: ${dashboardUrl}
Issue count: ${results}
Quality Gate status: ${qg.status}"""
                // Fail the build if not release branch and quality gate status is not OK
                if (!isReleaseBranch() && qg.status != 'OK') {
                  echo "Quality gate status NOT met on short-lived branch"
                  if (!vars.skipFailure) {
                    echo "SONAR CHECK UNSTABLE on QUALITY GATE"
                    currentBuild.result = 'UNSTABLE'
                    //error "Pipeline aborted because of quality gate status on short-lived branch"
                  }
                }

            } // withSonarQubeEnv

        } catch (exc) {
          if (!vars.skipFailure) {
              echo "SONAR CHECK UNSTABLE"
              currentBuild.result = 'UNSTABLE'
          } else {
              echo "SONAR CHECK FAILURE skipped"
              //error 'There are errors in sonar check'
          }
          echo "WARNING : There was a problem retrieving sonar scan results" + exc.toString()
        }

        } // tee

        archiveArtifacts artifacts: "${vars.sonarCheckOutputFile}, ${vars.sonarCheckResultFile}, ${vars.reportTaskFile}", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true

    } // script

}
