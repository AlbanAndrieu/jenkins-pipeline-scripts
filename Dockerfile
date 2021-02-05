FROM jenkins/jenkins:lts-jdk11 as RUNTIME
#FROM jenkinsci/blueocean:latest as RUNTIME

# Install dependencies
USER root

# hadolint ignore=DL3018
#RUN apk add --no-cache make openjdk8-jre

#ARG CERT_NAME=${CERT_NAME:-"NABLA.crt"}
#ARG CERT_URL=${CERT_URL:-"http://albandrieu.com/download/certs/"}
ENV JAVA_HOME /opt/java/openjdk/

RUN echo ${JAVA_HOME}
#RUN ls -lrta /opt/java/
#RUN ls -lrta /opt/java/openjdk
#RUN which java

#COPY ${CERT_URL}/NABLA.crt /usr/local/share/ca-certificates/NABLA.crt
#COPY ${CERT_URL}/${CERT_NAME} /usr/local/share/ca-certificates/${CERT_NAME}
#RUN update-ca-certificates
#RUN apk add --no-cache ca-certificates && \
#    update-ca-certificates

#RUN ln -sf /etc/ssl/certs/java/cacerts ${JAVA_HOME}/jre/lib/security/cacerts

# Update Java certs
#RUN keytool -v -noprompt \
#    -keystore ${JAVA_HOME}/jre/lib/security/cacerts \
#    -importcert \
#    -trustcacerts \
#    -file /usr/local/share/ca-certificates/${CERT_NAME} \
#    -alias test \
#    -keypass changeit \
#    -storepass changeit

# drop back to the regular jenkins user - good practice
USER jenkins

RUN mkdir /usr/share/jenkins/ref/configs
COPY --chown=jenkins src/test/jenkins/jenkins.yaml /usr/share/jenkins/ref/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/jenkins.yaml

ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

ENV JENKINS_OPTS --httpPort=-1 --httpsPort=8080
ENV JENKINS_SLAVE_AGENT_PORT 50000
ENV GIT_SSH_COMMAND="ssh -oStrictHostKeyChecking=no"

COPY src/test/jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt

# Install plugins
#RUN unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy

RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

COPY src/test/groovy/04-Executors.groovy /usr/share/jenkins/ref/init.groovy.d/04-Executors.groovy

# drop back to the regular jenkins user - good practice
USER jenkins

EXPOSE 8080 50000
