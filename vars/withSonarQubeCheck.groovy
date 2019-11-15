#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/withSonarQubeCheck.groovy`"

    vars = vars ?: [:]

    def SONAR_INSTANCE = vars.get("SONAR_INSTANCE", env.SONAR_INSTANCE ?: "sonar").trim()
    def SONAR_SCANNER_OPTS = vars.get("SONAR_SCANNER_OPTS", env.SONAR_SCANNER_OPTS ?: "-Xmx2g")
    //def SONAR_USER_HOME = vars.get("SONAR_USER_HOME", env.SONAR_USER_HOME ?: "$WORKSPACE")
    def JENKINS_CREDENTIALS = vars.get("JENKINS_CREDENTIALS", "jenkins").trim()

    //def CLEAN_RUN = vars.get("CLEAN_RUN", env.CLEAN_RUN ?: false).toBoolean()
    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN ?: false).toBoolean()
    def DEBUG_RUN = vars.get("DEBUG_RUN", env.DEBUG_RUN ?: false).toBoolean()
    def RELEASE = vars.get("RELEASE", env.RELEASE ?: false).toBoolean()

    //vars.propertiesPath = vars.get("propertiesPath", "sonar-project.properties")

    script {
        if (!DRY_RUN && !RELEASE) {

            tee("sonar-check.log") {

                  def qg = null
                  // Wait until sonar scan is completed. Known issues - it freezes at first execution of waitForQualityGate()
                  retry(10) {
                    timeout(time: 10, unit: 'SECONDS') {
                      println "Wait for Quality Gate"
                      qg = waitForQualityGate()
                      if (qg.status == 'OK')
                        println "Quality Gate status is OK"
                      else
                        println "WARNING: Quality Gate status is ${qg.status}"
                    }
                  }
                  // Read branch and projectKey from report-task.txt
                  def report = readProperties file: "${env.WORKSPACE}/target/sonar/report-task.txt"
                  def branch = report["branch"]
                  def projectKey = report["projectKey"]
                  def dashboardUrl = report["dashboardUrl"]

                  // Create REST call to SonarQube for issues count grouped by severity
                  def apiUrl = "https://${SONAR_INSTANCE}/api/issues/search"
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
                  withCredentials([usernamePassword(credentialsId: "${SONAR_INSTANCE}", passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {

                     if (body) {
                       body()
                     }

                    def ret = sonarUtils.sonarRestCall(apiUrl, USER, PASSWORD, "GET", query)
                    // Parse issues count from json response
                    ret_json = utils.jsonParse(ret)
                    for (item in ret_json["facets"]) {
                      if (item["property"] == "severities")
                        severities = item["values"]
                    }
                    for (item in severities) {
                      results.put(item["val"], item["count"])
                    }
                  } // withCredentials
                  println """---Sonar scan summary---
Dashboard URL: ${dashboardUrl}
Issue count: ${results}
Quality Gate status: ${qg.status}"""
                  // Fail the build if not release branch and quality gate status is not OK
                  if (!isReleaseBranch() && qg.status != 'OK')
                    error "Pipeline aborted because of quality gate status on short-lived branch"

            } // tee

            archiveArtifacts artifacts: "sonar-check.log", excludes: null, fingerprint: vars.isFingerprintEnabled, onlyIfSuccessful: false, allowEmptyArchive: true

        } // if DRY_RUN
    } // script

}
