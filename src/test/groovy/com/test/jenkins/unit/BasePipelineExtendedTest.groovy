package com.test.jenkins.unit

import org.junit.Before

import com.lesfurets.jenkins.unit.BasePipelineTest

abstract class BasePipelineExtendedTest extends BasePipelineTest {

    @Override
    @Before
  void setUp() throws Exception {
    scriptRoots += 'src/test/jenkins'
    super.setUp()

    helper.registerAllowedMethod('pipeline', [Closure], null)
    helper.registerAllowedMethod('agent', [String], null)
    //binding.setVariable('env' , "")
    binding.setVariable('env', [BRANCH_NAME: 'master'])

    binding.setVariable('none' , '')
    helper.registerAllowedMethod('parameters', [Closure], null)
    helper.registerAllowedMethod('booleanParam', [Map], null)

    helper.registerAllowedMethod('environment', [Closure], null)
    binding.setVariable('params' , [DRY_RUN: false])
    binding.setVariable('params' , [BUILD_ONLY: false])
    binding.setVariable('params' , [BUILD_TEST: false])
    binding.setVariable('params' , [BUILD_GRADLE: false])

    helper.registerAllowedMethod('options', [Closure], null)
    helper.registerAllowedMethod('skipStagesAfterUnstable', [], null)
    helper.registerAllowedMethod('parallelsAlwaysFailFast', [], null)
    helper.registerAllowedMethod('ansiColor', [String], null)
    helper.registerAllowedMethod('timeout', [Map], null)

    //helper.registerAllowedMethod("timestamps", [], null)
    helper.registerAllowedMethod('timestamps', [], { println 'Printing timestamp' })

    //helper.registerAllowedMethod("stages", [String.class, Closure.class], null)
    helper.registerAllowedMethod('stages', [Closure], null)
    helper.registerAllowedMethod('agent', [Closure], null)
    helper.registerAllowedMethod('docker', [Closure], null)

    //binding.setVariable('image' , [String.class], {c -> "registry.misys.global.ad/fusion-risk/ansible-jenkins-slave:1.0.8"})
    helper.registerAllowedMethod('image' , [String], null)
    helper.registerAllowedMethod('alwaysPull' , [Boolean], null)
    helper.registerAllowedMethod('reuseNode' , [Boolean], null)
    helper.registerAllowedMethod('registryUrl' , [String], null)
    helper.registerAllowedMethod('registryCredentialsId' , [String], null)
    helper.registerAllowedMethod('args' , [String], null)
    helper.registerAllowedMethod('label' , [String], null)

    helper.registerAllowedMethod('steps', [Closure], null)
    helper.registerAllowedMethod('script', [Closure], null)

    helper.registerAllowedMethod('when', [Closure], null)
    helper.registerAllowedMethod('expression', [Closure], null)

    helper.registerAllowedMethod('sh', [Map], { c -> 'bcc19744' })
        //helper.registerAllowedMethod("sh", [Map.class], null)

    binding.setVariable('WORKSPACE' , '')

    helper.registerAllowedMethod('build', [Map], null)

    helper.registerAllowedMethod('post', [Closure], null)
    helper.registerAllowedMethod('always', [Closure], null)
    helper.registerAllowedMethod('cleanup', [Closure], null)

    //helper.registerAllowedMethod("timeout", [Map.class, Closure.class], null)
    helper.registerAllowedMethod('junit', [Map], null)
    helper.registerAllowedMethod('file', [Map], stringInterceptor)
    helper.registerAllowedMethod('archiveArtifacts', [String], null)

    helper.registerAllowedMethod('tee', [String, Closure], null)

    helper.registerAllowedMethod('publishHTML', [], null)
    helper.registerAllowedMethod('publishHTML', [Map], null)

    helper.registerAllowedMethod('checkStyle', [], null)
    helper.registerAllowedMethod('cpd', [], null)
    helper.registerAllowedMethod('cpd', [Map], null)
    helper.registerAllowedMethod('pmdParser', [], null)
    helper.registerAllowedMethod('pmdParser', [Map], null)
    helper.registerAllowedMethod('pit', [], null)
    helper.registerAllowedMethod('mavenConsole', [], null)
    helper.registerAllowedMethod('java', [], null)
    helper.registerAllowedMethod('java', [Map], null)
    helper.registerAllowedMethod('javaDoc', [], null)
    helper.registerAllowedMethod('spotBugs', [], null)
    helper.registerAllowedMethod('excludeFile', [String], null)

    helper.registerAllowedMethod('recordIssues', [Map], null)

    helper.registerAllowedMethod('dockerFingerprintFrom', [Map], null)

    helper.registerAllowedMethod('setUp', [], null)
    helper.registerAllowedMethod('setUp', [Map], null)
    helper.registerAllowedMethod('tearDown', [], null)
    helper.registerAllowedMethod('tearDown', [Map], null)

    helper.registerAllowedMethod('draftStage', [], null)
    helper.registerAllowedMethod('draftStage', [Map], null)

    helper.registerAllowedMethod('helmPush', [], null)
    helper.registerAllowedMethod('helmPush', [Map], null)

    binding.setVariable('JAVA_HOME' , '')
    binding.setVariable('HOME' , '')
  }

}
