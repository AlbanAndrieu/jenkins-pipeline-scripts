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
  prometheus:latest

COPY src/test/groovy/04-Executors.groovy /usr/share/jenkins/ref/init.groovy.d/04-Executors.groovy

# drop back to the regular jenkins user - good practice
USER jenkins

EXPOSE 8686 50000
