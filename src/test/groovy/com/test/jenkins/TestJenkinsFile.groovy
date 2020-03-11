package com.test.jenkins

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library

import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import static com.lesfurets.jenkins.unit.MethodSignature.method

import static org.assertj.core.api.Assertions.assertThat

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import com.lesfurets.jenkins.unit.MethodCall
import com.test.jenkins.unit.BasePipelineRegressionTest

@RunWith(Parameterized.class)
class TestJenkinsFile extends BasePipelineRegressionTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder()

	String sharedLibs = this.class.getResource('/libs').getFile()


	@Parameterized.Parameter
	public String branch
	@Parameterized.Parameter(1)
	public String expectedPhase


	@Parameterized.Parameters(name = "Test branch {0} phase {1}")
	static Collection<Object[]> data() {
		return [
			['develop', 'develop'],
			['master', 'master'],
			['feature_', 'feature_']
		].collect { it as Object[] }
	}

	@Override
	@Before
	void setUp() throws Exception {

		super.setUp()

		//binding.setVariable('BRANCH_NAME' , 'develop')

		//def scmBranch = branch
		//binding.setVariable('scm', [branch: 'master'])
		/*
		 binding.setVariable('scm', [
		 $class                           : 'GitSCM',
		 branches                         : [[name: scmBranch]],
		 doGenerateSubmoduleConfigurations: false,
		 extensions                       : [],
		 submoduleCfg                     : [],
		 userRemoteConfigs                : [
		 [
		 credentialsId: 'gitlab_git_ssh',
		 url          : 'github.com/lesfurets/JenkinsPipelineUnit.git'
		 ]]
		 ])
		 */

		def library = library().name('jenkins-pipeline-scripts')
				.defaultVersion("master")
				.allowOverride(true)
				.implicit(false)
				.targetPath(sharedLibs)
				.retriever(localSource(sharedLibs))
				.build()
		helper.registerSharedLibrary(library)

		//binding.setVariable('myenv' , '')
		helper.registerAllowedMethod("myenv", [Map.class], {c -> "myenv"})
		helper.registerAllowedMethod("getPropertyList", [], {c -> []})	
		
		helper.registerAllowedMethod("defineEnvironment", [], null)
		helper.registerAllowedMethod("printEnvironment", [], null)

		binding.setVariable('env', '')
		//binding.setVariable('env', [BRANCH_NAME: 'master'])
		//helper.registerAllowedMethod("env", [Map.class], {c -> "env"})
		//helper.registerAllowedMethod("getEnvironment", [], null)
		//registerAllowedMethod(methodSignature, callback != null ? { params -> return callback.apply(params) } : null)
		//helper.registerAllowedMethod(method("getEnvironment",  Map.class),  { map ->
		//    return "0"
		//})
		binding.setVariable('env', [getEnvironment: [COMPOSE_HTTP_TIMEOUT: '200']])
		binding.getVariable('env').getEnvironment = [COMPOSE_HTTP_TIMEOUT: '200']

		helper.registerAllowedMethod("createPropertyList", [], {c ->
			[
				buildDiscarder(
				logRotator(
				daysToKeepStr:         30,
				numToKeepStr:          2,
				artifactDaysToKeepStr: 10,
				artifactNumToKeepStr:  1
				)
				)
			]})
		//helper.registerAllowedMethod("createPropertyList", [Map.class], null)
		//helper.registerAllowedMethod("createPropertyList", [Map.class, Closure.class], null)
		
		helper.registerAllowedMethod("setBuildName", [], null)

		helper.registerAllowedMethod("sh", [Map.class], {c -> "build.sh"})

		helper.registerAllowedMethod("withMavenWrapper", [Map.class], null)
		helper.registerAllowedMethod("withMavenWrapper", [Map.class, Closure.class], null)

		helper.registerAllowedMethod("withShellCheckWrapper", [Map.class], null)
		helper.registerAllowedMethod("withShellCheckWrapper", [Map.class, Closure.class], null)

		helper.registerAllowedMethod("withSonarQubeWrapper", [Map.class], null)
		helper.registerAllowedMethod("withSonarQubeWrapper", [Map.class, Closure.class], null)

		helper.registerAllowedMethod("withCheckmarxWrapper", [Map.class], null)
		helper.registerAllowedMethod("withCheckmarxWrapper", [Map.class, Closure.class], null)

		helper.registerAllowedMethod("runHtmlPublishers", [List.class], null)

		helper.registerAllowedMethod("wrapCleanWs", [], null)
		helper.registerAllowedMethod("wrapCleanWs", [Map.class], null)
		helper.registerAllowedMethod("wrapCleanWs", [Map.class, Closure.class], null)
		helper.registerAllowedMethod("wrapCleanWsOnNode", [], null)
		helper.registerAllowedMethod("wrapCleanWsOnNode", [Map.class], null)
		helper.registerAllowedMethod("wrapCleanWsOnNode", [Map.class, Closure.class], null)

		helper.registerAllowedMethod("getDockerOpts", [], null)
		helper.registerAllowedMethod("getDockerOpts", [Map.class], {c ->"-v /home/jenkins:/home/jenkins -v /etc/passwd:/etc/passwd:ro -v /etc/group:/etc/group:ro -v /var/run/docker.sock:/var/run/docker.sock"})

		helper.registerAllowedMethod("getJenkinsOpts", [], null)
		helper.registerAllowedMethod("getJenkinsOpts", [Map.class], {c ->"true"})
		
		helper.registerAllowedMethod("cleanStash", [], null)
		helper.registerAllowedMethod("cleanStash", [Map.class], null)
		helper.registerAllowedMethod("cleanStash", [Map.class, Closure.class], null)

		helper.registerAllowedMethod("dockerTag", [String.class], {c ->"test"})
	}

	@Test
	void name() throws Exception {

		binding.getVariable('env').BRANCH_NAME = "$expectedPhase"

		runScript("Jenkinsfile")
		super.testNonRegression(branch)
		assertThat(helper.callStack.stream()
				.filter { c ->
					c.methodName == "sh"
				}
				.map(MethodCall.&callArgsToString)
				.findAll { s -> s.contains("echo TEST : $expectedPhase") })
				.hasSize(1)
	}
}
