#!/usr/bin/groovy
// com/test/jenkins/getEnv.groovy
package com.test.jenkins

import hudson.model.*
import groovy.transform.CompileStatic
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import com.cloudbees.groovy.cps.NonCPS
import groovy.grape.Grape
//import org.apache.ivy.core.report.ResolveReport

//@Grab('org.apache.ivy:ivy:2.4.0')
//@GrabResolver(name='jenkins', root='https://repo.jenkins-ci.org/public/')
//@Grab(group='org.jenkins-ci.main', module='jenkins-core', version='2.9')

//@CompileStatic
class GlobalEnvironmentVariables implements Serializable {

	@NonCPS
	static def createGlobalEnvironmentVariables(String key, String value) {

		Jenkins instance = Jenkins.getInstance();

		DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = instance.getGlobalNodeProperties();
		List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);

		EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = null;
		EnvVars envVars = null;

		if ( envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0 ) {
			newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
			globalNodeProperties.add(newEnvVarsNodeProperty);
			envVars = newEnvVarsNodeProperty.getEnvVars();
		} else {
			envVars = envVarsNodePropertyList.get(0).getEnvVars();
		}
		envVars.put(key, value)
		instance.save()
	}

} 

//createGlobalEnvironmentVariables('Var1','DummyValue')
