#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
    this.vars = [:]
    call(vars, body)
}

def call(Map vars, Closure body=null) {

    echo "[JPL] Executing `vars/getTimestamp.groovy`"

    vars = vars ?: [:]

    // See https://plugins.jenkins.io/build-timestamp/

    try {
      //println(env.BUILD_TIMESTAMP)
      if (!env.BUILD_TIMESTAMP?.trim()) {
          env.BUILD_TIMESTAMP = timestamp()
          echo "NEW BUILD_TIMESTAMP : ${env.BUILD_TIMESTAMP}"
      }
      environment()
    } catch(exc) {
        echo 'Error: There were errors in getTimestamp. '+exc.toString()
        env.BUILD_TIMESTAMP = "TODO"
    }
    echo "BUILD_TIMESTAMP : ${env.BUILD_TIMESTAMP}"
    return env.BUILD_TIMESTAMP
}

// This timestamp will be used in case plugin is missing https://plugins.jenkins.io/build-timestamp/
def timestamp() {
    def now = new Date()
    def timezone="Europe/Paris"
    return now.format("yyyyMMdd'T'HHmmss", TimeZone.getTimeZone(timezone))
}
