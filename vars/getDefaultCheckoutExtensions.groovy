#!/usr/bin/groovy

def call(Closure body=null) {
  this.vars = [:]
  call(vars, body)
}

//def call(isDefaultBranch = false, relativeTargetDir = "", timeout = 20, isCleaningEnabled = true, isShallowEnabled = true, depth = 0, noTags = true) {
def call(Map vars, Closure body=null) {
  vars = vars ?: [:]

  vars.isDefaultBranch = vars.get('isDefaultBranch', false).toBoolean()
  vars.relativeTargetDir = vars.get('relativeTargetDir', '').trim()
  vars.timeout = vars.get('timeout', 20)

  vars.isCleaningEnabled = vars.get('isCleaningEnabled', false).toBoolean()
  vars.noTags = vars.get('noTags', false).toBoolean()

  // See https://issues.jenkins-ci.org/browse/JENKINS-46736 noTags to false need isCleaningEnabled to false
  if (vars.noTags == false) {
    echo 'Forcing isCleaningEnabled to false when noTags is false'
    vars.isCleaningEnabled = false
  }

  vars.isShallowEnabled = vars.get('isShallowEnabled', true).toBoolean()
  vars.depth = vars.get('depth', 0)
  vars.reference = vars.get('reference', '/var/lib/gitcache/nabla.git').trim()
  vars.honorRefspec = vars.get('honorRefspec', true).toBoolean()

    //call(vars.isDefaultBranch, vars.relativeTargetDir, vars.timeout, vars.isCleaningEnabled, vars.isShallowEnabled)

  if (body) { body() }

  def DEFAULT_EXTENSIONS = [
            //[$class: 'LocalBranch', localBranch: "**"],
            [$class: 'LocalBranch', localBranch: "${scm.branches[0]}"],
            [$class: 'RelativeTargetDirectory', relativeTargetDir: vars.relativeTargetDir],
            [$class: 'MessageExclusion', excludedMessage: '.*\\\\[maven-release-plugin\\\\].*'],
            [$class: 'IgnoreNotifyCommit'],
            [$class: 'CheckoutOption', timeout: vars.timeout],
        //[$class: 'ChangelogToBranch', options: [compareRemote: 'origin', compareTarget: 'release/1.7.0']]
        ]

  // Saving time and disk space when you just want to access the latest version of a repository.
  def DEFAULT_CLONE_OPTIONS_EXTENSIONS = [
            [$class: 'CloneOption', depth: vars.depth, noTags: vars.noTags, reference: vars.reference, shallow: vars.isShallowEnabled, honorRefspec: vars.honorRefspec, timeout: vars.timeout]
        ]

  def DEFAULT_CLEAN_OPTIONS_EXTENSIONS = [
            //[$class: 'WipeWorkspace'],
            //[$class: 'CleanCheckout'],
            [$class: 'CleanBeforeCheckout'],
        ]

  def myExtensions = null

  if (vars.isDefaultBranch && vars.isCleaningEnabled) {
    echo 'Default extensions managed by Jenkins'
    myExtensions = scm.extensions + DEFAULT_EXTENSIONS + DEFAULT_CLONE_OPTIONS_EXTENSIONS
    } else {
    myExtensions = DEFAULT_EXTENSIONS
    if (vars.isCleaningEnabled) {
      echo 'Adding cleaning to extensions'
      myExtensions += DEFAULT_CLEAN_OPTIONS_EXTENSIONS
    }
  }

  return myExtensions
}
