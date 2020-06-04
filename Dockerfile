FROM maven:3-jdk-11 as BUILD

COPY . /usr/src/app
RUN mvn --batch-mode -f /usr/src/app/pom.xml clean package

FROM jenkins/jenkins:lts-jdk11 as RUNTIME

RUN mkdir $JENKINS_HOME/configs
COPY src/test/jenkins/jenkins.yaml $JENKINS_HOME/configs/jenkins.yaml
ENV CASC_JENKINS_CONFIG=$JENKINS_HOME/configs

ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

ENV JENKINS_OPTS --httpPort=-1 --httpsPort=8686
ENV JENKINS_SLAVE_AGENT_PORT 50000

RUN unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy \
  && /usr/local/bin/install-plugins.sh \
  groovy-events-listener-plugin:1.014 \
  configuration-as-code \
  configuration-as-code-support \
  blueocean \
  job-dsl \
  cloudbees-folder \
  workflow-aggregator \
  pipeline-utility-steps \
  generic-webhook-trigger \
  tasks \
  pipeline-maven \
  locale \
  sonar \
  ws-cleanup \
  ansicolor \
  timestamper \
  groovy-postbuild \
  git-changelog \
  warnings-ng \
  prometheus:latest

#ws-cleanup envinject artifactdeployer nodenamecolumn summary_report docker-commons publish-over-cifs progress-bar-column-plugin thinBackup token-macro jobConfigHistory scons downstream-buildview clamav scm-sync-configuration deploy naginator build-publisher sonar xvfb windows-slaves m2release git scm-api maven-repo-cleaner ssh-agent robot environment-script stepcounter plugin-usage-plugin jenkins-cloudformation-plugin cvs jira fail-the-build-plugin workflow-step-api checkmarx selenium matrix-combinations-parameter jobtype-column findbugs batch-task warnings violation-columns configurationslicing parameterized-trigger sounds job-exporter preSCMbuildstep fitnesse dashboard-view jquery-ui gatling job-node-stalker favorite exclusive-execution build-pipeline-plugin postbuild-task skip-certificate-check maven-info disk-usage node-iterator-api ssh stashNotifier publish-over-ssh sitemonitor build-metrics distfork promoted-builds flexible-publish selenium-builder tasks computer-queue-plugin saferestart throttle-concurrents read-only-configurations ant countjobs-viewstabbar email-ext credentials cobertura nodejs audit-trail text-finder-run-condition versioncolumn global-build-stats mapdb-api plot jquery show-build-parameters conditional-buildstep maven-plugin view-job-filters rebuild matrix-project performance copyartifact groovy claim docker-build-step join tap nodelabelparameter custom-tools-plugin ansicolor translation sidebar-link bulk-builder checkstyle mttr ssh-slaves ssh-credentials Exclusion subversion antisamy-markup-formatter covcomplplot testng-plugin greenballs dropdown-viewstabbar-plugin nested-view test-stability external-monitor-job docker-build-publish any-buildstep project-stats-plugin timestamper build-name-setter build-timeout prereq-buildstep jacoco caliper-ci pmd lastsuccessversioncolumn dry dependencyanalyzer analysis-collector analysis-core chosen-views-tabbar port-allocator description-setter publish-over-ftp groovy-postbuild build-environment seleniumrc-plugin git-client authentication-tokens monitoring text-finder extended-choice-parameter ftppublisher built-on-column run-condition lastfailureversioncolumn mask-passwords htmlpublisher hp-application-automation-tools-plugin project-description-setter build-with-parameters git-parameter javadoc build-failure-analyzer confluence-publisher script-security html5-notifier-plugin violations next-executions jenkins-multijob-plugin matrix-auth testInProgress copy-data-to-workspace-plugin mailer next-build-number qc multiple-scms job-import-plugin junit files-found-trigger compact-columns build-keeper-plugin dependency-check-jenkins-plugin pam-auth log-parser locale all-changes slave-status groovy-events-listener-plugin:1.014 configuration-as-code pipeline-utility-steps pipeline-maven git-changelog prometheus:latest

COPY src/test/groovy/04-Executors.groovy /usr/share/jenkins/ref/init.groovy.d/04-Executors.groovy

# drop back to the regular jenkins user - good practice
USER jenkins

EXPOSE 8686 50000
