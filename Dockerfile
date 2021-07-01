FROM jenkins/jenkins:2.289.1-lts-jdk11 as RUNTIME

# Install dependencies
USER root

RUN apt-get update && apt-get install -y apt-transport-https \
       ca-certificates curl gnupg2 \
       software-properties-common
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN apt-key fingerprint 0EBFCD88
RUN add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/debian \
       $(lsb_release -cs) stable"
RUN apt-get update && apt-get install -y docker-ce-cli

ARG JENKINS_HOME=${JENKINS_HOME:-"/var/jenkins_home"}

ARG CERT_NAME=${CERT_NAME:-"NABLA.crt"}
ARG CERT_URL=${CERT_URL:-"http://albandrieu.com/download/certs/"}
ENV JAVA_HOME /opt/java/openjdk/

ADD ${CERT_URL}/${CERT_NAME} /usr/local/share/ca-certificates/${CERT_NAME}

# hadolint ignore=DL3008
RUN apt-get install ca-certificates && \
    update-ca-certificates && apt-get clean && rm -rf /var/lib/apt/lists/*

ARG JAVA_HOME=${JAVA_HOME:-"/opt/java/openjdk"}
#RUN echo ${JAVA_HOME}
#RUN ls -lrta ${JAVA_HOME}/lib/security/cacerts
#RUN ln -sf /etc/ssl/certs/ca-certificates.crt ${JAVA_HOME}/lib/security/cacerts
#RUN ln -sf /etc/ssl/certs/java/cacerts ${JAVA_HOME}/lib/security/cacerts

RUN keytool -import -alias nabla -file /usr/local/share/ca-certificates/${CERT_NAME} -keystore ${JAVA_HOME}/lib/security/cacerts -storepass changeit -noprompt

# Update Java certs
#RUN keytool -v -noprompt \
#    -keystore ${JAVA_HOME}/lib/security/cacerts \
#    -importcert \
#    -trustcacerts \
#    -file /usr/local/share/ca-certificates/${CERT_NAME} \
#    -alias test \
#    -keypass changeit \
#    -storepass changeit

# test certificate
RUN keytool -list -keystore ${JAVA_HOME}/lib/security/cacerts -alias nabla -storepass changeit

RUN mkdir /usr/share/jenkins/ref/configs
COPY --chown=jenkins src/test/jenkins/jenkins.yaml /usr/share/jenkins/ref/jenkins.yaml
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/jenkins.yaml

#ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

ENV JENKINS_OPTS --httpPort=-1 --httpsPort=8080
ENV JENKINS_SLAVE_AGENT_PORT 50000
#ENV JAVA_ARGS="$JAVA_ARGS -Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts"
ENV GIT_SSH_COMMAND="ssh -oStrictHostKeyChecking=no"

COPY src/test/jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt

# Install plugins
RUN unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy

RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

COPY src/test/groovy/04-Executors.groovy /usr/share/jenkins/ref/init.groovy.d/04-Executors.groovy

# drop back to the regular jenkins user - good practice
USER jenkins

EXPOSE 8080 50000
