FROM maven:3-jdk-11 as BUILD

COPY . /usr/src/app
RUN mvn --batch-mode -f /usr/src/app/pom.xml clean package

FROM openjdk:11-jre-slim
ENV PORT 4567
EXPOSE 4567
COPY --from=BUILD /usr/src/app/target /opt/target
WORKDIR /opt/target

CMD ["/bin/bash", "-c", "find -type f -name '*-with-dependencies.jar' | xargs java -jar"]

FROM jenkins/jenkins:lts as RUNTIME

RUN mkdir $JENKINS_HOME/configs
COPY ./jenkins.yaml $JENKINS_HOME/configs/jenkins.yaml
ENV CASC_JENKINS_CONFIG=$JENKINS_HOME/configs

ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

RUN install-plugins.sh \
  configuration-as-code \
  configuration-as-code-support \
  blueocean \
  job-dsl \
  cloudbees-folder \
  workflow-aggregator \
  pipeline-utility-steps \
  generic-webhook-trigger \
  git-changelog
