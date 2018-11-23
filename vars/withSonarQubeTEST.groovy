#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
	this.vars = [:]
	call(vars, body)
}

def call(Map vars, Closure body=null) {

    vars = vars ?: [:]

    wrapInTEST(vars) {

        withSonarQubeWrapper(vars, body)
    }

}
