#!/usr/bin/env groovy

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(Map vars, Closure body) {

    if (!body) {
        error 'no body specified, mandatory'
    }

    def DRY_RUN = vars.get("DRY_RUN", env.DRY_RUN.toBoolean() ?: false)

    def isScmEnabled = vars.get("isScmEnabled", false).toBoolean()
    def isMavenEnabled = vars.get("isMavenEnabled", false).toBoolean()

    if (isScmEnabled) {
        echo "scm is enabled, using git!!! (SLOWER adn UNSTABLE)"
        // This is a bad because of the timeout which cannot be extended in jenkins

        gitCheckoutTEST()

        //dir ("test/app") {
            body()
        //} // dir

    } else {
        echo "scm is disabled, using unstash"
        // This is a workaround because of got clone is too slow with lfs

        //dir ("test") {

            unstash 'sources'

            if (isMavenEnabled) {
                unstash 'sources-tools'
            }

            //dir ("app") {

                if (!DRY_RUN && isMavenEnabled) {
                    unstash 'maven-artifacts'
                }

                body()

            //} // dir

        //} // dir
    }

}
