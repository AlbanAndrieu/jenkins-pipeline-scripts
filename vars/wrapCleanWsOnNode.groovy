#!/usr/bin/env groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/wrapCleanWsOnNode.groovy`'

  vars = vars ?: [:]

  vars.nodeLabel = vars.get('nodeLabel', 'any||flyweight').trim()

  if ( "${vars.nodeLabel}".trim() == '') {
    vars.nodeLabel = 'any||flyweight'
  }

  node("${vars.nodeLabel}") {
        wrapCleanWs(vars) {
      echo 'Hi from wrapCleanWs'
      echo 'Running on label:'
      echo vars.nodeLabel.toString()
                //echo "Currently in: "
                //sh "pwd"

        if (body) { body() }
      } // wrapCleanWs
    } // node
}
