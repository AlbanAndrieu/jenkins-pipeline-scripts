#!/usr/bin/groovy
import hudson.model.*

/** <p>
 * This is an overloaded method, so javadoc should be visible
 *  </p>
 *  {@inheritDoc}
 * @return
 * {@inheritDoc}
 * */
def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

/**
 * <h1>Give the version or appVersion of a chart from his file.</h1>
 * Allow to retrieve <code>version</code> or <code>appVersion</code> from Chart.yaml (or requirement.yaml).
 * <p>
 * <b>Note:</b> helmChartVersion is used by helmPush, but is it also used for helm package (helmChartVersionTag) and helm install (helmInstallVersionTag) as default value if not provided.
 *
 * @param helmChartName this is the default value which should be provided.
 * @param draftPack this is the seconde value which should be provided. If provided it will become the <code>helmChartName</code>
 * @param imageName this is the third value of <code>helmChartName</code>. <code>imageName</code> usually should be the same as <code>draftPack</code>. This is the name of the generated docker image. If not provided, it will be the name of the git repository
 * @param helmDir this allow to override the chart location (by default: ./packs).
 * @return the helmChartName value
 */
def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmChartVersionFromFile.groovy`'

  vars = vars ?: [:]

  helmChartName(vars)

  vars.isYamlFile = vars.get('isYamlFile', true).toBoolean()
  vars.isAppVersion = vars.get('isAppVersion', false).toBoolean()

  vars.helmChartFileName = vars.get('helmChartFileName', "${vars.helmDir}/${vars.helmChartName}/charts/Chart.yaml").trim()
  vars.helmChartFileName = vars.helmChartFileName.replaceAll("\\./", '')

  vars.skipChartVersion = vars.get('skipChartVersion', false).toBoolean()
  //vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()

  if (isHelmTag(vars)) {
    echo 'Skipping getting version from Chart file'
    // Skipping getting version from Chart, we should use the one provided
    vars.skipChartVersion = true
  }

  if (!vars.skipChartVersion) {
    try {
      if (body) { body() }

      if (fileExists(vars.helmChartFileName)) {
        if (vars.isYamlFile.toBoolean()) {
          def data = readYaml file: vars.helmChartFileName
          //println data.getClass()
          //println data.version.getClass()
          if (vars.isAppVersion.toBoolean()) {
            echo 'AppVersion found : ' + data.appVersion + ' for chart : ' + vars.helmChartName
            if (!data.appVersion?.trim() || data.appVersion.trim() == 'null' || data.appVersion == null) {
              echo 'No appVersion found (Fix the chart)'
              return false
            } else {
              vars.helmChartAppVersionTag = data.appVersion
              return true
            }
          } else {
            echo 'Version found : ' + data.version + ' for chart : ' + vars.helmChartName
            if (!data.version?.trim() || data.version.trim() == 'null' || data.version == null) {
              echo 'No version found (Fix the chart)'
              return false
            } else {
              vars.helmChartVersion = data.version
              return true
            }
          } // isAppVersion
        } else {
          echo 'No yaml'
        }
      } else {
        echo "No fileExists(${vars.helmChartFileName})"
      }
    } catch (exc) {
      echo 'Warn: There was a problem with getting charts version : ' + exc
    } finally {
      archiveArtifacts artifacts: "${vars.helmChartFileName}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'Helm version retrieved from chart skipped'
  }

  //vars.helmChartAppVersionTag = vars.get("helmChartAppVersionTag", vars.helmChartVersion).trim() // Most of the time it is used as the docker image tag

  return false
}
