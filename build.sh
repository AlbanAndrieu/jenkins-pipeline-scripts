#!/bin/bash
set -e

WORKING_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}"  )" && pwd  )"

# shellcheck source=/dev/null
source "${WORKING_DIR}/step-0-color.sh"

# curl (REST API)
# Assuming "anonymous read access" has been enabled on your Jenkins instance.
# JENKINS_URL=[root URL of Jenkins master]
if [ -n "${JENKINS_URL}" ]; then
  echo -e "${green} JENKINS_URL is defined ${happy_smiley} : ${JENKINS_URL} ${NC}"
else
  echo -e "${red} ${double_arrow} Undefined build parameter ${head_skull} : JENKINS_URL, use the default one ${NC}"
  JENKINS_URL="https://localhost/"
  export JENKINS_URL
  echo -e "${magenta} JENKINS_URL : ${JENKINS_URL} ${NC}"
fi

echo -e "${green} Running the jenkins validation. ${NC}"

# JENKINS_CRUMB is needed if your Jenkins master has CRSF protection enabled as it should
JENKINS_CRUMB=$(curl "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)")
curl -X POST -H "$JENKINS_CRUMB" -F "jenkinsfile=<Jenkinsfile" "$JENKINS_URL/pipeline-model-converter/validate"
RC=$?
if [ ${RC} -ne 0 ]; then
  echo ""
  # shellcheck disable=SC2154
  echo -e "${red} ${head_skull} Sorry, jenkins validation failed. ${NC}"
  exit 1
else
  # shellcheck disable=SC2154
  echo -e "${magenta} Check by hand the jenkins validation. ${NC}"
fi

# ssh (Jenkins CLI)
# JENKINS_SSHD_PORT=[sshd port on master]
#JENKINS_SSHD_PORT=222
# JENKINS_HOSTNAME=[Jenkins master hostname]
#JENKINS_HOSTNAME=localhost
#ssh -p $JENKINS_SSHD_PORT $JENKINS_HOSTNAME declarative-linter < Jenkinsfile

#./mvnw install 
#./gradlew build

exit 0
