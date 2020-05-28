package com.lesfurets.jenkins

import org.junit.Test

import com.lesfurets.jenkins.unit.BasePipelineTest

class TestHelperInitialization extends BasePipelineTest {

	private static String JOB = "job/exampleJob.jenkins"

    @Test(expected = IllegalStateException)
    void non_initialized_helper() throws Exception {
        runScript(JOB)
    }

    @Test(expected = NullPointerException)
    void non_initialized_gse() throws Exception {
        helper.loadScript(JOB)
    }

    @Test
    void initialized_helper() throws Exception {
        scriptRoots += 'src/test/jenkins'
        super.setUp()
        helper.loadScript(JOB)
    }
}
