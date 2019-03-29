#!/usr/bin/groovy

import hudson.model.*
import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  // Find all pom.xml files and switch SNAPSHOT versions
  // Does not affect stable / release versions
  def files = findFiles(glob: "**/pom.xml")
  for (file in files) {
    fileName = file.toString()
    println("Changing version in pom file: ${fileName}")
    pom = readMavenPom(file: fileName)
    version = pom.getVersion()
    if (version != null) {
      println("Current pom version is ${version}")
      if (version.contains("SNAPSHOT")) {
        pom.setVersion(config.newVersion)
        println("Changed pom version to ${config.newVersion}")
      }
    }
    parent = pom.getParent()
    if (parent != null) {
      parentVersion = parent.getVersion()
      println("Current parent pom version is ${version}")
      if (parentVersion.contains("SNAPSHOT")) {
        parent.setVersion(config.newVersion)
        pom.setParent(parent)
        println("Changed parent pom version to ${config.newVersion}")
      }
    }
    writeMavenPom(model: pom, file: fileName)
  }
}
