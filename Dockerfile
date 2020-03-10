FROM maven:3-jdk-11 as BUILD

COPY . /usr/src/app
RUN mvn --batch-mode -f /usr/src/app/pom.xml clean package

FROM jenkins/jenkins:lts-jdk11 as RUNTIME

#RUN mkdir $JENKINS_HOME/configs
#COPY ./jenkins.yaml $JENKINS_HOME/configs/jenkins.yaml
#ENV CASC_JENKINS_CONFIG=$JENKINS_HOME/configs

ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

RUN unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy \
  && /usr/local/bin/install-plugins.sh \
#  groovy-events-listener-plugin:latest \ # warning about groovy version
  configuration-as-code \
  configuration-as-code-support \
  blueocean \
  job-dsl \
  cloudbees-folder \
  workflow-aggregator \
  pipeline-utility-steps \
  generic-webhook-trigger \
  git-changelog

USER 0

EXPOSE 8080 50000
