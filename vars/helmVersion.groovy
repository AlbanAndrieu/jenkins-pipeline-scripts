#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmVersion.groovy`'

  vars = vars ?: [:]

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', vars.draftPack ?: 'charts').toLowerCase().trim()

  vars.isGitVersion = vars.get('isGitVersion', false).toBoolean()
  vars.isYamlFile = vars.get('isYamlFile', true).toBoolean()

  //vars.fileName = "${vars.helmDir}/${vars.helmChartName}/Chart.yaml"
  vars.helmFileName = vars.get('helmFileName', "${vars.helmDir}/${vars.helmChartName}/requirements.yaml").trim()
  vars.helmFileName = vars.helmFileName.replaceAll("\\./", '')

  vars.helmChartDefaultVersion = vars.get('helmChartDefaultVersion', '0.0.1').trim()
  vars.pomFile = vars.get('pomFile', '../pom.xml').trim()
  RELEASE_VERSION = helmTag(vars) ?: vars.helmChartDefaultVersion

  vars.helmChartVersion = vars.get('helmChartVersion', RELEASE_VERSION).trim().replaceAll(' ', '-')
  //vars.helmChartAppVersionTag = vars.get("helmChartAppVersionTag", vars.helmChartVersion).trim()
  //vars.helmChartVersionTag = vars.get("helmChartVersionTag", vars.helmChartVersion).trim()
  vars.helmUmbrellaChartVersionTag = vars.get('helmUmbrellaChartVersionTag', '^0.0').trim()
  vars.customRepoName = vars.get('customRepoName', 'custom').trim()

  vars.skipVersion = vars.get('skipVersion', false).toBoolean()
  //vars.helmFileId = vars.get("helmFileId", vars.draftPack ?: "0").trim()

  //vars.helmVersionOutputFile = vars.get("helmVersionOutputFile", "helm-get-${vars.helmFileId}.log").trim()
  //vars.skipVersionFailure = vars.get("skipVersionFailure", true).toBoolean()

  if (!vars.skipVersion) {
    try {
      if (body) { body() }

      if (vars.isGitVersion.toBoolean()) {
        sh 'git describe > .git/current-describe'
        vars.helmUmbrellaChartVersionTag = readFile('.git/current-describe').trim()
      } // isGitVersion

      if (fileExists(vars.helmFileName)) {
        if (vars.isYamlFile.toBoolean()) {
          def data = readYaml file: vars.helmFileName
          //println data.getClass()
          //println data.dependencies.getClass()
          if (data.dependencies.getClass() == java.util.ArrayList) {
            for (String item : data.dependencies) {
              println item
              //echo item.repository.toString()
              if (item.repository.toString() == '@{vars.customRepoName}') {
                item.repository.version = vars.helmUmbrellaChartVersionTag
                echo 'Replacing version for : ' + item.repository.name + ' - for ' + item.version.toString()
              } // if
            } // for
            //sh "rm -f ${vars.helmFileName}"
            writeYaml file: vars.helmFileName, data: data, overwrite: true
          } // if ArrayList
        } else {
          def text = readFile file: vars.helmFileName
          text.replaceAll('version:.*', "version: ${vars.helmUmbrellaChartVersionTag}")
          writeFile file: vars.helmFileName, text: text
        }
      } else {
        echo "No fileExists(${vars.helmFileName})"
      }
    } catch (exc) {
      echo 'Warn: There was a problem with versioning helm umbrella charts ' + exc
    } finally {
      archiveArtifacts artifacts: "${vars.helmFileName}", onlyIfSuccessful: false, allowEmptyArchive: true
    }
  } else {
    echo 'Helm version skipped'
  }
}
