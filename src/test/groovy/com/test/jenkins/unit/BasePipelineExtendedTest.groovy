package com.test.jenkins.unit;

import org.junit.Before;

import com.lesfurets.jenkins.unit.BasePipelineTest;

abstract class BasePipelineExtendedTest extends BasePipelineTest {

	@Override
	@Before
	void setUp() throws Exception {
		scriptRoots += 'src/test/jenkins'
		super.setUp()

		helper.registerAllowedMethod("pipeline", [Closure.class], null)
		helper.registerAllowedMethod("agent", [String.class], null)
		//binding.setVariable('env' , "")
		binding.setVariable('env', [BRANCH_NAME: 'master'])

		binding.setVariable('none' , "")
		helper.registerAllowedMethod("parameters", [Closure.class], null)
		helper.registerAllowedMethod("booleanParam", [Map.class], null)

		helper.registerAllowedMethod("environment", [Closure.class], null)
		binding.setVariable('params' , [DRY_RUN: false])
		binding.setVariable('params' , [BUILD_ONLY: false])
		binding.setVariable('params' , [BUILD_TEST: false])
		binding.setVariable('params' , [BUILD_GRADLE: false])

		helper.registerAllowedMethod("options", [Closure.class], null)
		helper.registerAllowedMethod("skipStagesAfterUnstable", [], null)
		helper.registerAllowedMethod("parallelsAlwaysFailFast", [], null)
		helper.registerAllowedMethod("ansiColor", [String.class], null)
		helper.registerAllowedMethod("timeout", [Map.class], null)

		//helper.registerAllowedMethod("timestamps", [], null)
		helper.registerAllowedMethod("timestamps", [], { println 'Printing timestamp' })

		//helper.registerAllowedMethod("stages", [String.class, Closure.class], null)
		helper.registerAllowedMethod("stages", [Closure.class], null)
		helper.registerAllowedMethod("agent", [Closure.class], null)
		helper.registerAllowedMethod("docker", [Closure.class], null)

		//binding.setVariable('image' , [String.class], {c -> "registry.misys.global.ad/fusion-risk/ansible-jenkins-slave:1.0.8"})
		helper.registerAllowedMethod('image' , [String.class], null)
		helper.registerAllowedMethod('alwaysPull' , [Boolean.class], null)
		helper.registerAllowedMethod('reuseNode' , [Boolean.class], null)
		helper.registerAllowedMethod('registryUrl' , [String.class], null)
		helper.registerAllowedMethod('registryCredentialsId' , [String.class], null)
		helper.registerAllowedMethod('args' , [String.class], null)
		helper.registerAllowedMethod('label' , [String.class], null)

		helper.registerAllowedMethod("steps", [Closure.class], null)
		helper.registerAllowedMethod("script", [Closure.class], null)

		helper.registerAllowedMethod("when", [Closure.class], null)
		helper.registerAllowedMethod("expression", [Closure.class], null)

		helper.registerAllowedMethod("sh", [Map.class], {c -> "bcc19744"})
		//helper.registerAllowedMethod("sh", [Map.class], null)

		binding.setVariable('WORKSPACE' , "")

		helper.registerAllowedMethod("build", [Map.class], null)

		helper.registerAllowedMethod("post", [Closure.class], null)
		helper.registerAllowedMethod("always", [Closure.class], null)
		helper.registerAllowedMethod("cleanup", [Closure.class], null)

		//helper.registerAllowedMethod("timeout", [Map.class, Closure.class], null)
		helper.registerAllowedMethod("junit", [Map.class], null)
		helper.registerAllowedMethod("file", [Map.class], stringInterceptor)
		helper.registerAllowedMethod("archiveArtifacts", [String.class], null)

		helper.registerAllowedMethod("tee", [String.class, Closure.class], null)

		helper.registerAllowedMethod("publishHTML", [], null)
		helper.registerAllowedMethod("publishHTML", [Map.class], null)

		helper.registerAllowedMethod("checkStyle", [], null)
		helper.registerAllowedMethod("cpd", [], null)
		helper.registerAllowedMethod("cpd", [Map.class], null)
		helper.registerAllowedMethod("pmdParser", [], null)
		helper.registerAllowedMethod("pmdParser", [Map.class], null)
		helper.registerAllowedMethod("pit", [], null)
		helper.registerAllowedMethod("mavenConsole", [], null)
		helper.registerAllowedMethod("java", [], null)
		helper.registerAllowedMethod("java", [Map.class], null)
		helper.registerAllowedMethod("javaDoc", [], null)
		helper.registerAllowedMethod("spotBugs", [], null)
		helper.registerAllowedMethod("excludeFile", [String.class], null)

		helper.registerAllowedMethod("recordIssues", [Map.class], null)

		helper.registerAllowedMethod("dockerFingerprintFrom", [Map.class], null)

		binding.setVariable('JAVA_HOME' , "")
		binding.setVariable('HOME' , "")

	}

}
