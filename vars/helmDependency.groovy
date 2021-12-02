#!/usr/bin/groovy
import hudson.model.*

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

def call(Map vars, Closure body=null) {
  echo '[JPL] Executing `vars/helmDependency.groovy`'

  vars = vars ?: [:]

  //getJenkinsOpts(vars)

  // helmDir must be relatif, never ${pwd()}/charts
  vars.helmDir = vars.get('helmDir', './packs').toLowerCase().trim()
  vars.helmChartName = vars.get('helmChartName', 'packs').toLowerCase().trim()

  //vars.isHelm2 = vars.get("isHelm2", false).toBoolean()
  vars.skipDependency = vars.get('skipDependency', false).toBoolean()
  vars.isDependencyUpdate = vars.get('isDependencyUpdate', false).toBoolean()
  vars.isDependencyBuild = vars.get('isDependencyBuild', true).toBoolean()
  vars.helmFileId = vars.get('helmFileId', vars.draftPack ?: '0').trim()

  vars.helmDependencyOutputFile = vars.get('helmDependencyOutputFile', "helm-dependency-${vars.helmFileId}.log").trim()
  vars.skipDependencyFailure = vars.get('skipDependencyFailure', false).toBoolean()

  if (!vars.skipDependency) {
    try {
      if (body) { body() }

      sh """#!/bin/bash -l
        pwd
        ls -lrta .
        ls -lrta ${vars.helmDir}/${vars.helmChartName} || true
        """

      String helmDependencyCmd = 'helm dependency '

      // if requirements.yml
      if (vars.isDependencyBuild.toBoolean()) {
        helmDependencyCmd += "build ${vars.helmDir}/${vars.helmChartName}"
        } else if (vars.isDependencyUpdate.toBoolean()) {
        helmDependencyCmd += "update ${vars.helmDir}/${vars.helmChartName}"
      }

      if (DEBUG_RUN) {
        sh """#!/bin/bash -l
          cat requirements.lock || true
          helm dependency list ${vars.helmDir}/${vars.helmChartName} || true"""
      }

      // TODO Remove it when tee will be back
      helmDependencyCmd += " 2>&1 > ${vars.helmDependencyOutputFile} "

      helm = sh (script: helmDependencyCmd, returnStatus: true)
      echo "HELM DEPENDENCY RETURN CODE : ${helm}"
      if (helm == 0) {
        echo 'HELM DEPENDENCY SUCCESS'
        } else {
        echo "WARNING : Helm dependency failed, check output at \'${env.BUILD_URL}/artifact/${vars.helmDependencyOutputFile}\' "
        if (!vars.skipDependencyFailure) {
          echo 'HELM DEPENDENCY FAILURE'
          //currentBuild.result = 'UNSTABLE'
          currentBuild.result = 'FAILURE'
          error 'There are errors in helm'
          } else {
          echo 'HELM DEPENDENCY FAILURE skipped'
        //error 'There are errors in helm'
        }
      }
      } catch (exc) {
      echo 'Warn: There was a problem with helm dependency ' + exc
    }
  } else {
    echo 'Helm dependency skipped'
  }
}
