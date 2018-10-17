#!/usr/bin/groovy
//import com.cloudbees.groovy.cps.NonCPS
import hudson.model.*

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(body) {

    wrapInTEST() {

        withSonarQubeWrapper(body)
    }

}
