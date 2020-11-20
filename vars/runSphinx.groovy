#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/runSphinx.groovy`"

    vars = vars ?: [:]

    vars.shell = vars.get("shell", "./build.sh")
    vars.targetDirectory = vars.get("targetDirectory", "./sphinx")
    vars.skipSphinx = vars.get("skipSphinx", false).toBoolean()
    vars.skipSphinxFailure = vars.get("skipSphinxFailure", true).toBoolean()
    vars.sphinxFileId = vars.get("sphinxFileId", vars.draftPack ?: "0").trim()
    vars.sphinxOutputFile = vars.get("sphinxOutputFile", "sphinx-${vars.sphinxFileId}.log").trim()

    if (!vars.skipSphinx) {

        try {
            tee("${vars.sphinxOutputFile}") {

                dir("docs") {

                    sphinxResult = sh (
                            script: vars.shell,
                            returnStatus: true
                            )

                    echo "BUILD RETURN CODE : ${sphinxResult}"
                    if (sphinxResult == 0) {
                        echo "SPHINX SUCCESS"

				                publishHTML([
				                    allowMissing: false,
				                    alwaysLinkToLastBuild: false,
				                    keepAll: true,
				                    reportDir: "./_build",
				                    reportFiles: 'index.html',
				                    includes: '**/*',
				                    reportName: 'Sphinx Docs',
				                    reportTitles: "Sphinx Docs Index"
				                ])

				                //if (isReleaseBranch()) {
				                //    dir("_build") {
				                //        try {
				                //            rsync([
				                //                source: "*",
				                //                destination: "jenkins@albandrieu:/release/docs/" + targetDirectory,
				                //                credentialsId: "jenkins_unix_slaves"
				                //            ])
				                //        } catch (exc) {
				                //            currentBuild.result = 'UNSTABLE'
				                //            echo "WARNING : There was a problem copying results " + exc.toString()
				                //        }
				                //    }
				                //} // isReleaseBranch

                    } else {
                      echo "WARNING : Sphinx failed, check output at \'${vars.sphinxOutputFile}\' "
                      if (!vars.skipSphinxFailure) {
                        echo "SPHINX UNSTABLE"
                        currentBuild.result = 'UNSTABLE'
                      } else {
                        echo "SPHINX FAILURE skipped"
                        //error 'There are errors in sphinx' // not needed
                      }
                    }

                    if (body) {
                        body()
                    }

                } // dir docs

            } // tee

        } catch (exc) {
            echo "SPHINX FAILURE"
            currentBuild.result = 'FAILURE'
            //build = "FAIL" // make sure other exceptions are recorded as failure too
            echo "WARNING : There was a problem with sphinx " + exc.toString()
        } finally {
            archiveArtifacts artifacts: "sphinx.log", onlyIfSuccessful: false, allowEmptyArchive: true
        }
    } // if skipSphinx

}
